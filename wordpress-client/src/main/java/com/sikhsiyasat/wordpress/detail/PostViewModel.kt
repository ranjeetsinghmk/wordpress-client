package com.sikhsiyasat.wordpress.detail

import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.sikhsiyasat.wordpress.WordpressRepository
import com.sikhsiyasat.wordpress.detail.PostFragment.Companion.KEY_POST_URL
import com.sikhsiyasat.wordpress.models.Post

class PostViewModel constructor(
    savedStateHandle: SavedStateHandle,
    wordpressRepository: WordpressRepository
) : ViewModel() {
    private val url: String =
        savedStateHandle[KEY_POST_URL] ?: throw IllegalAccessException("post url is missing")
    val post: LiveData<Post> = Transformations.map(
        wordpressRepository.getPost(url)
    ) { post ->
        post ?: kotlin.run {
            //TODO send state info
            Post.dummyPost()
        }
    }
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
        return PostViewModel(handle, repository) as T
    }
}
