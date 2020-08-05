package com.sikhsiyasat.wordpress.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.sikhsiyasat.logger.SSLogger
import com.sikhsiyasat.wordpress.AppScope
import com.sikhsiyasat.wordpress.R
import com.sikhsiyasat.wordpress.models.DisplayablePost
import com.sikhsiyasat.wordpress.models.TermEntity
import com.sikhsiyasat.wordpress.ui.PostsInteractionListener
import com.sikhsiyasat.wordpress.ui.PostsViewAdapter
import kotlinx.android.synthetic.main.fragment_posts_list.*

class PostsFragment : Fragment() {

    private lateinit var interactionListener: InteractionListener
    val log = SSLogger.getLogger("PostsFragment")

    private val viewModel: PostListViewModel by viewModels(
            factoryProducer = {
                PostsListViewModelFactory(
                        AppScope.repository(requireContext()),
                        this,
                        defaultArgs = arguments
                )
            }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.info("onCreate")
        setHasOptionsMenu(false)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_posts_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        log.info("onViewCreated")

        swipe_refresh_widget.setOnRefreshListener {
            viewModel.refresh()
        }

        (activity as AppCompatActivity)
                .let {
                    toolbar.setTitle(R.string.app_name)
                    it.setSupportActionBar(toolbar)
                }

        updateFilterView()

        viewModel.posts.observe(viewLifecycleOwner, Observer { posts ->
            if (posts.isNotEmpty()) {
                swipe_refresh_widget.isRefreshing = false
            }
            with(posts_list) {
                layoutManager = LinearLayoutManager(context)
                adapter =
                        PostsViewAdapter(posts,
                                object :
                                        PostsInteractionListener {
                                    override fun onSelected(post: DisplayablePost) {
                                        interactionListener.goToPostDetailPage(post)
                                    }
                                })
            }
        })

    }

    private fun updateFilterView() {
        if (viewModel.term != null) {
            page_filter_type.visibility = View.VISIBLE
            page_filter_value.visibility = View.VISIBLE

            page_filter_type.text = viewModel.term?.taxonomy?.name
            page_filter_value.text = viewModel.term?.name
        } else {
            page_filter_type.visibility = View.GONE
            page_filter_value.visibility = View.GONE
        }
    }

    companion object {
        const val ARG_WEBSITE_BASE_URL = "web-base-url"
        const val ARG_TERM_ID = "term-id"
        /*
        * posts by tag url example - ${websiteUrl}/tag/${tag}
        * posts by category url example - ${websiteUrl}/category/${tag}
        * */

        @JvmStatic
        fun newInstance(baseUrl: String, term: TermEntity?, interactionListener: InteractionListener) =
                PostsFragment()
                        .apply {
                            this.log.info("creating newInstance with baseUrl {} with term {}", baseUrl, term)

                            arguments = Bundle()
                                    .apply {
                                        putString(ARG_WEBSITE_BASE_URL, baseUrl)
                                        term?.let {
                                            putString(ARG_TERM_ID, it.id)
                                        }
                                    }
                            this.interactionListener = interactionListener
                        }
    }

    interface InteractionListener {
        fun goToPostDetailPage(post: DisplayablePost)
    }
}
