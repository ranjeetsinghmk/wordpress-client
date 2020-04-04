package com.sikhsiyasat.wordpress.api

import android.text.Html
import com.google.gson.annotations.SerializedName
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
        val featuredMedia: String?,
        @SerializedName("_embedded")
        val embeddedData: PostEmbeddedData? = null
)

data class PostEmbeddedData(
        val author: List<Author>,
        @SerializedName("wp:featuredmedia")
        val featuredMedia: List<FeaturedMedia>,
        @SerializedName("wp:term")
        val terms: List<List<Term>>
)


data class PostField(var rendered: String? = "", var protected: Boolean = false) {
    val spannedText: String
        get() = Html.fromHtml(rendered).toString()
}

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

