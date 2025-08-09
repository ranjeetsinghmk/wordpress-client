package com.sikhsiyasat.wordpress

import com.sikhsiyasat.wordpress.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.*

/**
 * Unit tests for LRU (Least Recently Used) post management functionality
 */
class LruPostManagementTest {

    @Mock
    private lateinit var postDao: PostDao

    @Mock
    private lateinit var authorDao: AuthorDao

    @Mock
    private lateinit var termDao: TermDao

    @Mock
    private lateinit var featuredMediaDao: FeaturedMediaDao

    private lateinit var localStorageService: LocalStorageService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        localStorageService = LocalStorageService(postDao, authorDao, termDao, featuredMediaDao)
    }

    @Test
    fun `markPostAsAccessed should update last access time`() {
        // Given
        val postId = "test-post-1"

        // When
        localStorageService.markPostAsAccessed(postId)

        // Then
        verify(postDao).updateLastAccessTime(eq(postId), any(Date::class.java))
    }

    @Test
    fun `getLeastRecentlyUsedPosts should return posts from dao with default limit`() {
        // Given
        val expectedPosts = createTestPosts(5)
        `when`(postDao.getLeastRecentlyUsedPosts(LocalStorageService.DEFAULT_LRU_CLEANUP_COUNT))
            .thenReturn(expectedPosts)

        // When
        val result = localStorageService.getLeastRecentlyUsedPosts()

        // Then
        assertEquals(expectedPosts, result)
        verify(postDao).getLeastRecentlyUsedPosts(LocalStorageService.DEFAULT_LRU_CLEANUP_COUNT)
    }

    @Test
    fun `getLeastRecentlyUsedPosts should return posts from dao with custom limit`() {
        // Given
        val customLimit = 50
        val expectedPosts = createTestPosts(customLimit)
        `when`(postDao.getLeastRecentlyUsedPosts(customLimit)).thenReturn(expectedPosts)

        // When
        val result = localStorageService.getLeastRecentlyUsedPosts(customLimit)

        // Then
        assertEquals(expectedPosts, result)
        verify(postDao).getLeastRecentlyUsedPosts(customLimit)
    }

    @Test
    fun `deleteLeastRecentlyUsedPosts should return number of deleted posts with default count`() {
        // Given
        val expectedDeletedCount = 50
        `when`(postDao.deleteLeastRecentlyUsedPosts(LocalStorageService.DEFAULT_LRU_CLEANUP_COUNT))
            .thenReturn(expectedDeletedCount)

        // When
        val result = localStorageService.deleteLeastRecentlyUsedPosts()

        // Then
        assertEquals(expectedDeletedCount, result)
        verify(postDao).deleteLeastRecentlyUsedPosts(LocalStorageService.DEFAULT_LRU_CLEANUP_COUNT)
    }

    @Test
    fun `deleteLeastRecentlyUsedPosts should return number of deleted posts with custom count`() {
        // Given
        val customCount = 25
        val expectedDeletedCount = 25
        `when`(postDao.deleteLeastRecentlyUsedPosts(customCount)).thenReturn(expectedDeletedCount)

        // When
        val result = localStorageService.deleteLeastRecentlyUsedPosts(customCount)

        // Then
        assertEquals(expectedDeletedCount, result)
        verify(postDao).deleteLeastRecentlyUsedPosts(customCount)
    }

    @Test
    fun `performLruCleanupIfNeeded should not delete posts when under limit`() {
        // Given
        val maxPosts = 1000
        val currentCount = 500
        `when`(postDao.getPostCount()).thenReturn(currentCount)

        // When
        val result = localStorageService.performLruCleanupIfNeeded(maxPosts)

        // Then
        assertEquals(0, result)
        verify(postDao, never()).deleteLeastRecentlyUsedPosts(any())
    }

    @Test
    fun `performLruCleanupIfNeeded should delete posts when over limit`() {
        // Given
        val maxPosts = 1000
        val currentCount = 1200
        val expectedDeletions = currentCount - maxPosts + LocalStorageService.DEFAULT_LRU_CLEANUP_COUNT
        `when`(postDao.getPostCount()).thenReturn(currentCount)
        `when`(postDao.deleteLeastRecentlyUsedPosts(expectedDeletions)).thenReturn(expectedDeletions)

        // When
        val result = localStorageService.performLruCleanupIfNeeded(maxPosts)

        // Then
        assertEquals(expectedDeletions, result)
        verify(postDao).deleteLeastRecentlyUsedPosts(expectedDeletions)
    }

    @Test
    fun `deletePostsOlderThan should delete posts older than specified days`() {
        // Given
        val maxAgeDays = 30
        val expectedDeletedCount = 15
        `when`(postDao.deletePostsOlderThan(any(Date::class.java))).thenReturn(expectedDeletedCount)

        // When
        val result = localStorageService.deletePostsOlderThan(maxAgeDays)

        // Then
        assertEquals(expectedDeletedCount, result)
        verify(postDao).deletePostsOlderThan(any(Date::class.java))
    }

    @Test
    fun `getPostCount should return current post count from dao`() {
        // Given
        val expectedCount = 750
        `when`(postDao.getPostCount()).thenReturn(expectedCount)

        // When
        val result = localStorageService.getPostCount()

        // Then
        assertEquals(expectedCount, result)
        verify(postDao).getPostCount()
    }

    @Test
    fun `performComprehensiveCleanup should perform both age-based and LRU cleanup`() {
        // Given
        val maxPosts = 1000
        val maxAgeDays = 30
        val currentCount = 1200
        val ageBasedDeletions = 20
        val lruBasedDeletions = 100

        `when`(postDao.deletePostsOlderThan(any(Date::class.java))).thenReturn(ageBasedDeletions)
        `when`(postDao.getPostCount()).thenReturn(currentCount)
        `when`(postDao.deleteLeastRecentlyUsedPosts(any())).thenReturn(lruBasedDeletions)

        // When
        val result = localStorageService.performComprehensiveCleanup(maxPosts, maxAgeDays)

        // Then
        assertEquals(Pair(ageBasedDeletions, lruBasedDeletions), result)
        verify(postDao).deletePostsOlderThan(any(Date::class.java))
        verify(postDao).getPostCount()
        verify(postDao).deleteLeastRecentlyUsedPosts(any())
    }

    @Test
    fun `constants should have expected values`() {
        assertEquals(1000, LocalStorageService.DEFAULT_MAX_POSTS)
        assertEquals(100, LocalStorageService.DEFAULT_LRU_CLEANUP_COUNT)
        assertEquals(30, LocalStorageService.DEFAULT_MAX_AGE_DAYS)
    }

    private fun createTestPosts(count: Int): List<PostEntity> {
        return (1..count).map { index ->
            PostEntity(
                id = "post-$index",
                date = Date(),
                slug = "post-slug-$index",
                link = "https://example.com/post-$index",
                title = PostField("Post $index", "Post $index"),
                content = PostField("Content $index", "Content $index"),
                excerpt = PostField("Excerpt $index", "Excerpt $index"),
                author = index,
                categories = listOf(1, 2),
                tags = listOf(3, 4),
                featuredMedia = "media-$index",
                lastAccessTime = Date(System.currentTimeMillis() - (index * 1000L))
            )
        }
    }
}