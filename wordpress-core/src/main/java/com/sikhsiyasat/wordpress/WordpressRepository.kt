package com.sikhsiyasat.wordpress

import android.net.Uri
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.sikhsiyasat.logger.SSLogger
import com.sikhsiyasat.wordpress.api.*
import com.sikhsiyasat.wordpress.models.DisplayablePost
import com.sikhsiyasat.wordpress.models.DisplayablePostLiveData
import com.sikhsiyasat.wordpress.models.PostEntity

//TODO separate API vs CORE
class WordpressRepository(
    private val webClient: WebClient,
    private val localStorageService: LocalStorageService
) {

    private val logger: SSLogger = SSLogger.getLogger("Wordpress repository")

    fun getPost(postUrl: String): LiveData<DisplayablePost?> {
        reloadPosts(postUrl)

        return Transformations.map(
            Transformations.switchMap(
                localStorageService.getPost(postUrl)
            ) { post ->
                post?.let { displayablePostLiveData(listOf(it)) }
                    ?: displayablePostLiveData(emptyList())
            }
        ) { posts ->
            posts.firstOrNull()
        }
    }

    private fun displayablePostLiveData(posts: List<PostEntity>): DisplayablePostLiveData {
        return DisplayablePostLiveData(
            posts,
            localStorageService.getAuthors(
                posts.map { it.author }
                    .map { it.toString() }
                    .distinct()
            ),
            localStorageService.getCategories(
                posts.flatMap { it.categories }
                    .map { it.toString() }
                    .distinct()
            ),
            localStorageService.getTags(
                posts.flatMap { it.tags }
                    .map { it.toString() }
                    .distinct()
            ),
            localStorageService.getFeaturedMedia(
                posts.map { it.featuredMedia }
                    .distinct()
            )
        )
    }

    fun reloadPosts(postUrl: String) {
        extractSlug(postUrl)?.let {
            extractWebsiteUrl(postUrl)?.let { it1 ->
                webClient.webService(it1)
                    .getPost(it)
                    .subscribe(object : ObservableObserver<ApiResponse<Post>> {
                        override fun onSubscribe(d: ApiResponse<Post>) {
                            when (d) {
                                is ApiResponse.Success<Post> -> localStorageService.savePosts(
                                    listOf(
                                        d.data
                                    )
                                )
                                is ApiResponse.Error -> {
                                    d.error
                                }
                            }
                        }

                        override fun onComplete() {

                        }

                        override fun onError(e: ApiError) {

                        }
                    })
            }
        }

    }

    fun loadPosts(websiteUrl: String, page: Int) {
        webClient.webService(websiteUrl)
            .getPosts(page = page)
            .subscribe(object : ObservableObserver<ApiResponse<List<Post>>> {
                override fun onSubscribe(d: ApiResponse<List<Post>>) {
                    when (d) {
                        is ApiResponse.Success<List<Post>> -> localStorageService.savePosts(d.data)
                        is ApiResponse.Error -> {
                            d.error
                        }
                    }
                }

                override fun onComplete() {
                }

                override fun onError(e: ApiError) {
                }
            })
    }

    fun getPosts(websiteUrl: String): LiveData<List<DisplayablePost>> {
        loadPosts(websiteUrl, 1)
        return Transformations.switchMap(
            localStorageService.getPosts(websiteUrl)
        ) { displayablePostLiveData(it) }
    }

    private fun extractSlug(postUrl: String): String? =
        Uri.parse(postUrl).pathSegments.firstOrNull { !it.isDigitsOnly() }

    private fun extractWebsiteUrl(postUrl: String): String? = Uri.parse(postUrl)
        .let { it.scheme + "://" + it.host }
}
