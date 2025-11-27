package com.sikhsiyasat.wordpress.api

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WordPress REST API integration.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}

/**
 * Unit tests for API Response classes.
 */
class ApiResponseTest {
    
    @Test
    fun `Success response contains data`() {
        val data = "test data"
        val response = ApiResponse.Success(data)
        
        assertEquals("test data", response.data)
    }
    
    @Test
    fun `Error response contains error message`() {
        val error = ApiError("Test error message")
        val response = ApiResponse.Error(error)
        
        assertEquals("Test error message", response.error.message)
    }
    
    @Test
    fun `ApiError with code and status`() {
        val error = ApiError(
            message = "Invalid parameter",
            code = "rest_invalid_param",
            status = 400
        )
        
        assertEquals("Invalid parameter", error.message)
        assertEquals("rest_invalid_param", error.code)
        assertEquals(400, error.status)
    }
}

/**
 * Unit tests for Category model.
 */
class CategoryModelTest {
    
    @Test
    fun `Category has required fields`() {
        val category = Category(
            id = 1,
            count = 10,
            description = "Test category description",
            link = "https://example.com/category/test",
            name = "Test Category",
            slug = "test-category",
            taxonomy = "category",
            parent = 0
        )
        
        assertEquals(1, category.id)
        assertEquals(10, category.count)
        assertEquals("Test category description", category.description)
        assertEquals("https://example.com/category/test", category.link)
        assertEquals("Test Category", category.name)
        assertEquals("test-category", category.slug)
        assertEquals("category", category.taxonomy)
        assertEquals(0, category.parent)
    }
}

/**
 * Unit tests for Tag model.
 */
class TagModelTest {
    
    @Test
    fun `Tag has required fields`() {
        val tag = Tag(
            id = 5,
            count = 25,
            description = "Test tag description",
            link = "https://example.com/tag/test",
            name = "Test Tag",
            slug = "test-tag",
            taxonomy = "post_tag"
        )
        
        assertEquals(5, tag.id)
        assertEquals(25, tag.count)
        assertEquals("Test tag description", tag.description)
        assertEquals("https://example.com/tag/test", tag.link)
        assertEquals("Test Tag", tag.name)
        assertEquals("test-tag", tag.slug)
        assertEquals("post_tag", tag.taxonomy)
    }
}

/**
 * Unit tests for SearchResult model.
 */
class SearchResultModelTest {
    
    @Test
    fun `SearchResult has required fields`() {
        val result = SearchResult(
            id = 123,
            title = "Test Post Title",
            url = "https://example.com/test-post",
            type = "post",
            subtype = "post"
        )
        
        assertEquals(123, result.id)
        assertEquals("Test Post Title", result.title)
        assertEquals("https://example.com/test-post", result.url)
        assertEquals("post", result.type)
        assertEquals("post", result.subtype)
    }
}

/**
 * Unit tests for Observable pattern.
 */
class ObservableTest {
    
    @Test
    fun `Observable emits data to subscriber`() {
        var receivedData: String? = null
        
        val observable = Observable.create(object : ObservableOnSubscribe<String> {
            override fun subscribe(emitter: ObservableEmitter<String>) {
                emitter.onNext("test data")
                emitter.onComplete()
            }
        })
        
        observable.subscribe(object : ObservableObserver<String> {
            override fun onSubscribe(d: String) {
                receivedData = d
            }
            
            override fun onComplete() {}
            
            override fun onError(e: ApiError) {}
        })
        
        assertEquals("test data", receivedData)
    }
    
    @Test
    fun `Observable calls onComplete`() {
        var completed = false
        
        val observable = Observable.create(object : ObservableOnSubscribe<String> {
            override fun subscribe(emitter: ObservableEmitter<String>) {
                emitter.onComplete()
            }
        })
        
        observable.subscribe(object : ObservableObserver<String> {
            override fun onSubscribe(d: String) {}
            
            override fun onComplete() {
                completed = true
            }
            
            override fun onError(e: ApiError) {}
        })
        
        assertTrue(completed)
    }
    
    @Test
    fun `Observable calls onError`() {
        var errorReceived: ApiError? = null
        
        val observable = Observable.create(object : ObservableOnSubscribe<String> {
            override fun subscribe(emitter: ObservableEmitter<String>) {
                emitter.onError(ApiError("Test error"))
            }
        })
        
        observable.subscribe(object : ObservableObserver<String> {
            override fun onSubscribe(d: String) {}
            
            override fun onComplete() {}
            
            override fun onError(e: ApiError) {
                errorReceived = e
            }
        })
        
        assertNotNull(errorReceived)
        assertEquals("Test error", errorReceived?.message)
    }
}
