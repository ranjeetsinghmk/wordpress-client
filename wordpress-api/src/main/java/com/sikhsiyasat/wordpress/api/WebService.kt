package com.sikhsiyasat.wordpress.api

/**
 * WordPress REST API Service interface.
 * Provides methods to interact with WordPress REST API v2 endpoints.
 * 
 * Reference: https://developer.wordpress.org/rest-api/
 */
interface WebService {
    /**
     * Get a single post by its slug.
     * @param slug The post slug (URL-friendly name)
     * @return Observable with the post data
     */
    fun getPost(slug: String): Observable<ApiResponse<Post>>

    /**
     * Get a paginated list of posts.
     * @param page Page number (1-indexed)
     * @param perPage Number of posts per page (default: 10, max: 100)
     * @param context Scope under which the request is made (embed, view, or edit)
     * @return Observable with the list of posts
     */
    fun getPosts(
        page: Int = 1,
        perPage: Int = 10,
        context: String = "embed"
    ): Observable<ApiResponse<List<Post>>>
    
    /**
     * Get posts filtered by category.
     * @param categoryId The category ID to filter by
     * @param page Page number (1-indexed)
     * @param perPage Number of posts per page
     * @return Observable with the list of posts in the category
     */
    fun getPostsByCategory(
        categoryId: Int,
        page: Int = 1,
        perPage: Int = 10
    ): Observable<ApiResponse<List<Post>>>
    
    /**
     * Get posts filtered by tag.
     * @param tagId The tag ID to filter by
     * @param page Page number (1-indexed)
     * @param perPage Number of posts per page
     * @return Observable with the list of posts with the tag
     */
    fun getPostsByTag(
        tagId: Int,
        page: Int = 1,
        perPage: Int = 10
    ): Observable<ApiResponse<List<Post>>>
    
    /**
     * Search for posts by search term.
     * @param searchTerm The search query
     * @param page Page number (1-indexed)
     * @param perPage Number of results per page
     * @return Observable with the list of matching posts
     */
    fun searchPosts(
        searchTerm: String,
        page: Int = 1,
        perPage: Int = 10
    ): Observable<ApiResponse<List<Post>>>
    
    /**
     * Get all categories.
     * @param page Page number (1-indexed)
     * @param perPage Number of categories per page (default: 100)
     * @return Observable with the list of categories
     */
    fun getCategories(
        page: Int = 1,
        perPage: Int = 100
    ): Observable<ApiResponse<List<Category>>>
    
    /**
     * Get all tags.
     * @param page Page number (1-indexed)
     * @param perPage Number of tags per page (default: 100)
     * @return Observable with the list of tags
     */
    fun getTags(
        page: Int = 1,
        perPage: Int = 100
    ): Observable<ApiResponse<List<Tag>>>
    
    /**
     * Perform a search across posts.
     * @param searchTerm The search query
     * @param page Page number (1-indexed)
     * @param perPage Number of results per page
     * @return Observable with the search results
     */
    fun search(
        searchTerm: String,
        page: Int = 1,
        perPage: Int = 10
    ): Observable<ApiResponse<List<SearchResult>>>
}

/**
 * Simple Observable implementation for async API calls.
 * Provides a lightweight reactive pattern for handling API responses.
 * 
 * @param T The type of data being observed
 */
class Observable<T> {
    private var limit = Int.MAX_VALUE
    private val observers: MutableList<ObservableObserver<T>> = ArrayList()
    private lateinit var onSubscribe: ObservableOnSubscribe<T>

    /**
     * Subscribe to this Observable to receive data, completion, and error events.
     * 
     * @param observer The observer to receive events
     */
    fun subscribe(
        observer: ObservableObserver<T>
    ) {
        if (observers.size > limit) {
            throw RuntimeException("Limit exceeded")
        }

        observers.add(observer)

        onSubscribe.subscribe(object : ObservableEmitter<T> {
            override fun onComplete() {
                observers.forEach { it.onComplete() }
            }

            override fun onNext(t: T) {
                observers.forEach { it.onSubscribe(t) }
            }

            override fun onError(error: ApiError) {
                observers.forEach { it.onError(error) }
            }
        })
    }

    companion object {
        /**
         * Create a new Observable with the specified subscription logic.
         * 
         * @param subscribe The subscription handler that emits data
         * @return A new Observable instance
         */
        fun <T> create(subscribe: ObservableOnSubscribe<T>): Observable<T> {
            val observable = Observable<T>()
            observable.onSubscribe = subscribe
            return observable
        }
    }
}

/**
 * Simple observer interface for data changes.
 */
interface Observer<T> {
    fun onChanged(t: T)
}

/**
 * Observer interface for Observable subscriptions.
 * Receives data, completion, and error events.
 */
interface ObservableObserver<T> {
    /**
     * Called when new data is available.
     */
    fun onSubscribe(d: T)

    /**
     * Called when the Observable has finished emitting data.
     */
    fun onComplete()

    /**
     * Called when an error occurs.
     */
    fun onError(e: ApiError)
}

/**
 * Emitter interface for Observable data sources.
 * Used to emit data, completion, and error events to observers.
 */
interface ObservableEmitter<T> {
    fun onComplete()
    fun onNext(t: T)
    fun onError(error: ApiError)
}

/**
 * Subscription handler interface for Observable creation.
 */
interface ObservableOnSubscribe<T> {
    fun subscribe(emitter: ObservableEmitter<T>)
}
