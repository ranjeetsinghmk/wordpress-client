package com.sikhsiyasat.wordpress.api

sealed class ApiResponse<out T : Any> {
    data class Success<out T : Any>(val page: Int, val perPage: Int, val total: Int, val data: T) : ApiResponse<T>()
    data class Error(val error: ApiError) : ApiResponse<Nothing>()
}


data class ApiError(override val message: String) : RuntimeException(message)