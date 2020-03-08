package com.sikhsiyasat.wordpress.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.sikhsiyasat.wordpress.api.*
import java.util.*

//TODO separate out category & tags
@Entity(tableName = "wp_temp", indices = [Index("id", "name", "taxonomy")])
data class TermEntity(
    @PrimaryKey
    val id: Int,
    val link: String,
    val name: String,
    val slug: String,
    val taxonomy: TermTaxonomyEntity
)

enum class TermTaxonomyEntity {
    category, post_tag
}

@Entity(tableName = "feature_media", indices = [Index("id", "type", "mimeType")])
data class FeaturedMediaEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val sourceUrl: String,
    val mimeType: String,
    @Embedded(prefix = "media_")
    val mediaDetails: MediaDetailsEntity
)

data class MediaDetailsEntity(
    val width: Int,
    val height: Int
)

@Entity(tableName = "author", indices = [Index("id", "name", "slug")])
data class AuthorEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val description: String,
    val link: String,
    val slug: String,
    @Embedded(prefix = "avatar_")
    val avatarUrls: AvatarUrlsEntity?
)

data class AvatarUrlsEntity(
    @SerializedName("24")
    val twentyFour: String,
    @SerializedName("48")
    val fortyEight: String,
    @SerializedName("96")
    val ninetySix: String
)


@Entity(tableName = "post", indices = [Index("link", "slug", "title_rendered", "author")])
data class PostEntity(
    @PrimaryKey
    val id: String,
    val date: Date,
    val slug: String,
    val link: String,
    @Embedded(prefix = "title_")
    val title: PostField,
    @Embedded(prefix = "content_")
    val content: PostField,
    @Embedded(prefix = "excerpt_")
    val excerpt: PostField,
    val author: Int,
    val categories: List<Int>,
    val tags: List<Int>,
    val featuredMedia: String
)

class DisplayablePostLiveData(
    private val posts: List<PostEntity>,
    private val authorsLD: LiveData<List<AuthorEntity>>,
    private val categoriesLD: LiveData<List<TermEntity>>,
    private val tagsLD: LiveData<List<TermEntity>>,
    private val featureMediaLD: LiveData<List<FeaturedMediaEntity>>
) : MediatorLiveData<List<DisplayablePost>>() {
    private fun combine() {
        val authors = authorsLD.value ?: emptyList()
        val categories = categoriesLD.value ?: emptyList()
        val tags = tagsLD.value ?: emptyList()
        val featuredMediaList = featureMediaLD.value ?: emptyList()

        value = posts.map { postEntity ->
            DisplayablePost(
                postEntity.id,
                postEntity.date,
                postEntity.slug,
                postEntity.link,
                postEntity.title,
                postEntity.content,
                postEntity.excerpt,
                authors.firstOrNull { it.id == postEntity.author.toString() },
                categories.filter { it.id in postEntity.categories },
                tags.filter { it.id in postEntity.tags },
                featuredMediaList.firstOrNull { it.id == postEntity.featuredMedia }
            )
        }
    }

    init {
        addSource(authorsLD) { combine() }
        addSource(categoriesLD) { combine() }
        addSource(tagsLD) { combine() }
        addSource(featureMediaLD) { combine() }
        combine()
    }
}

data class DisplayablePost(
    val id: String,
    val date: Date,
    val slug: String,
    val link: String,
    val title: PostField,
    val content: PostField,
    val excerpt: PostField,
    val author: AuthorEntity?,
    val categories: List<TermEntity>,
    val tags: List<TermEntity>,
    val featuredMedia: FeaturedMediaEntity?
) {
    fun asHtml(): String? = content.rendered?.let { contentRendered ->
        "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<title>${title.rendered}</title>\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<style>\n" +
                "   .wp-caption{width:100%!important;}\n" +
                "   img.size-full{\n" +
                "       width:98%;\n" +
                "       margin:0px auto;  \n" +
                "       height: auto;\n" +
                "   }" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "   $contentRendered" +
                "</body>\n" +
                "</html>"
    }
}

