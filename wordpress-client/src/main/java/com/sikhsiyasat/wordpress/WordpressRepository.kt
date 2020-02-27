package com.sikhsiyasat.wordpress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sikhsiyasat.logger.SSLogger
import com.sikhsiyasat.wordpress.models.Post
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WordpressRepository(
    private val webClient: WebClient,
    private val localStorageService: LocalStorageService
) {

    private val logger: SSLogger = SSLogger.getLogger("Wordpress repository")

    fun getPost(postUrl: String): LiveData<Post?> {
        refreshPost(postUrl)
        return localStorageService.getPost(postUrl)
    }

    private fun refreshPost(postUrl: String): LiveData<Post> {
        // This isn't an optimal implementation. We'll fix it later.
        val data = MutableLiveData<Post>()
        webClient.webService("")
            .getPost(extractSlug(postUrl))
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

    private fun loadPosts(page: Int): LiveData<Post> {
        // This isn't an optimal implementation. We'll fix it later.
        val data = MutableLiveData<Post>()
        webClient.webService("")
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

    private fun extractSlug(postUrl: String) = postUrl
        .replace(Regex(".*[0-9]+/[0-9]{2}"), "")
        .split("/")
        .first { it.isNotEmpty() }

    fun getPosts(websiteUrl: String): LiveData<List<Post>> {
        loadPosts(1)
        return localStorageService.getPosts(websiteUrl)
    }
}
