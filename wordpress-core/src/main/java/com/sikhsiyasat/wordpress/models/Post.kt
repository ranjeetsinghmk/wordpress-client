package com.sikhsiyasat.wordpress.models

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.sikhsiyasat.wordpress.api.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

@Entity(tableName = "term", indices = [Index("id", "name", "taxonomy")])
data class TermEntity(
        @PrimaryKey
        val id: String,
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
        @Embedded(prefix = "caption_")
        val caption: PostField,
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


@Entity(tableName = "post", indices = [Index("link", "slug", "title_rendered", "author", "date")])
data class PostEntity(
        @PrimaryKey
        val id: String,
        val date: Date,
        val modifiedGmt: Date,
        val slug: String,
        val link: String,
        @Embedded(prefix = "title_")
        val title: PostField,
        @Embedded(prefix = "excerpt_")
        val excerpt: PostField,
        val author: String,
        val categories: List<String>,
        val tags: List<String>,
        val featuredMedia: String
)

@Entity(tableName = "post_term", primaryKeys = ["postId", "termId"])
data class PostTermEntity(
        val postId: String,
        val termId: String
)

@Entity(tableName = "post_content")
data class PostContentEntity(
        @PrimaryKey
        val postId: String,
        @Embedded(prefix = "content_")
        val content: PostField
)

data class PostWithContent(
        @Embedded val post: PostEntity,
        @Relation(
                parentColumn = "id",
                entityColumn = "postId"
        )
        val postContent: PostContentEntity?
)

class DisplayablePostLiveData(
        private val posts: List<PostWithContent>,
        private val authorsLD: LiveData<List<AuthorEntity>>,
        private val categoriesLD: LiveData<List<TermEntity>>,
        private val tagsLD: LiveData<List<TermEntity>>,
        private val featureMediaLD: LiveData<List<FeaturedMediaEntity>>
) : MediatorLiveData<List<DisplayablePost>>() {
    private val eventCount: Map<String, AtomicInteger> = HashMap<String, AtomicInteger>().apply {
        put("authorsLD", AtomicInteger(0))
        put("categoriesLD", AtomicInteger(0))
        put("tagsLD", AtomicInteger(0))
        put("featureMediaLD", AtomicInteger(0))
    }

    private var lastCount = AtomicInteger(0)

    private fun combine() {
        if (eventCount.values.map { it.get() }
                        .any { it <= lastCount.get() }) {
            return
        }

        lastCount.incrementAndGet()

        val authors = authorsLD.value ?: emptyList()
        val categories = categoriesLD.value ?: emptyList()
        val tags = tagsLD.value ?: emptyList()
        val featuredMediaList = featureMediaLD.value ?: emptyList()

        value = posts.map { postEntity ->
            DisplayablePost(
                    postEntity.post.id,
                    postEntity.post.date,
                    postEntity.post.slug,
                    postEntity.post.link,
                    postEntity.post.title,
                    postEntity.postContent?.content ?: PostField(),
                    postEntity.post.excerpt,
                    authors.firstOrNull { it.id == postEntity.post.author },
                    categories.filter { it.id in postEntity.post.categories },
                    tags.filter { it.id in postEntity.post.tags },
                    featuredMediaList.firstOrNull { it.id == postEntity.post.featuredMedia }
            )
        }
    }

    init {
        addSource(authorsLD) {
            if (eventCount["authorsLD"]?.incrementAndGet() == 1 || authorsLD.value?.size != it.size) {
                combine()
            }
        }
        addSource(categoriesLD) {
            if (eventCount["categoriesLD"]?.incrementAndGet() == 1 || categoriesLD.value?.size != it.size) {
                combine()
            }
        }
        addSource(tagsLD) {
            if (eventCount["tagsLD"]?.incrementAndGet() == 1 || tagsLD.value?.size != it.size) {
                combine()
            }
        }
        addSource(featureMediaLD) {
            if (eventCount["featureMediaLD"]?.incrementAndGet() == 1 || featureMediaLD.value?.size != it.size) {
                combine()
            }
        }
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
        val featuredMedia: FeaturedMediaEntity?,
        var theme: PostTheme = PostTheme.Normal
) {
    private val titleAsHtml = "<p><strong class='ssn-post-title'>${title.rendered}</strong></p>"

    private val featuredMediaHtml = featuredMedia?.let { mediaEntity ->
        "<div id=\"attachment_${mediaEntity.id}\" style=\"width: ${mediaEntity.mediaDetails.width}px\" " +
                "class=\"wp-caption aligncenter\">" +
                "           <img aria-describedby=\"caption-attachment-${mediaEntity.id}\" " +
                "                src=\"${mediaEntity.sourceUrl}\" " +
                "                alt=\"\" width=\"${mediaEntity.mediaDetails.width}\" " +
                "                height=\"${mediaEntity.mediaDetails.height}\" " +
                "                class='size-full wp-image-${mediaEntity.id} secureimg_wp' />" +
                (mediaEntity.caption.rendered?.let {
                    "   <div id=\"caption-attachment-${mediaEntity.id}\" " +
                            "class=\"wp-caption-text\">${mediaEntity.caption.rendered}" +
                            "</div>"
                } ?: "") +
                "</div>"
    } ?: ""


    private val styles = "<style>" +
            "   .wp-caption{" +
            "       width:100%!important;" +
            "       margin-top:15px;" +
            "   }" +
            "   img.size-full{" +
            "       width:98%;" +
            "       margin:0px auto;  " +
            "       position:relative;  " +
            "       height: auto;" +
            "   }" +
            "   .wp-caption-text {" +
            "           border-bottom:1px red;" +
            "           padding:0px 8px;" +
            "           font-size:${theme.captionFontSize}px;" +
            "           font-style:italic;" +
            "           color:#888;" +
            "       }" +
            "   div.ssn-post-body_content {" +
            "           margin-top:15px;" +
            "       }" +
            "   iframe{" +
            "       max-width:98%;" +
            "       margin:0px auto;  " +
            "   }" +
            "   strong {" +
            "           font-size:${theme.headingFontSize}px;" +
            "       }" +
            "   strong.ssn-post-title {" +
            "           font-style:italic;" +
            "       }" +
            "   p {" +
            "       font-size:${theme.paraFontSize}px;" +
            "       }" +
            "   p.ssn-post-author, p.ssn-post-date {" +
            "       font-size:${theme.captionFontSize}px;" +
            "       font-style:italic;" +
            "       font-color:#444;" +
            "   }" +
            "   .loader {" +
            "       border: 3px solid #f3f3f3;" +
            "       border-radius: 50%;" +
            "       border-top: 3px solid #3498db;" +
            "       width: 24px;" +
            "       height: 24px;" +
            "       -webkit-animation: spin 2s linear infinite; /* Safari */" +
            "       animation: spin 2s linear infinite;" +
            "       margin: 0px auto;" +
            "   }" +
            "   /* Safari */" +
            "   @-webkit-keyframes spin {" +
            "       0% { -webkit-transform: rotate(0deg); }" +
            "       100% { -webkit-transform: rotate(360deg); }" +
            "   }" +
            "   @keyframes spin {" +
            "       0% { transform: rotate(0deg); }" +
            "       100% { transform: rotate(360deg); }" +
            "   }" +
            "</style>"

    fun asHtml(): String? =
            "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<title>${title.rendered}</title>" +
                    "<meta name=\"viewport\" content=\"initial-scale=1.0, maximum-scale=3.0, user-scalable=no, width=device-width\">" +
                    styles +
                    "</head>" +
                    "<body class='ssn-post-body'>" +
                    "   $titleAsHtml" +
                    (author?.name?.let { "<p class='ssn-post-author'>$it</p>" } ?: "") +
                    "<p class='ssn-post-date'>${SimpleDateFormat("dd MMM, YYYY hh:mm a", Locale.getDefault()).format(date)}</p>" +
                    "   $featuredMediaHtml" +
                    "   <div class='ssn-post-body_content'>" +
                    "       ${content.rendered?.notEmpty() ?: "<div class=\"loader\"></div>"}" +
                    "   </div>" +
                    "</body>" +
                    "</html>"


}

sealed class PostTheme(
        val captionFontSize: Int,
        val paraFontSize: Int,
        val headingFontSize: Int,
        @StringRes val nameId: Int
) {
    object ExtraSmall : PostTheme(
            8,
            12, 14,
            com.sikhsiyasat.wordpress.R.string.text_extra_small
    )

    object Small : PostTheme(
            10,
            14, 16,
            com.sikhsiyasat.wordpress.R.string.text_small
    )

    object Normal : PostTheme(12,
            16, 18,
            com.sikhsiyasat.wordpress.R.string.text_normal
    )

    object Large : PostTheme(14,
            18, 20,
            com.sikhsiyasat.wordpress.R.string.text_large
    )

    object ExtraLarge : PostTheme(16,
            20, 22,
            com.sikhsiyasat.wordpress.R.string.text_extra_large
    )

    fun next(): PostTheme {
        return when (this) {
            ExtraSmall -> Small
            Small -> Normal
            Normal -> Large
            Large -> ExtraLarge
            ExtraLarge -> ExtraSmall
        }
    }
}

@Database(
        entities = [
            PostEntity::class, PostContentEntity::class, AuthorEntity::class,
            TermEntity::class, FeaturedMediaEntity::class, PostTermEntity::class
        ],
        version = 1
)
@TypeConverters(Converters::class)
abstract class WordpressDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun postContentDao(): PostContentDao
    abstract fun postTermDao(): PostTermDao
    abstract fun authorDao(): AuthorDao
    abstract fun termDao(): TermDao
    abstract fun featuredMediaDao(): FeaturedMediaDao
}


