package com.sikhsiyasat.wordpress.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSession

/**
 * Application-level dependency injection scope for WordPress API.
 * Provides singleton instances of web client components.
 */
object AppScope {
    /**
     * Provides a WebClient instance for creating WordPress API services.
     */
    val webClient: WebClient
        get() = WebClient(retrofitServiceFactory)

    private val retrofitServiceFactory: RetrofitServiceFactory
        get() = RetrofitServiceFactory()
}

/**
 * Client for accessing WordPress REST API services.
 * Creates WebService instances configured for specific WordPress site URLs.
 * 
 * @param retrofitServiceFactory Factory for creating Retrofit instances
 */
class WebClient(private val retrofitServiceFactory: RetrofitServiceFactory) {
    /**
     * Creates a WebService instance for the specified WordPress site.
     * 
     * @param websiteUrl Base URL of the WordPress site (e.g., "https://example.com")
     * @return WebService instance configured for the WordPress REST API
     */
    fun webService(websiteUrl: String): WebService {
        return WebServiceImpl(
            retrofitServiceFactory.provideRetrofit(websiteUrl)
                .create(WebserviceInternal::class.java)
        )
    }
}

/**
 * Factory for creating and caching Retrofit instances.
 * Ensures a single Retrofit instance per WordPress site URL.
 */
class RetrofitServiceFactory {
    private val retrofits: MutableMap<String, Retrofit> = HashMap()

    /**
     * Provides a Retrofit instance for the specified WordPress site.
     * Caches instances to avoid recreating them for the same URL.
     * 
     * @param websiteUrl Base URL of the WordPress site
     * @return Configured Retrofit instance
     */
    fun provideRetrofit(websiteUrl: String): Retrofit {
        val retrofit = retrofits[websiteUrl] ?: retrofit(websiteUrl)
        retrofits[websiteUrl] = retrofit
        return retrofit
    }

    /**
     * Creates a new Retrofit instance configured for WordPress REST API v2.
     * 
     * Configuration:
     * - Base URL: {websiteUrl}/wp-json/wp/v2/
     * - JSON field naming: lower_case_with_underscores (WordPress convention)
     * - Date format: ISO 8601 (yyyy-MM-dd'T'hh:mm:ss)
     * - Connection timeout: 120 seconds
     * - HTTP logging enabled for debugging
     */
    private fun retrofit(websiteUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl("$websiteUrl/wp-json/wp/v2/")
        .client(okHttpClient)
        .addConverterFactory(
            GsonConverterFactory.create(
                GsonBuilder()
                    .enableComplexMapKeySerialization()
                    .serializeNulls()
                    .setDateFormat("yyyy-MM-dd'T'hh:mm:ss")
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .setPrettyPrinting()
                    .setVersion(1.0)
                    .create()
            )
        )
        .build()


    private val okHttpClient: OkHttpClient
        get() =
            OkHttpClient.Builder()
                .hostnameVerifier { _: String?, _: SSLSession? -> true }
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .connectTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor()
                    .apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                .build()
}

/**
 * Scope for post listing operations.
 * Reserved for future use.
 */
object ListPostsScope {

}