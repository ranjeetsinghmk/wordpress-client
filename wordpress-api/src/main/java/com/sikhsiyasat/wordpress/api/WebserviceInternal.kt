package com.sikhsiyasat.wordpress.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WebserviceInternal {
    @GET("posts")
    fun getPost(
            @Query("slug") slug: String,
            @Query("_embed") embed: String = "" //Used to get full objects for reference fields embedded into the response
    ): Call<List<Post>>

    @GET("posts")
    fun getPosts(
            @Query("page") page: Int = 0,
            @Query("per_page") perPage: Int = 10,
            @Query("tags") tags: List<String> = emptyList(),
            @Query("author") authors: List<String> = emptyList(),
            @Query("categories") categories: List<String> = emptyList(),
            @Query("after") after: String?,
            @Query("context") context: String = "embed", //Won't return heavy/extra fields in the response
            @Query("_embed") embed: String = "" //Used to get full objects for reference fields embedded into the response
    ): Call<List<Post>>
}
