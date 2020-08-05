package com.sikhsiyasat.wordpress.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSession


object AppScope {
    val webClient: WebClient
        get() = WebClient(retrofitServiceFactory)

    private val retrofitServiceFactory: RetrofitServiceFactory
        get() = RetrofitServiceFactory()
}


class WebClient(private val retrofitServiceFactory: RetrofitServiceFactory) {
    fun webService(websiteUrl: String): WebService {
        return WebServiceImpl(
                retrofitServiceFactory.provideRetrofit(websiteUrl)
                        .create(WebserviceInternal::class.java)
        )
    }
}


class RetrofitServiceFactory {
    private val retrofits: MutableMap<String, Retrofit> = HashMap()

    fun provideRetrofit(websiteUrl: String): Retrofit {
        val retrofit = retrofits[websiteUrl] ?: retrofit(websiteUrl)
        retrofits[websiteUrl] = retrofit
        return retrofit
    }

    private fun retrofit(websiteUrl: String): Retrofit = Retrofit.Builder()
            .baseUrl("$websiteUrl/wp-json/wp/v2/")
//        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
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