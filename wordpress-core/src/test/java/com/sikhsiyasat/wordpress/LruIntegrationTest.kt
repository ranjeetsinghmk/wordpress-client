package com.sikhsiyasat.wordpress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sikhsiyasat.wordpress.api.Post
import com.sikhsiyasat.wordpress.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.*

/**
 * Integration test for LRU functionality with WordpressRepository
 */
class LruIntegrationTest {

    @Mock
    private lateinit var postDao: PostDao

    @Mock
    private lateinit var authorDao: AuthorDao

    @Mock
    private lateinit var termDao: TermDao

    @Mock
    private lateinit var featuredMediaDao: FeaturedMediaDao

    @Mock
    private lateinit var webClient: com.sikhsiyasat.wordpress.api.WebClient

    @Mock
    private lateinit var clientStorage: ClientStorage

    private lateinit var localStorageService: LocalStorageService
    private lateinit var repository: WordpressRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        localStorageService = LocalStorageService(postDao, authorDao, termDao, featuredMediaDao)
        repository = WordpressRepository(webClient, localStorageService, clientStorage)
    }

    @Test
    fun `repository should mark posts as accessed when retrieved`() {
        // Given
        val postUrl = "https://example.com/post-1"
        val testPost = createTestPostEntity("1", "Test Post")
        val liveData = MutableLiveData<PostEntity?>(testPost)
        `when`(postDao.loadByLink(postUrl)).thenReturn(liveData)
        `when`(authorDao.load(any())).thenReturn(MutableLiveData(emptyList()))
        `when`(termDao.load(any())).thenReturn(MutableLiveData(emptyList()))
        `when`(featuredMediaDao.load(any())).thenReturn(MutableLiveData(emptyList()))

        // When
        val result = repository.getPost(postUrl)

        // Then - verify the post is marked as accessed
        verify(postDao).updateLastAccessTime(eq("1"), any(Date::class.java))
    }

    @Test
    fun `repository should provide convenient LRU management methods`() {
        // Given
        val expectedDeletions = 50
        val expectedAgeDeletions = 20
        val expectedLruDeletions = 30
        val expectedPostCount = 750

        `when`(postDao.getPostCount()).thenReturn(1200, expectedPostCount)
        `when`(postDao.deleteLeastRecentlyUsedPosts(any())).thenReturn(expectedDeletions)
        `when`(postDao.deletePostsOlderThan(any())).thenReturn(expectedAgeDeletions)

        // When & Then
        val cleanupResult = repository.performLruCleanupIfNeeded()
        assertEquals(expectedDeletions, cleanupResult)

        val oldPostsResult = repository.deleteOldPosts()
        assertEquals(expectedAgeDeletions, oldPostsResult)

        val postCount = repository.getCachedPostCount()
        assertEquals(expectedPostCount, postCount)

        `when`(postDao.deleteLeastRecentlyUsedPosts(any())).thenReturn(expectedLruDeletions)
        val comprehensiveResult = repository.performComprehensiveCleanup()
        assertEquals(Pair(expectedAgeDeletions, expectedLruDeletions), comprehensiveResult)
    }

    @Test
    fun `LRU cleanup scenario - managing cache size`() {
        // Scenario: App has been running for a while and cache is getting large
        // We want to clean up old and least recently used posts

        // Given: Cache has exceeded the limit
        val currentPostCount = 1500 // Exceeds default limit of 1000
        val ageBasedDeletions = 50 // Posts older than 30 days
        val lruBasedDeletions = 450 // Additional LRU cleanup needed
        
        `when`(postDao.deletePostsOlderThan(any())).thenReturn(ageBasedDeletions)
        `when`(postDao.getPostCount()).thenReturn(currentPostCount)
        `when`(postDao.deleteLeastRecentlyUsedPosts(any())).thenReturn(lruBasedDeletions)

        // When: Perform comprehensive cleanup
        val (actualAgeDeletions, actualLruDeletions) = repository.performComprehensiveCleanup()

        // Then: Both age-based and LRU-based cleanup should occur
        assertEquals(ageBasedDeletions, actualAgeDeletions)
        assertEquals(lruBasedDeletions, actualLruDeletions)
        
        // Verify the correct DAO methods were called
        verify(postDao).deletePostsOlderThan(any())
        verify(postDao).getPostCount()
        verify(postDao).deleteLeastRecentlyUsedPosts(any())
    }

    @Test
    fun `LRU access tracking scenario - frequently accessed posts stay longer`() {
        // Scenario: User frequently reads certain posts, these should be kept longer

        // Given: User accesses a post multiple times
        val postUrl = "https://example.com/frequently-read-post"
        val postId = "frequently-read-1"
        val testPost = createTestPostEntity(postId, "Frequently Read Post")
        val liveData = MutableLiveData<PostEntity?>(testPost)
        
        `when`(postDao.loadByLink(postUrl)).thenReturn(liveData)
        `when`(authorDao.load(any())).thenReturn(MutableLiveData(emptyList()))
        `when`(termDao.load(any())).thenReturn(MutableLiveData(emptyList()))
        `when`(featuredMediaDao.load(any())).thenReturn(MutableLiveData(emptyList()))

        // When: User accesses the post multiple times
        repository.getPost(postUrl) // First access
        repository.getPost(postUrl) // Second access
        repository.getPost(postUrl) // Third access

        // Then: Post should be marked as accessed each time
        verify(postDao, times(3)).updateLastAccessTime(eq(postId), any(Date::class.java))
    }

    @Test
    fun `constants should be accessible through service`() {
        // Test that default values are reasonable for a mobile app
        assertEquals(1000, LocalStorageService.DEFAULT_MAX_POSTS)
        assertEquals(100, LocalStorageService.DEFAULT_LRU_CLEANUP_COUNT)
        assertEquals(30, LocalStorageService.DEFAULT_MAX_AGE_DAYS)
        
        // These defaults mean:
        // - Keep maximum 1000 posts in cache (reasonable for mobile storage)
        // - When cleanup is needed, delete 100 posts at a time (batch efficiency)
        // - Delete posts older than 30 days (reasonable retention period)
    }

    private fun createTestPostEntity(id: String, title: String): PostEntity {
        return PostEntity(
            id = id,
            date = Date(),
            slug = "post-slug-$id",
            link = "https://example.com/post-$id",
            title = com.sikhsiyasat.wordpress.api.PostField(title, false),
            content = com.sikhsiyasat.wordpress.api.PostField("Content for $title", false),
            excerpt = com.sikhsiyasat.wordpress.api.PostField("Excerpt for $title", false),
            author = 1,
            categories = listOf(1, 2),
            tags = listOf(3, 4),
            featuredMedia = "media-$id",
            lastAccessTime = Date()
        )
    }
}