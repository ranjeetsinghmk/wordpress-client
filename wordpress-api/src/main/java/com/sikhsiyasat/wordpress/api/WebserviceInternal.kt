package com.sikhsiyasat.wordpress.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Internal Retrofit interface for WordPress REST API v2 endpoints.
 * Base URL: {website_url}/wp-json/wp/v2/
 * 
 * Reference: https://developer.wordpress.org/rest-api/reference/
 */
interface WebserviceInternal {
    
    // Posts Endpoints
    // Reference: https://developer.wordpress.org/rest-api/reference/posts/
    
    @GET("posts")
    fun getPost(@Query("slug") slug: String, @Query("_embed") embed: String = ""): Call<List<Post>>

    @GET("posts")
    fun getPosts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("context") context: String = "embed",
        @Query("_embed") embed: String = ""
    ): Call<List<Post>>
    
    @GET("posts")
    fun getPostsByCategory(
        @Query("categories") categoryId: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("_embed") embed: String = ""
    ): Call<List<Post>>
    
    @GET("posts")
    fun getPostsByTag(
        @Query("tags") tagId: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("_embed") embed: String = ""
    ): Call<List<Post>>
    
    @GET("posts")
    fun searchPosts(
        @Query("search") searchTerm: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("_embed") embed: String = ""
    ): Call<List<Post>>
    
    @GET("posts/{id}")
    fun getPostById(
        @Path("id") postId: Int,
        @Query("_embed") embed: String = ""
    ): Call<Post>
    
    // Categories Endpoints
    // Reference: https://developer.wordpress.org/rest-api/reference/categories/
    
    @GET("categories")
    fun getCategories(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
        @Query("orderby") orderBy: String = "count",
        @Query("order") order: String = "desc"
    ): Call<List<Category>>
    
    @GET("categories/{id}")
    fun getCategoryById(@Path("id") categoryId: Int): Call<Category>
    
    // Tags Endpoints
    // Reference: https://developer.wordpress.org/rest-api/reference/tags/
    
    @GET("tags")
    fun getTags(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
        @Query("orderby") orderBy: String = "count",
        @Query("order") order: String = "desc"
    ): Call<List<Tag>>
    
    @GET("tags/{id}")
    fun getTagById(@Path("id") tagId: Int): Call<Tag>
    
    // Search Endpoint
    // Reference: https://developer.wordpress.org/rest-api/reference/search-results/
    
    @GET("search")
    fun search(
        @Query("search") searchTerm: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("type") type: String = "post",
        @Query("subtype") subtype: String = "post"
    ): Call<List<SearchResult>>
}