@Database(
    entities = [
        PostEntity::class, AuthorEntity::class, TermEntity::class, FeaturedMediaEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class WordpressDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun authorDao(): AuthorDao
    abstract fun termDao(): TermDao
    abstract fun featuredMediaDao(): FeaturedMediaDao
}


@Dao
interface PostDao {
    @Insert(onConflict = REPLACE)
    fun save(post: PostEntity)

    @Insert(onConflict = REPLACE)
    fun save(post: Set<PostEntity>)

    @Query("SELECT * FROM post WHERE link = :link")
    fun loadByLink(link: String): LiveData<PostEntity?>

    @Query("SELECT * FROM post WHERE link like :likeExp")
    fun loadWhereLinkLike(likeExp: String): LiveData<List<PostEntity>>
}

@Dao
interface AuthorDao {
    @Insert(onConflict = REPLACE)
    fun save(author: AuthorEntity)

    @Insert(onConflict = REPLACE)
    fun save(authors: Set<AuthorEntity>)

    @Query("SELECT * FROM author WHERE id in (:ids)")
    fun load(ids: List<String>): LiveData<List<AuthorEntity>>
}

@Dao
interface TermDao {
    @Insert(onConflict = REPLACE)
    fun save(post: Set<TermEntity>)

    @Query("SELECT * FROM wp_temp WHERE id in (:ids)")
    fun load(ids: List<String>): LiveData<List<TermEntity>>
}

@Dao
interface FeaturedMediaDao {
    @Insert(onConflict = REPLACE)
    fun save(media: Set<FeaturedMediaEntity>)

    @Query("SELECT * FROM feature_media WHERE id in (:ids)")
    fun load(ids: List<String>): LiveData<List<FeaturedMediaEntity>>
}

object PostMapper {
    fun post(postEntity: PostEntity): Post = Post(
        postEntity.id,
        postEntity.date,
        postEntity.slug,
        postEntity.link,
        postEntity.title,
        postEntity.content,
        postEntity.excerpt,
        postEntity.author,
        postEntity.categories,
        postEntity.tags,
        postEntity.featuredMedia
    )

    fun postEntity(postEntity: Post): PostEntity? {
        return PostEntity(
            postEntity.id!!,
            postEntity.date ?: Date(0),
            postEntity.slug!!,
            postEntity.link!!,
            postEntity.title ?: PostField(),
            postEntity.content ?: PostField(),
            postEntity.excerpt ?: PostField(),
            postEntity.author,
            postEntity.categories ?: emptyList(),
            postEntity.tags ?: emptyList(),
            postEntity.featuredMedia ?: ""
        )
    }

    fun authorEntity(author: Author): AuthorEntity {
        return AuthorEntity(
            author.id, author.name, author.url,
            author.description, author.link, author.slug,
            author.avatarUrls?.let { avatarUrlsEntity(it) }
        )
    }

    private fun avatarUrlsEntity(avatarUrls: AvatarUrls): AvatarUrlsEntity {
        return AvatarUrlsEntity(avatarUrls.twentyFour, avatarUrls.fortyEight, avatarUrls.ninetySix)
    }

    fun termEntity(term: Term): TermEntity = TermEntity(
        term.id,
        term.link,
        term.name,
        term.slug,
        taxonomyEntity(term.taxonomy)
    )

    private fun taxonomyEntity(taxonomy: TermTaxonomy): TermTaxonomyEntity =
        TermTaxonomyEntity.valueOf(taxonomy.name)

    fun featuredMediaEntity(featuredMedia: FeaturedMedia): FeaturedMediaEntity =
        FeaturedMediaEntity(
            featuredMedia.id,
            featuredMedia.type,
            featuredMedia.sourceUrl,
            featuredMedia.mimeType,
            mediaDetailsEntity(featuredMedia.mediaDetails)
        )

    private fun mediaDetailsEntity(mediaDetails: MediaDetails): MediaDetailsEntity =
        MediaDetailsEntity(
            mediaDetails.width,
            mediaDetails.height
        )
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromIntList(value: List<*>): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toIntList(string: String?): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(string, listType)
    }

    @TypeConverter
    fun fromTermTaxonomy(value: TermTaxonomyEntity?): String? = value?.name

    @TypeConverter
    fun toTermTaxonomy(string: String?): TermTaxonomyEntity? =
        string?.let { TermTaxonomyEntity.valueOf(it) }
}
