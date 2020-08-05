package com.sikhsiyasat.wordpress.api

import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

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
                            emitter.onComplete()
                        }

                        override fun onResponse(
                                call: Call<List<Post>>,
                                response: Response<List<Post>>
                        ) {
                            emitter.onNext(
                                    response.body()
                                            ?.firstOrNull()
                                            ?.let { post ->
                                                ApiResponse.Success(
                                                        1, 1, response.headers()["X-WP-Total"]?.toInt()
                                                        ?: 1, post
                                                )
                                            } ?: ApiResponse.Error(
                                            ApiError(
                                                    "No result found"
                                            )
                                    )
                            )
                            emitter.onComplete()
                        }
                    })
                }
            })

    override fun getPosts(
            page: Int,
            perPage: Int,
            tags: List<String>,
            authors: List<String>,
            categories: List<String>,
            after: Date,
            context: String
    ): Observable<ApiResponse<List<Post>>> =
            Observable.create(object : ObservableOnSubscribe<ApiResponse<List<Post>>> {
                override fun subscribe(emitter: ObservableEmitter<ApiResponse<List<Post>>>) {
                    val postsCall = webserviceInternal.getPosts(
                            page, perPage, context = context,
                            authors = authors,
                            categories = categories,
                            tags = tags,
                            after = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss", Locale.ENGLISH).format(after)
                    )
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

//                            response.headers()["X-WP-TotalPages"]?.toInt()
                            emitter.onNext(
                                    response.body()?.let { posts ->
                                        ApiResponse.Success(
                                                page, perPage, response.headers()["X-WP-Total"]?.toInt()
                                                ?: 1,
                                                posts
                                        )
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
}