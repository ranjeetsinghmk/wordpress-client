package com.sikhsiyasat.wordpress

import android.content.Context
import androidx.room.Room
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.sikhsiyasat.wordpress.models.WordpressDatabase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSession


object AppScope {
    private var repository: WordpressRepository? = null

    fun repository(context: Context): WordpressRepository {

        val repository = this.repository ?: WordpressRepository(
            webClient = webClient,
            localStorageService = localStorageService(context)
        )

        this.repository = repository

        return repository
    }

    private val webClient: WebClient
        get() = WebClient(retrofitServiceFactory)

    private val retrofitServiceFactory: RetrofitServiceFactory
        get() = RetrofitServiceFactory()

    private fun localStorageService(context: Context): LocalStorageService =
        LocalStorageService(database(context).postDao())

    private fun database(context: Context): WordpressDatabase = Room.databaseBuilder(
        context,
        WordpressDatabase::class.java, "wordpress"
    )
//        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .fallbackToDestructiveMigration() //Different DB version required
        .allowMainThreadQueries() //Removing this may cause crashes
        .build()

}


class WebClient(private val retrofitServiceFactory: RetrofitServiceFactory) {
    fun webService(websiteUrl: String): WebserviceInternal {
        return retrofitServiceFactory.provideRetrofit(websiteUrl)
            .create(WebserviceInternal::class.java)
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
//            .addCallAdapterFactory(adapterFactory)
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

object ListPostsScope {

}

object PostScope {

}