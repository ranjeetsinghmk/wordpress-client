package com.sikhsiyasat.wordpress.api

import retrofit2.Call
import retrofit2.Response

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
}