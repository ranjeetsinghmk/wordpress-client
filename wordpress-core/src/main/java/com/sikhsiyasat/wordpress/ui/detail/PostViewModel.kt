package com.sikhsiyasat.wordpress.ui.detail

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.sikhsiyasat.wordpress.WordpressRepository
import com.sikhsiyasat.wordpress.models.DisplayablePost
import com.sikhsiyasat.wordpress.ui.detail.PostFragment.Companion.KEY_POST_URL

class PostViewModel constructor(
    savedStateHandle: SavedStateHandle,
    wordpressRepository: WordpressRepository
) : ViewModel() {
    private val url: String =
        savedStateHandle[KEY_POST_URL] ?: throw IllegalAccessException("post url is missing")
    val post: LiveData<DisplayablePost?> = wordpressRepository.getPost(url)
}

class PostViewModelFactory(
    private val repository: WordpressRepository,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return PostViewModel(
            handle,
            repository
        ) as T
    }
}
