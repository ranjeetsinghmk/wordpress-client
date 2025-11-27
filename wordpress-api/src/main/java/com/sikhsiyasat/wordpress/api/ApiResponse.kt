package com.sikhsiyasat.wordpress.api

/**
 * Sealed class representing API responses.
 * Used to wrap successful data or error information from WordPress REST API calls.
 * 
 * @param T The type of data expected on success
 */
sealed class ApiResponse<out T : Any> {
    /**
     * Represents a successful API response containing data.
     * @param data The parsed response data
     */
    data class Success<out T : Any>(val data: T) : ApiResponse<T>()
    
    /**
     * Represents an error response from the API.
     * @param error The error details
     */
    data class Error(val error: ApiError) : ApiResponse<Nothing>()
}

/**
 * Represents an error from the WordPress REST API.
 * WordPress REST API returns errors in a specific format:
 * {
 *   "code": "rest_post_invalid_page_number",
 *   "message": "The page number requested is larger than the number of pages available.",
 *   "data": { "status": 400 }
 * }
 * 
 * @param message Human-readable error message
 * @param code Optional error code from WordPress (e.g., "rest_invalid_param")
 * @param status Optional HTTP status code
 */
data class ApiError(
    override val message: String,
    val code: String? = null,
    val status: Int? = null
) : RuntimeException(message)