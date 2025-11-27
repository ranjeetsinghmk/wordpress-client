package com.sikhsiyasat.wordpress.api

import android.text.Html
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * WordPress REST API Post model.
 * Reference: https://developer.wordpress.org/rest-api/reference/posts/
 */
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
)

/**
 * Embedded data included when using _embed parameter.
 * Contains author, featured media, and taxonomy terms.
 */
data class PostEmbeddedData(
        val author: List<Author>,
        @SerializedName("wp:featuredmedia")
        val featuredMedia: List<FeaturedMedia>,
        @SerializedName("wp:term")
        val terms: List<List<Term>>
)

/**
 * Represents rendered content fields in WordPress posts.
 */
data class PostField(var rendered: String? = "", var protected: Boolean = false) {
    val spannedText: String
        get() = Html.fromHtml(rendered).toString()
}

/**
 * WordPress REST API Term model (used in embedded data).
 * Reference: https://developer.wordpress.org/rest-api/reference/categories/
 */
data class Term(
        val id: Int,
        val link: String,
        val name: String,
        val slug: String,
        val taxonomy: TermTaxonomy
)

enum class TermTaxonomy {
    category, post_tag
}

/**
 * WordPress REST API Featured Media model.
 * Reference: https://developer.wordpress.org/rest-api/reference/media/
 */
data class FeaturedMedia(
        val id: String,
        val type: String,
        val sourceUrl: String,
        val mimeType: String,
        val caption: PostField?,
        val mediaDetails: MediaDetails
)

data class MediaDetails(
        val width: Int,
        val height: Int
)

/**
 * WordPress REST API Author/User model.
 * Reference: https://developer.wordpress.org/rest-api/reference/users/
 */
data class Author(
        val id: String,
        val name: String,
        val url: String,
        val description: String,
        val link: String,
        val slug: String,
        val avatarUrls: AvatarUrls?
)

data class AvatarUrls(
        @SerializedName("24")
        val twentyFour: String,
        @SerializedName("48")
        val fortyEight: String,
        @SerializedName("96")
        val ninetySix: String
)

/**
 * WordPress REST API Category model.
 * Reference: https://developer.wordpress.org/rest-api/reference/categories/
 */
data class Category(
        val id: Int,
        val count: Int,
        val description: String,
        val link: String,
        val name: String,
        val slug: String,
        val taxonomy: String,
        val parent: Int
)

/**
 * WordPress REST API Tag model.
 * Reference: https://developer.wordpress.org/rest-api/reference/tags/
 */
data class Tag(
        val id: Int,
        val count: Int,
        val description: String,
        val link: String,
        val name: String,
        val slug: String,
        val taxonomy: String
)

/**
 * WordPress REST API Search Result model.
 * Reference: https://developer.wordpress.org/rest-api/reference/search-results/
 */
data class SearchResult(
        val id: Int,
        val title: String,
        val url: String,
        val type: String,
        val subtype: String
)

