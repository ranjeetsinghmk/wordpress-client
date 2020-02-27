package com.sikhsiyasat.wordpress.list

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.sikhsiyasat.wordpress.WordpressRepository
import com.sikhsiyasat.wordpress.models.Post

class PostListViewModel(
    savedStateHandle: SavedStateHandle,
    wordpressRepository: WordpressRepository
) : ViewModel() {
    private val websiteUrl: String =
        savedStateHandle[PostsFragment.ARG_WEBSITE_BASE_URL]
            ?: throw IllegalAccessException("post url is missing")
    val posts: LiveData<List<Post>> = wordpressRepository.getPosts(websiteUrl)
}


class PostsListViewModelFactory(
    private val repository: WordpressRepository,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return PostListViewModel(handle, repository) as T
    }
}
