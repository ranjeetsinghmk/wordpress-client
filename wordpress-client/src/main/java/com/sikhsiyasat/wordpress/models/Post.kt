package com.sikhsiyasat.wordpress.models

import android.text.Html
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*


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
    val featuredMedia: String?
) {
    constructor(post: PostEntity) : this(
        post.id, post.date, post.slug, post.link,
        post.title, post.content, post.excerpt, post.author, post.categories,
        post.tags, post.featuredMedia
    )

    fun asHtml(): String = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<title>${title?.rendered}</title>\n" +
            "<meta name=\"viewport\" content=\"width=device-width, user-scalable=no\" />\n" +
            "<body>\n" +
            "${content?.rendered}" +
            "</body>\n" +
            "</html>"

    companion object {
        @JvmStatic
        fun dummyPost(): Post {
            return Post(
                "",
                Date(),
                "",
                "",
                PostField(),
                PostField(),
                PostField(),
                0,
                emptyList(),
                emptyList(),
                ""
            )
        }
    }
}

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
) {
    constructor(post: Post) : this(
        post.id ?: "", post.date ?: Date(),
        post.slug ?: "", post.link ?: "",
        post.title ?: PostField(), post.content ?: PostField(),
        post.excerpt ?: PostField(), post.author, post.categories ?: emptyList(),
        post.tags ?: emptyList(), post.featuredMedia ?: ""
    )
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
