package com.sikhsiyasat.wordpress.api

import retrofit2.Call
import retrofit2.Response

/**
 * Implementation of WebService that wraps Retrofit calls in Observable pattern.
 * Handles async API calls and error handling.
 */
class WebServiceImpl(private val webserviceInternal: WebserviceInternal) :
    WebService {

    override fun getPost(slug: String): Observable<ApiResponse<Post>> =
        Observable.create(object : ObservableOnSubscribe<ApiResponse<Post>> {
            override fun subscribe(emitter: ObservableEmitter<ApiResponse<Post>>) {
                val postsCall = webserviceInternal.getPost(slug)
                postsCall.enqueue(object : retrofit2.Callback<List<Post>> {
                    override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                        emitter.onNext(
                            ApiResponse.Error(
                                ApiError(
                                    t.message
                                        ?: "Some unknown error occurred"
                                )
                            )
                        )
                    }

                    override fun onResponse(
                        call: Call<List<Post>>,
                        response: Response<List<Post>>
                    ) {
                        emitter.onNext(
                            response.body()
                                ?.firstOrNull()
                                ?.let { post ->
                                    ApiResponse.Success(post)
                                } ?: ApiResponse.Error(
                                ApiError(
                                    "No result found"
                                )
                            )
                        )
                    }
                })
            }
        })

    override fun getPosts(
        page: Int,
        perPage: Int,
        context: String
    ): Observable<ApiResponse<List<Post>>> =
        Observable.create(object : ObservableOnSubscribe<ApiResponse<List<Post>>> {
            override fun subscribe(emitter: ObservableEmitter<ApiResponse<List<Post>>>) {
                val postsCall = webserviceInternal.getPosts(page, perPage, context)
                postsCall.enqueue(object : retrofit2.Callback<List<Post>> {
                    override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                        emitter.onNext(
                            ApiResponse.Error(
                                ApiError(
                                    t.message
                                        ?: "Some unknown error occurred"
                                )
                            )
                        )
                    }

                    override fun onResponse(
                        call: Call<List<Post>>,
                        response: Response<List<Post>>
                    ) {
                        emitter.onNext(
                            response.body()?.let {
                                ApiResponse.Success(it)
                            } ?: ApiResponse.Error(
                                ApiError(
                                    "No result found"
                                )
                            )
                        )
                    }
                })
            }
        })
    
    override fun getPostsByCategory(
        categoryId: Int,
        page: Int,
        perPage: Int
    ): Observable<ApiResponse<List<Post>>> =
        Observable.create(object : ObservableOnSubscribe<ApiResponse<List<Post>>> {
            override fun subscribe(emitter: ObservableEmitter<ApiResponse<List<Post>>>) {
                val postsCall = webserviceInternal.getPostsByCategory(categoryId, page, perPage)
                postsCall.enqueue(object : retrofit2.Callback<List<Post>> {
                    override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                        emitter.onNext(
                            ApiResponse.Error(
                                ApiError(
                                    t.message ?: "Failed to fetch posts by category"
                                )
                            )
                        )
                    }

                    override fun onResponse(
                        call: Call<List<Post>>,
                        response: Response<List<Post>>
                    ) {
                        emitter.onNext(
                            response.body()?.let {
                                ApiResponse.Success(it)
                            } ?: ApiResponse.Error(
                                ApiError("No posts found for category")
                            )
                        )
                    }
                })
            }
        })
    
    override fun getPostsByTag(
        tagId: Int,
        page: Int,
        perPage: Int
    ): Observable<ApiResponse<List<Post>>> =
        Observable.create(object : ObservableOnSubscribe<ApiResponse<List<Post>>> {
            override fun subscribe(emitter: ObservableEmitter<ApiResponse<List<Post>>>) {
                val postsCall = webserviceInternal.getPostsByTag(tagId, page, perPage)
                postsCall.enqueue(object : retrofit2.Callback<List<Post>> {
                    override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                        emitter.onNext(
                            ApiResponse.Error(
                                ApiError(
                                    t.message ?: "Failed to fetch posts by tag"
                                )
                            )
                        )
                    }

                    override fun onResponse(
                        call: Call<List<Post>>,
                        response: Response<List<Post>>
                    ) {
                        emitter.onNext(
                            response.body()?.let {
                                ApiResponse.Success(it)
                            } ?: ApiResponse.Error(
                                ApiError("No posts found for tag")
                            )
                        )
                    }
                })
            }
        })
    
    override fun searchPosts(
        searchTerm: String,
        page: Int,
        perPage: Int
    ): Observable<ApiResponse<List<Post>>> =
        Observable.create(object : ObservableOnSubscribe<ApiResponse<List<Post>>> {
            override fun subscribe(emitter: ObservableEmitter<ApiResponse<List<Post>>>) {
                val postsCall = webserviceInternal.searchPosts(searchTerm, page, perPage)
                postsCall.enqueue(object : retrofit2.Callback<List<Post>> {
                    override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                        emitter.onNext(
                            ApiResponse.Error(
                                ApiError(
                                    t.message ?: "Search failed"
                                )
                            )
                        )
                    }

                    override fun onResponse(
                        call: Call<List<Post>>,
                        response: Response<List<Post>>
                    ) {
                        emitter.onNext(
                            response.body()?.let {
                                ApiResponse.Success(it)
                            } ?: ApiResponse.Error(
                                ApiError("No search results found")
                            )
                        )
                    }
                })
            }
        })
    
    override fun getCategories(
        page: Int,
        perPage: Int
    ): Observable<ApiResponse<List<Category>>> =
        Observable.create(object : ObservableOnSubscribe<ApiResponse<List<Category>>> {
            override fun subscribe(emitter: ObservableEmitter<ApiResponse<List<Category>>>) {
                val categoriesCall = webserviceInternal.getCategories(page, perPage)
                categoriesCall.enqueue(object : retrofit2.Callback<List<Category>> {
                    override fun onFailure(call: Call<List<Category>>, t: Throwable) {
                        emitter.onNext(
                            ApiResponse.Error(
                                ApiError(
                                    t.message ?: "Failed to fetch categories"
                                )
                            )
                        )
                    }

                    override fun onResponse(
                        call: Call<List<Category>>,
                        response: Response<List<Category>>
                    ) {
                        emitter.onNext(
                            response.body()?.let {
                                ApiResponse.Success(it)
                            } ?: ApiResponse.Error(
                                ApiError("No categories found")
                            )
                        )
                    }
                })
            }
        })
    
    override fun getTags(
        page: Int,
        perPage: Int
    ): Observable<ApiResponse<List<Tag>>> =
        Observable.create(object : ObservableOnSubscribe<ApiResponse<List<Tag>>> {
            override fun subscribe(emitter: ObservableEmitter<ApiResponse<List<Tag>>>) {
                val tagsCall = webserviceInternal.getTags(page, perPage)
                tagsCall.enqueue(object : retrofit2.Callback<List<Tag>> {
                    override fun onFailure(call: Call<List<Tag>>, t: Throwable) {
                        emitter.onNext(
                            ApiResponse.Error(
                                ApiError(
                                    t.message ?: "Failed to fetch tags"
                                )
                            )
                        )
                    }

                    override fun onResponse(
                        call: Call<List<Tag>>,
                        response: Response<List<Tag>>
                    ) {
                        emitter.onNext(
                            response.body()?.let {
                                ApiResponse.Success(it)
                            } ?: ApiResponse.Error(
                                ApiError("No tags found")
                            )
                        )
                    }
                })
            }
        })
    
    override fun search(
        searchTerm: String,
        page: Int,
        perPage: Int
    ): Observable<ApiResponse<List<SearchResult>>> =
        Observable.create(object : ObservableOnSubscribe<ApiResponse<List<SearchResult>>> {
            override fun subscribe(emitter: ObservableEmitter<ApiResponse<List<SearchResult>>>) {
                val searchCall = webserviceInternal.search(searchTerm, page, perPage)
                searchCall.enqueue(object : retrofit2.Callback<List<SearchResult>> {
                    override fun onFailure(call: Call<List<SearchResult>>, t: Throwable) {
                        emitter.onNext(
                            ApiResponse.Error(
                                ApiError(
                                    t.message ?: "Search failed"
                                )
                            )
                        )
                    }

                    override fun onResponse(
                        call: Call<List<SearchResult>>,
                        response: Response<List<SearchResult>>
                    ) {
                        emitter.onNext(
                            response.body()?.let {
                                ApiResponse.Success(it)
                            } ?: ApiResponse.Error(
                                ApiError("No search results found")
                            )
                        )
                    }
                })
            }
        })
}