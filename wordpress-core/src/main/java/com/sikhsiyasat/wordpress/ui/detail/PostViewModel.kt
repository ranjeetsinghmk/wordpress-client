package com.sikhsiyasat.wordpress.ui.detail

import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.sikhsiyasat.wordpress.WordpressRepository
import com.sikhsiyasat.wordpress.models.DisplayablePost
import com.sikhsiyasat.wordpress.models.PostTheme
import com.sikhsiyasat.wordpress.ui.detail.PostFragment.Companion.KEY_ALLOW_TEXT_SPEECH
import com.sikhsiyasat.wordpress.ui.detail.PostFragment.Companion.KEY_POST_URL

class PostViewModel constructor(
        savedStateHandle: SavedStateHandle,
        private val wordpressRepository: WordpressRepository
) : ViewModel() {
    private var postThemeMV: MutableLiveData<PostTheme> = MutableLiveData(PostTheme.Normal)
    val postTextToSpeechPlaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val relatedPosts = MutableLiveData<List<DisplayablePost>>(emptyList())
    private var textToSpeechPlaying = false
    var displayToastLD: MutableLiveData<Int> = MutableLiveData(0)

    fun toggleBookmark(bookmarked: Boolean) {
        post.value?.let { displayablePost ->
            wordpressRepository.toggleBookmark(bookmarked = bookmarked, post = displayablePost)
        }
    }

    fun changeTextSize() {
        val next = (postThemeMV.value ?: PostTheme.Small).next()
        postThemeMV.postValue(next)
        displayToastLD.postValue(next.nameId)
    }

    fun toggleTextToSpeech() {
        val textToSpeech = postTextToSpeechPlaying.value?.not() ?: false
        postTextToSpeechPlaying.postValue(textToSpeech)
    }

    fun postTextToSpeechInitiated() {
        textToSpeechPlaying = true
        postTextToSpeechPlaying.postValue(false)
    }

    fun markPostViewed() {
        post.value?.let { wordpressRepository.markPostViewed(it) }
    }

    private val url: String =
            savedStateHandle[KEY_POST_URL] ?: throw IllegalAccessException("post url is missing")
    val isTextToSpeechEnabled: Boolean =
            savedStateHandle[KEY_ALLOW_TEXT_SPEECH] ?: true
    val post: LiveData<DisplayablePost?> = Transformations.switchMap(postThemeMV) { theme ->
        Transformations.map(wordpressRepository.getPost(url)) {
            it?.theme = theme
            it
        }
    }
    val bookmarked: LiveData<Boolean> = wordpressRepository.isBookmarked(url)

    private fun fetchRelatedPosts() {
        post.value?.let { displayablePost ->
            wordpressRepository.relatedPosts(displayablePost)
                    .observeForever {
                        relatedPosts.postValue(it)
                    }
            wordpressRepository.fetchRelatedPosts(displayablePost)
        }
    }

    fun mainViewScrolledY(oldScrollY: Int, scrollY: Int, fullScrollY: Int) {
        if (oldScrollY != scrollY && scrollY / fullScrollY.toFloat() > 0.8 && relatedPosts.value?.isEmpty() != false) {
            fetchRelatedPosts()
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
        return PostViewModel(
                handle,
                repository
        ) as T
    }
}