@Dao
interface PostDao {
    @Insert(onConflict = REPLACE)
    fun save(post: Set<PostEntity>)

    @Query("SELECT * FROM post WHERE link = :link order by date desc limit 1")
    fun loadByLink(link: String): LiveData<PostWithContent?>

    @Query("SELECT * FROM post WHERE link like :likeExp order by date desc")
    fun loadWhereLinkLike(likeExp: String): LiveData<List<PostEntity>>

    @Query("SELECT * FROM post")
    fun getAll(): List<PostEntity>

    @Query("SELECT * FROM post WHERE link like :likeExp and id not in (:exceptIds) and (id in (:ids) or author in (:authorIds)) order by date desc")
    fun findPosts(likeExp: String, ids: Set<String>, authorIds: Set<String>, exceptIds: Set<String>): LiveData<List<PostEntity>>

    @Query("SELECT * FROM post WHERE link like :likeExp and id not in (:exceptIds) and (id in (:ids) or author in (:authorIds)) order by date desc limit 10")
    fun findTopPosts(likeExp: String, ids: Set<String>, authorIds: Set<String>, exceptIds: Set<String>): LiveData<List<PostEntity>>
}

@Dao
interface PostContentDao {
    @Insert(onConflict = REPLACE)
    fun save(post: Set<PostContentEntity>)
}

