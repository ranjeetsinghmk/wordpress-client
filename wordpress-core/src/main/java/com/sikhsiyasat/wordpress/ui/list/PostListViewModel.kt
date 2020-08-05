package com.sikhsiyasat.wordpress.ui.list

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.sikhsiyasat.wordpress.WordpressRepository
import com.sikhsiyasat.wordpress.models.DisplayablePost
import com.sikhsiyasat.wordpress.models.PostsSearchParams
import com.sikhsiyasat.wordpress.models.TermEntity
import com.sikhsiyasat.wordpress.models.TermTaxonomyEntity

class PostListViewModel(
        savedStateHandle: SavedStateHandle,
        private val wordpressRepository: WordpressRepository
) : ViewModel() {
    fun refresh() {
        wordpressRepository.loadPosts(websiteUrl, 20)
    }

    private val websiteUrl: String =
            savedStateHandle[PostsFragment.ARG_WEBSITE_BASE_URL]
                    ?: throw IllegalAccessException("post url is missing")
    private val termId: String? = savedStateHandle[PostsFragment.ARG_TERM_ID]
    val term: TermEntity? = termId?.let { wordpressRepository.getTermById(it) }

    val posts: LiveData<List<DisplayablePost>> = wordpressRepository
            .getPosts(PostsSearchParams(
                    websiteUrl,
                    categories = if (term?.taxonomy == TermTaxonomyEntity.category) setOf(term.id) else emptySet(),
                    tags = if (term?.taxonomy == TermTaxonomyEntity.post_tag) setOf(term.id) else emptySet()
            ))
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
        return PostListViewModel(
                handle,
                repository
        ) as T
    }
}
