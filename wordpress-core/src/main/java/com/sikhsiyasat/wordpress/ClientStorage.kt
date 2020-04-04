package com.sikhsiyasat.wordpress

import androidx.lifecycle.LiveData
import com.sikhsiyasat.wordpress.models.DisplayablePost

interface ClientStorage {
    fun toggleBookmark(post: DisplayablePost, bookmarked: Boolean)
    fun isBookmarked(url: String): LiveData<Boolean>
}