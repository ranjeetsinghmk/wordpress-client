package com.sikhsiyasat.wordpress.models

import android.text.Html
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.util.*

//TODO separate out api & core classes
data class Post(
    val id: String?,
    val date: Date?,
    val slug: String?,
    val link: String?,
    val title: PostField?,
    val content: PostField?,
    val excerpt: PostField?,
    val author: Int,
    val categories: List<Int>?,
    val tags: List<Int>?,
    val featuredMedia: String?,
    @SerializedName("_embedded")
    val embeddedData: PostEmbeddedData? = null
) {
    constructor(post: PostEntity) : this(
        post.id, post.date, post.slug, post.link,
        post.title, post.content, post.excerpt, post.author, post.categories,
        post.tags, post.featuredMedia
    )
}

data class PostEmbeddedData(
    val author: List<Author>,
    @SerializedName("wp:featuredmedia")
    val featuredMedia: List<FeaturedMedia>,
    @SerializedName("wp:term")
    val terms: List<List<Term>>
)


@Entity(indices = [Index("id", "name", "taxonomy")])
data class Term(
    @PrimaryKey
    val id: Int,
    val link: String,
    val name: String,
    val slug: String,
    val taxonomy: TermTaxonomy
)

enum class TermTaxonomy {
    category, post_tag
}

@Entity(indices = [Index("id", "mimeType", "taxonomy")])
data class FeaturedMedia(
    @PrimaryKey
    val id: String,
    val type: String,
    val sourceUrl: String,
    val mimeType: String,
    val mediaDetails: MediaDetails
)

data class MediaDetails(
    val width: Int,
    val height: Int
)

@Entity(indices = [Index("id", "name", "slug")])
data class Author(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val description: String,
    val link: String,
    val slug: String,
    val avatarUrls: AvatarUrls
)

data class AvatarUrls(
    @SerializedName("24")
    val twentyFour: String,
    @SerializedName("48")
    val fortyEight: String,
    @SerializedName("96")
    val ninetySix: String
)


data class PostField(var rendered: String? = "", var protected: Boolean = false) {
    val spannedText: String
        get() = Html.fromHtml(rendered).toString()
}


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
    private val postLD: LiveData<PostEntity?>,
    private val authorLD: LiveData<Author?>,
    private val categoriesLD: LiveData<List<Term>>,
    private val tagsLD: LiveData<List<Term>>,
    private val featureMediaLD: LiveData<FeaturedMedia?>
) : MediatorLiveData<DisplayablePost?>() {
    private fun combine() {
        val postEntity = postLD.value
        val author = authorLD.value
        val categories = categoriesLD.value
        val tags = tagsLD.value
        val featuredMedia = featureMediaLD.value

        value =
            if (postEntity == null || author == null || categories == null || tags == null || featuredMedia == null) {
                null
            } else {
                DisplayablePost(
                    postEntity.id,
                    postEntity.date,
                    postEntity.slug,
                    postEntity.link,
                    postEntity.title,
                    postEntity.content,
                    postEntity.excerpt,
                    author, categories, tags, featuredMedia
                )
            }

    }

    init {
        addSource(postLD) { combine() }
        addSource(authorLD) { combine() }
        addSource(categoriesLD) { combine() }
        addSource(tagsLD) { combine() }
        addSource(featureMediaLD) { combine() }
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
    val author: Author,
    val categories: List<Term>,
    val tags: List<Term>,
    val featuredMedia: FeaturedMedia
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

@Database(entities = [PostEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class WordpressDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}


@Dao
interface PostDao {
    @Insert(onConflict = REPLACE)
    fun save(post: PostEntity)

    @Insert(onConflict = REPLACE)
    fun save(post: List<PostEntity>)

    @Query("SELECT * FROM post WHERE link = :link")
    fun loadByLink(link: String): LiveData<PostEntity?>

    @Query("SELECT * FROM post WHERE link like :likeExp")
    fun loadWhereLinkLike(likeExp: String): LiveData<List<PostEntity>>
}

//TODO implement daossssss


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
}