@Dao
interface PostTermDao {
    @Insert(onConflict = REPLACE)
    fun save(posts: List<PostTermEntity>)

    @Query("SELECT distinct(postId) FROM post_term WHERE termId in (:terms)")
    fun findPostsByTermIdIn(terms: Set<String>): LiveData<List<String>>

    @Query("SELECT * FROM post_term WHERE postId in (:posts)")
    fun findByPostIds(posts: List<String>): List<PostTermEntity>
}

@Dao
interface AuthorDao {
    @Insert(onConflict = REPLACE)
    fun save(author: AuthorEntity)

    @Insert(onConflict = REPLACE)
    fun save(authors: Set<AuthorEntity>)

    @Query("SELECT * FROM author WHERE id in (:ids)")
    fun findByIdIn(ids: Set<String>): List<AuthorEntity>
}

@Dao
interface TermDao {
    @Insert(onConflict = REPLACE)
    fun save(post: Set<TermEntity>)

    @Query("SELECT * FROM term WHERE id in (:ids)")
    fun findByIdIn(ids: List<String>): List<TermEntity>

    @Query("SELECT * FROM term WHERE id in (:ids)")
    fun findLDByIdIn(ids: List<String>): LiveData<List<TermEntity>>
}

@Dao
interface FeaturedMediaDao {
    @Insert(onConflict = REPLACE)
    fun save(media: Set<FeaturedMediaEntity>)

    @Query("SELECT * FROM feature_media WHERE id in (:ids)")
    fun load(ids: Set<String>): LiveData<List<FeaturedMediaEntity>>

    @Query("SELECT * FROM feature_media WHERE id in (:ids)")
    fun getByIdInNow(ids: List<String>): List<FeaturedMediaEntity>
}

object PostMapper {
    fun postEntity(post: Post): PostEntity? {
        return PostEntity(
                post.id!!,
                post.date ?: Date(0),
                post.modifiedGmt ?: Date(0),
                post.slug!!,
                post.link!!,
                post.title ?: PostField(),
                post.excerpt ?: PostField(),
                post.author,
                post.categories ?: emptyList(),
                post.tags ?: emptyList(),
                post.featuredMedia ?: ""
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
                    featuredMedia.caption ?: PostField(),
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
    fun fromStringList(value: List<*>): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(string: String?): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(string, listType)
    }

    @TypeConverter
    fun fromTermTaxonomy(value: TermTaxonomyEntity?): String? = value?.name

    @TypeConverter
    fun toTermTaxonomy(string: String?): TermTaxonomyEntity? =
            string?.let { TermTaxonomyEntity.valueOf(it) }
}
