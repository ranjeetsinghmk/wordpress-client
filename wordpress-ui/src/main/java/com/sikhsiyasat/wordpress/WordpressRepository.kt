package com.sikhsiyasat.wordpress

import android.net.Uri
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sikhsiyasat.logger.SSLogger
import com.sikhsiyasat.wordpress.models.DisplayablePost
import com.sikhsiyasat.wordpress.models.DisplayablePostLiveData
import com.sikhsiyasat.wordpress.models.Post
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

//TODO separate API vs CORE
class WordpressRepository(
    private val webClient: WebClient,
    private val localStorageService: LocalStorageService
) {

    private val logger: SSLogger = SSLogger.getLogger("Wordpress repository")

    fun getPost(postUrl: String): LiveData<DisplayablePost?> {
        refreshPost(postUrl)
        return DisplayablePostLiveData(
            localStorageService.getPost(postUrl)
        )
    }

    private fun refreshPost(postUrl: String): LiveData<Post> {
        // This isn't an optimal implementation. We'll fix it later.
        val data = MutableLiveData<Post>()
        extractSlug(postUrl)?.let {
            extractWebsiteUrl(postUrl)?.let { it1 ->
                webClient.webService(it1)
                    .getPost(it)
                    .enqueue(object : Callback<List<Post>> {
                        override fun onResponse(
                            call: Call<List<Post>>,
                            response: Response<List<Post>>
                        ) {
                            response.body()?.let { localStorageService.savePosts(it) }
                        }

                        // Error case is left out for brevity.
                        override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                            logger.error("on failure", t)
                        }
                    })
            }
        }
        return data
    }

    private fun loadPosts(websiteUrl: String, page: Int): LiveData<Post> {
        // This isn't an optimal implementation. We'll fix it later.
        val data = MutableLiveData<Post>()
        webClient.webService(websiteUrl)
            .getPosts(page = page)
            .enqueue(object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                    response.body()?.let { localStorageService.savePosts(it) }
                }

                // Error case is left out for brevity.
                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                    logger.error("on failure", t)
                }
            })
        return data
    }

    fun getPosts(websiteUrl: String): LiveData<List<Post>> {
        loadPosts(websiteUrl, 1)
        return localStorageService.getPosts(websiteUrl)
    }

    private fun extractSlug(postUrl: String): String? =
        Uri.parse(postUrl).pathSegments.first { !it.isDigitsOnly() }

    private fun extractWebsiteUrl(postUrl: String): String? = Uri.parse(postUrl)
        .let { it.scheme + "://" + it.host }
}
