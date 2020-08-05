package com.sikhsiyasat.wordpress

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sikhsiyasat.logger.SSLogger
import java.util.*

class WordpressPreferences internal constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val logger = SSLogger.getLogger("WordpressPreferences")

    private fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    private fun putLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    private fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun putString(key: String, value: String?) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getString(key: String, defValue: String?): String? {
        return sharedPreferences.getString(key, defValue)
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defValue)
    }

    fun getLong(key: String?, defValue: Long): Long {
        return sharedPreferences.getLong(key, defValue)
    }


    fun updatePostsFetched() {
        putLong(LAST_POSTS_FETCH_PUBLISHED_DATE, Date().time)
    }

    fun getLastFetchedPublishedDate(): Date = Date(getLong(LAST_POSTS_FETCH_PUBLISHED_DATE, 0))

    fun updatePostViewed(postId: String) {
        putString(POSTS_VIEWED, getViewedPosts().plus(Pair(postId, Date())).let { Gson().toJson(it) })
    }

    fun getViewedPosts(): Map<String, Date> {
        val type = object : TypeToken<Map<String, Date>>() {}.type
        return getString(POSTS_VIEWED, "{}").let { Gson().fromJson(it, type) as Map<String, Date> }
    }

    companion object {
        private const val LAST_POSTS_FETCH_PUBLISHED_DATE = "last_fetched_published_date"
        private const val POSTS_VIEWED = "posts_viewed_history"
    }
}