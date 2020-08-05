package com.sikhsiyasat.wordpress.models

data class PostsSearchParams(
        val websiteUrl: String,
        val categories: Set<String> = emptySet(),
        val tags: Set<String> = emptySet(),
        val authors: Set<String> = emptySet()
)