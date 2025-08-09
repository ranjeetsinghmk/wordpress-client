package com.sikhsiyasat.wordpress

import com.sikhsiyasat.wordpress.api.PostField
import com.sikhsiyasat.wordpress.models.PostEntity
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Simple verification test for LRU PostEntity changes
 */
class LruEntityVerificationTest {

    @Test
    fun `PostEntity should have lastAccessTime field with default value`() {
        // Given
        val postEntity = PostEntity(
            id = "test-1",
            date = Date(),
            slug = "test-slug",
            link = "https://example.com/test",
            title = PostField("Test Title"),
            content = PostField("Test Content"),
            excerpt = PostField("Test Excerpt"),
            author = 1,
            categories = listOf(1, 2),
            tags = listOf(3, 4),
            featuredMedia = "media-1"
            // lastAccessTime should use default value (current time)
        )

        // Then
        assertNotNull("lastAccessTime should not be null", postEntity.lastAccessTime)
        
        // Should be within a reasonable range of current time (within 1 second)
        val timeDiff = Math.abs(System.currentTimeMillis() - postEntity.lastAccessTime.time)
        assertTrue("lastAccessTime should be close to current time", timeDiff < 1000)
    }

    @Test
    fun `PostEntity should allow custom lastAccessTime`() {
        // Given
        val customTime = Date(System.currentTimeMillis() - 60000) // 1 minute ago
        val postEntity = PostEntity(
            id = "test-2",
            date = Date(),
            slug = "test-slug-2",
            link = "https://example.com/test-2",
            title = PostField("Test Title 2"),
            content = PostField("Test Content 2"),
            excerpt = PostField("Test Excerpt 2"),
            author = 2,
            categories = listOf(1),
            tags = listOf(3),
            featuredMedia = "media-2",
            lastAccessTime = customTime
        )

        // Then
        assertEquals("lastAccessTime should match custom value", customTime, postEntity.lastAccessTime)
    }

    @Test
    fun `PostField should work correctly with LRU tests`() {
        // Verify that PostField can be created with our test parameters
        val titleField = PostField("Test Title", false)
        val contentField = PostField("Test Content", true)
        val excerptField = PostField() // Default constructor

        assertEquals("Test Title", titleField.rendered)
        assertFalse(titleField.protected)

        assertEquals("Test Content", contentField.rendered)
        assertTrue(contentField.protected)

        assertEquals("", excerptField.rendered)
        assertFalse(excerptField.protected)
    }

    @Test
    fun `LocalStorageService constants should be reasonable for mobile app`() {
        // Test that the constants make sense for a mobile WordPress client
        
        // 1000 posts is reasonable for mobile storage (assuming ~1KB metadata per post = ~1MB)
        assertTrue("Max posts should be reasonable for mobile", 
            LocalStorageService.DEFAULT_MAX_POSTS in 500..2000)
        
        // 100 posts cleanup is a good batch size (not too small, not too large)
        assertTrue("Cleanup count should be efficient batch size", 
            LocalStorageService.DEFAULT_LRU_CLEANUP_COUNT in 50..200)
        
        // 30 days is reasonable retention period for news/blog content
        assertTrue("Max age should be reasonable retention period", 
            LocalStorageService.DEFAULT_MAX_AGE_DAYS in 7..90)
    }
}