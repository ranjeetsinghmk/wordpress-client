package com.sikhsiyasat.wordpress

import com.sikhsiyasat.wordpress.models.Post
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WebserviceInternal {
    @GET("posts")
    fun getPost(@Query("slug") slug: String): Call<List<Post>>

    @GET("posts")
    fun getPosts(
        @Query("page") page: Int = 0,
        @Query("per_page") perPage: Int = 10,
        @Query("context") context: String = "embed"
    ): Call<List<Post>>
}
