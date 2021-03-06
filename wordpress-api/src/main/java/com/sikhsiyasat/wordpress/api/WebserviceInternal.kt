package com.sikhsiyasat.wordpress.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WebserviceInternal {
    @GET("posts")
    fun getPost(@Query("slug") slug: String, @Query("_embed") embed: String = ""): Call<List<Post>>

    @GET("posts")
    fun getPosts(
        @Query("page") page: Int = 0,
        @Query("per_page") perPage: Int = 10,
        @Query("context") context: String = "embed",
        @Query("_embed") embed: String = ""
    ): Call<List<Post>>
}
