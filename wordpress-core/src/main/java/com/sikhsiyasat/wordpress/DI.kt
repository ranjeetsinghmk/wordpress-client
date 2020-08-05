package com.sikhsiyasat.wordpress

import android.content.Context
import androidx.room.Room
import com.sikhsiyasat.wordpress.api.AppScope
import com.sikhsiyasat.wordpress.models.WordpressDatabase


object AppScope {
    private var repository: WordpressRepository? = null

    fun repository(context: Context): WordpressRepository {
        val repository = this.repository ?: WordpressRepository(
                webClient = AppScope.webClient,
                localStorageService = localStorageService(context),
                preferences = WordpressPreferences(context)
        )

        this.repository = repository

        return repository
    }

    private fun localStorageService(context: Context): LocalStorageService {
        return database(context).let { database ->
            LocalStorageService(
                    database.postDao(),
                    database.postContentDao(),
                    database.postTermDao(),
                    database.authorDao(),
                    database.termDao(),
                    database.featuredMediaDao()
            )
        }
    }

    private fun database(context: Context): WordpressDatabase = Room.databaseBuilder(
                    context,
                    WordpressDatabase::class.java, "wordpress"
            )
//        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration() //Different DB version required
            .allowMainThreadQueries() //Removing this may cause crashes
            .build()

}