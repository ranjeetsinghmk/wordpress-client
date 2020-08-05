package com.sikhsiyasat.wordpress.ui.detail

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.*
import android.webkit.WebSettings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.sikhsiyasat.logger.SSLogger
import com.sikhsiyasat.webview.MyWebClient
import com.sikhsiyasat.webview.VideoEnabledWebChromeClient
import com.sikhsiyasat.wordpress.AppScope
import com.sikhsiyasat.wordpress.R
import com.sikhsiyasat.wordpress.models.DisplayablePost
import com.sikhsiyasat.wordpress.models.TermEntity
import com.sikhsiyasat.wordpress.ui.PostsInteractionListener
import com.sikhsiyasat.wordpress.ui.PostsViewAdapter
import com.sikhsiyasat.wordpress.ui.TermsAdapter
import kotlinx.android.synthetic.main.post_fragment.*
import java.util.*


class PostFragment : Fragment() {
    companion object {
        const val KEY_POST_URL = "post_url"
        const val KEY_ALLOW_TEXT_SPEECH = "allow_tts"

        @JvmStatic
        fun newInstance(url: String, allowTextToSpeech: Boolean = true): PostFragment {
            return PostFragment()
                    .apply {
                        arguments = Bundle()
                                .apply {
                                    putString(KEY_POST_URL, url)
                                    putBoolean(KEY_ALLOW_TEXT_SPEECH, allowTextToSpeech)
                                }
                    }
        }
    }


    private lateinit var mListener: PostInteractionListener
    private var tts: TextToSpeech? = null
    private val viewModel: PostViewModel by viewModels(
            factoryProducer = {
                PostViewModelFactory(
                        AppScope.repository(requireContext()),
                        this,
                        defaultArgs = arguments
                )
            }
    )
    private val log: SSLogger = SSLogger.getLogger("PostFragment")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (viewModel.isTextToSpeechEnabled) {
            tts = TextToSpeech(requireContext(), onInitListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_news_details, menu)
        super.onCreateOptionsMenu(menu, inflater)

        log.info("onCreateOptionsMenu returning")

        toolbar.menu
                .findItem(R.id.action_text_to_speech)
                ?.let {
                    it.isVisible = viewModel.isTextToSpeechEnabled
                }

        viewModel.bookmarked
                .observe(viewLifecycleOwner) { isBookmarked ->
                    log.info("viewModel.bookmarked $isBookmarked, menu available ${toolbar.menu != null}, item available ${toolbar.menu
                            .findItem(R.id.action_toggle_bookmark) != null}")
                    toolbar.menu
                            .findItem(R.id.action_toggle_bookmark)
                            ?.setIcon(
                                    if (isBookmarked)
                                        R.drawable.ic_bookmark_white_24dp
                                    else
                                        R.drawable.ic_bookmark_border_white_24dp
                            )
                            ?.apply {
                                this.isChecked = isBookmarked
                            }
                }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                viewModel.post.value?.let { post -> mListener.onShareRequested(post) }
                true
            }
            R.id.action_toggle_bookmark -> {
                viewModel.toggleBookmark(bookmarked = item.isChecked.not())
                true
            }
            R.id.action_text_size_change -> {
                viewModel.changeTextSize()
                true
            }
            R.id.action_text_to_speech -> {
                viewModel.toggleTextToSpeech()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.post_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity)
                .let {
                    it.setSupportActionBar(toolbar)
                    it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
                    it.title = null
                }

