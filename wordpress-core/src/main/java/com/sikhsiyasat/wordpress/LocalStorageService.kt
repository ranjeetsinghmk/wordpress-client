package com.sikhsiyasat.wordpress

import androidx.lifecycle.LiveData
import com.sikhsiyasat.wordpress.api.Post
import com.sikhsiyasat.wordpress.models.*
import java.util.*

class LocalStorageService constructor(
    private val postDao: PostDao,
    private val authorDao: AuthorDao,
    private val termDao: TermDao,
    private val featuredMediaDao: FeaturedMediaDao
) {

    companion object {
        const val DEFAULT_MAX_POSTS = 1000
        const val DEFAULT_LRU_CLEANUP_COUNT = 100
        const val DEFAULT_MAX_AGE_DAYS = 30
    }

    //Posts
    fun getPost(postUrl: String): LiveData<PostEntity?> {
        return postDao.loadByLink(postUrl)
    }

    fun savePosts(posts: List<Post>) {
        postDao.save(
            posts
                .mapNotNull { PostMapper.postEntity(it) }
                .distinctBy { it.id }
                .toSet()
        )
        authorDao.save(
            posts.mapNotNull { it.embeddedData }
                .flatMap { it.author }
                .map { PostMapper.authorEntity(it) }
                .distinctBy { it.id }
                .toSet()
        )
        termDao.save(
            posts.mapNotNull { it.embeddedData }
                .flatMap { it.terms }
                .flatten()
                .map { PostMapper.termEntity(it) }
                .distinctBy { it.id }
                .toSet()
        )
        featuredMediaDao.save(
            posts.mapNotNull { it.embeddedData }
                .map { it.featuredMedia }
                .flatten()
                .map { PostMapper.featuredMediaEntity(it) }
                .distinctBy { it.id }
                .toSet()
        )
    }

    fun getPosts(websiteUrl: String): LiveData<List<PostEntity>> =
        postDao.loadWhereLinkLike("$websiteUrl%")

    fun getAuthors(ids: List<String>): LiveData<List<AuthorEntity>> = authorDao.load(ids)

    fun getCategories(ids: List<String>): LiveData<List<TermEntity>> = termDao.load(ids)

    fun getTags(ids: List<String>): LiveData<List<TermEntity>> = termDao.load(ids)

    fun getFeaturedMedia(ids: List<String>) = featuredMediaDao.load(ids)

    // LRU Management Methods

    /**
     * Mark a post as accessed by updating its last access time
     */
    fun markPostAsAccessed(postId: String) {
        postDao.updateLastAccessTime(postId, Date())
    }

    /**
     * Get posts ordered by least recently used
     */
    fun getLeastRecentlyUsedPosts(limit: Int = DEFAULT_LRU_CLEANUP_COUNT): List<PostEntity> {
        return postDao.getLeastRecentlyUsedPosts(limit)
    }

    /**
     * Delete least recently used posts to manage cache size
     * @param count Number of posts to delete (default: DEFAULT_LRU_CLEANUP_COUNT)
     * @return Number of posts deleted
     */
    fun deleteLeastRecentlyUsedPosts(count: Int = DEFAULT_LRU_CLEANUP_COUNT): Int {
        return postDao.deleteLeastRecentlyUsedPosts(count)
    }

    /**
     * Perform LRU cleanup if post count exceeds maximum
     * @param maxPosts Maximum number of posts to keep (default: DEFAULT_MAX_POSTS)
     * @return Number of posts deleted
     */
    fun performLruCleanupIfNeeded(maxPosts: Int = DEFAULT_MAX_POSTS): Int {
        val currentCount = postDao.getPostCount()
        return if (currentCount > maxPosts) {
            val postsToDelete = currentCount - maxPosts + DEFAULT_LRU_CLEANUP_COUNT
            deleteLeastRecentlyUsedPosts(postsToDelete)
        } else {
            0
        }
    }

    /**
     * Delete posts older than specified number of days
     * @param maxAgeDays Maximum age in days (default: DEFAULT_MAX_AGE_DAYS)
     * @return Number of posts deleted
     */
    fun deletePostsOlderThan(maxAgeDays: Int = DEFAULT_MAX_AGE_DAYS): Int {
        val cutoffTime = Date(System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L))
        return postDao.deletePostsOlderThan(cutoffTime)
    }

    /**
     * Get current post count
     */
    fun getPostCount(): Int {
        return postDao.getPostCount()
    }

    /**
     * Comprehensive cleanup that combines age-based and LRU-based cleanup
     * @param maxPosts Maximum number of posts to keep
     * @param maxAgeDays Maximum age in days
     * @return Pair of (age-based deletions, LRU-based deletions)
     */
    fun performComprehensiveCleanup(
        maxPosts: Int = DEFAULT_MAX_POSTS,
        maxAgeDays: Int = DEFAULT_MAX_AGE_DAYS
    ): Pair<Int, Int> {
        val ageBasedDeletions = deletePostsOlderThan(maxAgeDays)
        val lruBasedDeletions = performLruCleanupIfNeeded(maxPosts)
        return Pair(ageBasedDeletions, lruBasedDeletions)
    }
}
