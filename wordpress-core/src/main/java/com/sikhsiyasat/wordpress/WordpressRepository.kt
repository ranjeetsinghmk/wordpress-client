package com.sikhsiyasat.wordpress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.sikhsiyasat.logger.SSLogger
import com.sikhsiyasat.wordpress.api.*
import com.sikhsiyasat.wordpress.models.*

//TODO separate API vs CORE
class WordpressRepository(
        private val webClient: WebClient,
        private val localStorageService: LocalStorageService,
        private val preferences: WordpressPreferences,
        var clientStorage: ClientStorage? = null
) {
    private val log: SSLogger = SSLogger.getLogger("Wordpress repository")

    fun getPost(postUrl: String): LiveData<DisplayablePost?> {
        loadPost(postUrl)

        return Transformations.map(
                localStorageService.getPost(postUrl)
        ) {
            it?.let { post ->
                fetchAndFillAdditionalFields(listOf(post))
                        .firstOrNull()
            }
        }
    }

    private fun fetchAndFillAdditionalFields(posts: List<PostWithContent>): List<DisplayablePost> {
        val onlyPosts = posts.map { it.post }
        val authorsById = localStorageService.getAuthors(
                onlyPosts.map { it.author }
                        .toSet()
        ).associateBy { it.id }

        val postTerms = localStorageService.getTermsForPost(
                onlyPosts.map { it.id }
        )

        val featuredMediaById = localStorageService.getFeaturedMediaByIdsNow(
                onlyPosts.map { it.featuredMedia }
                        .distinct()
        ).associateBy { it.id }

        return posts.map {
            DisplayablePost(it.post.id, it.post.date, it.post.slug, it.post.link, it.post.title,
                    it.postContent?.content ?: PostField(),
                    it.post.excerpt, authorsById[it.post.author],
                    postTerms[it.post.id]?.filter { it.taxonomy == TermTaxonomyEntity.category }
                            ?: emptyList(),
                    postTerms[it.post.id]?.filter { it.taxonomy == TermTaxonomy.post_tag }
                            ?: emptyList(),
                    featuredMediaById[it.post.featuredMedia])
        }
    }

    fun markPostViewed(post: DisplayablePost) {
        preferences.updatePostViewed(post.id)
    }

    fun relatedPosts(post: DisplayablePost): LiveData<List<DisplayablePost>> {
        log.info("relatedPosts for post {}", post.link)

        return Transformations.map(
                localStorageService.getTopPosts(post.link.extractWebsiteUrl, post.tags, post.categories, post.author, preferences.getViewedPosts().keys)
        ) { posts ->
            val featuredMediaById = localStorageService.getFeaturedMediaByIdsNow(posts.map { it.featuredMedia }).associateBy { it.id }
            posts.map { DisplayablePost(it.id, it.date, it.slug, it.link, it.title, PostField(), it.excerpt, null, emptyList(), emptyList(), featuredMediaById[it.featuredMedia]) }
        }
    }

    fun fetchRelatedPosts(post: DisplayablePost) {
        loadPosts(
                post.link.extractWebsiteUrl, page = 1,
                authors = post.author?.id?.let { id -> listOf(id) } ?: emptyList(),
                categories = post.categories.map { it.id },
                tags = post.tags.map { it.id }
        )
    }

    fun getPosts(params: PostsSearchParams): LiveData<List<DisplayablePost>> {
        log.info("getPosts for params {}", params)

        loadPosts(
                websiteUrl = params.websiteUrl,
                categories = params.categories.toList(),
                tags = params.tags.toList(),
                authors = params.authors.toList(),
                page = 1
        )

        return Transformations.map(
                localStorageService.getPosts(params)
        ) { posts ->
            posts.map { DisplayablePost(it.id, it.date, it.slug, it.link, it.title, PostField(), it.excerpt, null, emptyList(), emptyList(), null) }
        }
    }

    fun toggleBookmark(post: DisplayablePost, bookmarked: Boolean) {
        clientStorage?.toggleBookmark(post, bookmarked)
    }

    fun isBookmarked(url: String): LiveData<Boolean> {
        log.info("isBookmarked for url $url clientStorage is not null ${clientStorage != null}")
        return clientStorage?.isBookmarked(url) ?: MutableLiveData(false)
    }


    private val inProgressRequests: MutableSet<String> = HashSet()
    private val recentlySucceededRequests: MutableMap<String, Long> = HashMap()

    fun loadPosts(websiteUrl: String, page: Int,
                  tags: List<String> = emptyList(),
                  authors: List<String> = emptyList(),
                  categories: List<String> = emptyList()) {
        val key = "$websiteUrl$page$tags$authors$categories"

        val currentTimeMillis = System.currentTimeMillis()
        if (inProgressRequests.contains(key)
                || recentlySucceededRequests[key]?.minus(currentTimeMillis) ?: 5000 < 5000) {
            return
        }
        inProgressRequests.add(key)

        webClient.webService(websiteUrl)
                .getPosts(page = page, tags = tags, categories = categories, authors = authors, after = preferences.getLastFetchedPublishedDate())
                .subscribe(object : ObservableObserver<ApiResponse<List<Post>>> {
                    override fun onSubscribe(d: ApiResponse<List<Post>>) {
                        when (d) {
                            is ApiResponse.Success<List<Post>> ->
                                localStorageService.savePosts(d.data)
                            is ApiResponse.Error -> {
                                d.error
                                inProgressRequests.remove(key)
                            }
                        }
                    }

                    override fun onComplete() {
                        inProgressRequests.remove(key)
                        recentlySucceededRequests[key] = currentTimeMillis
                    }

                    override fun onError(e: ApiError) {
                        inProgressRequests.remove(key)
                    }
                })
    }


    private fun loadPost(postUrl: String) {
        val key = "loadPost$postUrl"

        val currentTimeMillis = System.currentTimeMillis()
        if (inProgressRequests.contains(key)
                || recentlySucceededRequests[key]?.minus(currentTimeMillis) ?: 5000 < 5000) {
            return
        }

        postUrl.extractPostSlug?.let { postSlug ->
            webClient.webService(postUrl.extractWebsiteUrl)
                    .getPost(postSlug)
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
                                    inProgressRequests.remove(key)
                                }
                            }
                        }

                        override fun onComplete() {
                            inProgressRequests.remove(key)
                            recentlySucceededRequests[key] = currentTimeMillis
                        }

                        override fun onError(e: ApiError) {
                            inProgressRequests.remove(key)
                        }
                    })
        }
    }

    fun getTermById(id: String): TermEntity? = localStorageService.getTermsByIds(listOf(id))
            .firstOrNull()
}