        setupWebView()
        post_nested_scroll_view.setOnScrollChangeListener { v: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
            v?.let { scrollView ->
                val fullScrollY = scrollView.children.map { childHeight -> childHeight.measuredHeight }.sum() - scrollView.measuredHeight
                viewModel.mainViewScrolledY(oldScrollY, scrollY, fullScrollY)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        startObservingVM()
    }

    private fun startObservingVM() {
        viewModel.post
                .observe(viewLifecycleOwner) { displayablePost ->
                    loadPostContent(displayablePost)
                    displayablePost?.let { post ->
                        log.info("viewModel.post observed ${post.title.spannedText} ${post.content.spannedText.isNotEmpty()} ${post.tags.map { it.name }} ${post.categories.map { it.name }}")

                        first_divider.visibility = if (post.tags.isEmpty() && post.categories.isEmpty()) View.GONE else View.VISIBLE
                        displayTags(post.tags)
                        displayCategories(post.categories)
                    }
                }

        viewModel.displayToastLD
                .observe(viewLifecycleOwner, Observer { toastStringResId ->
                    if (toastStringResId != 0) {
                        Toast.makeText(requireContext(), toastStringResId, Toast.LENGTH_SHORT).show()
                    }
                })

        viewModel.postTextToSpeechPlaying
                .observe(viewLifecycleOwner, Observer { enabled ->
                    playTextToSpeech(enabled)

                    handleTextToSpeechMenuVisibility(enabled)
                })

        viewModel.relatedPosts
                .observe(viewLifecycleOwner, Observer { posts ->
                    related_posts_no_post.visibility = if (posts.isNotEmpty()) View.GONE else View.VISIBLE
                    related_posts_progress_bar.visibility = View.GONE

                    with(related_posts) {
                        visibility = if (posts.isEmpty()) View.GONE else View.VISIBLE
                        layoutManager = LinearLayoutManager(context)
                        adapter =
                                PostsViewAdapter(posts,
                                        object :
                                                PostsInteractionListener {
                                            override fun onSelected(post: DisplayablePost) {
                                                mListener.onPostClicked(post)
                                            }
                                        })
                    }
                })
    }

    private val itemClickListener = object : TermsAdapter.ItemClickListener {
        override fun onClicked(termEntity: TermEntity) {
            mListener.onTagClicked(termEntity)
        }
    }

    private fun displayTags(tags: List<TermEntity>) {
        post_tags_heading.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE
        post_tags_list.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE
        post_tags_list.layoutManager = GridLayoutManager(requireContext(), 3)
        post_tags_list.adapter = TermsAdapter(requireContext(), tags, itemClickListener)
    }

    private fun displayCategories(categories: List<TermEntity>) {
        post_categories_heading.visibility = if (categories.isEmpty()) View.GONE else View.VISIBLE
        post_categories_list.visibility = if (categories.isEmpty()) View.GONE else View.VISIBLE
        post_categories_list.layoutManager = GridLayoutManager(requireContext(), 2, LinearLayoutManager.HORIZONTAL, false)
        post_categories_list.setHasFixedSize(true)
        post_categories_list.adapter = TermsAdapter(requireContext(), categories, itemClickListener)
    }

    private fun loadPostContent(post: DisplayablePost?) {
        post?.asHtml()?.let { spannedHtml ->
            progress_bar.visibility = View.VISIBLE
            webview.loadDataWithBaseURL(
                    baseUrl = null,
                    data = spannedHtml,
                    mimeType = "text/html; charset=utf-8",
                    encoding = "UTF-8",
                    historyUrl = null
            )
        }
    }

    private fun handleTextToSpeechMenuVisibility(enabled: Boolean) {
        toolbar.menu
                .findItem(R.id.action_text_to_speech)
                ?.apply {
                    setIcon(
                            if (enabled)
                                R.drawable.ic_volume_up_black_24dp
                            else
                                R.drawable.ic_volume_off_black_24dp
                    )
                    isVisible = viewModel.isTextToSpeechEnabled
                    isEnabled = viewModel.isTextToSpeechEnabled
                }
    }

    private fun playTextToSpeech(enabled: Boolean) {
        if (viewModel.isTextToSpeechEnabled.not()) {
            return
        }
        if (enabled) {
            tts?.language = Locale.US
            viewModel.post.value?.let { post ->
                val textToSpeech = "${post.title.spannedText}.\n" +
                        " ${post.author?.name?.let { "published by $it" }} on ${post.date}.\n" +
                        " ${post.content.spannedText}"
                tts?.speak(textToSpeech, TextToSpeech.QUEUE_ADD, null)
            }
        } else {
            tts?.stop()
        }
    }

    private val onInitListener = TextToSpeech.OnInitListener {
        log.info("onInitListener $it")
        viewModel.postTextToSpeechInitiated()
    }

    private val chromeClientCallback = object : VideoEnabledWebChromeClient.Callback {
        override fun toggledFullscreen(fullscreen: Boolean) {
            //Note: not yet handling the toggle screen
        }

        override fun onAlmostLoaded() {
            if (view != null && viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                progress_bar.visibility = View.GONE
                post_container.visibility = View.VISIBLE
            }
        }

        override fun onContentAvailable(url: String?) {
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                //Note: handled somewhere else
                post_container.visibility = View.VISIBLE
                viewModel.markPostViewed()
            }
        }
    }

    private val webViewListener = object : MyWebClient.WebViewListener {
        override fun onErrorStateChange(visible: Boolean) {
            //Note: TODO not yet handled
        }

        override fun onDetailsLinkArrived(uri: Uri) {
            //Note: nested post view will come here
        }

        override fun onLoading(refreshing: Boolean) {
            //Note: TODO complete it
        }
    }

    private fun setupWebView() {
        webview.isScrollContainer = false
        val webChromeClient = VideoEnabledWebChromeClient(
                webView = webview,
                pageProgressBar = progress_bar,
                activityVideoView = videoLayout
        )
        webChromeClient.setCallback(chromeClientCallback)
        webview.setWebChromeClient(webChromeClient)

        val webSettings: WebSettings = webview.settings
        webSettings.domStorageEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

//        webview.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
//        webview.isScrollbarFadingEnabled = false
        webview.isNestedScrollingEnabled = false

        val webClient = MyWebClient(webViewListener)
        webview.webViewClient = webClient
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PostInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement PostInteractionListener")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
    }

    interface PostInteractionListener {
        fun onShareRequested(post: DisplayablePost)
        fun onPostClicked(post: DisplayablePost)
        fun onTagClicked(termEntity: TermEntity)
    }
}
