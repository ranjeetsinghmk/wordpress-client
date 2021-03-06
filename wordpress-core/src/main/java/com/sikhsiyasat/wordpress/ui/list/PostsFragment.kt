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
import com.sikhsiyasat.wordpress.AppScope
import com.sikhsiyasat.wordpress.R
import com.sikhsiyasat.wordpress.models.DisplayablePost
import com.sikhsiyasat.wordpress.ui.PostsInteractionListener
import com.sikhsiyasat.wordpress.ui.PostsRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_posts_list.*

class PostsFragment : Fragment() {

    private lateinit var interactionListener: InteractionListener

    private val viewModel: PostListViewModel by viewModels(
        factoryProducer = {
            PostsListViewModelFactory(
                AppScope.repository(requireContext()),
                this,
                defaultArgs = arguments
            )
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_posts_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh_widget.setOnRefreshListener {
            viewModel.refresh()
        }

        (activity as AppCompatActivity)
            .let {
                toolbar.setTitle(R.string.app_name)
                it.setSupportActionBar(toolbar)
            }

        viewModel.posts.observe(viewLifecycleOwner, Observer { posts ->
            if (posts.isNotEmpty()) {
                swipe_refresh_widget.isRefreshing = false
            }
            with(posts_list) {
                layoutManager = LinearLayoutManager(context)
                adapter =
                    PostsRecyclerViewAdapter(posts,
                        object :
                            PostsInteractionListener {
                            override fun onSelected(post: DisplayablePost) {
                                interactionListener.goToPostDetailPage(post)
                            }
                        })
            }
        })

    }

    companion object {
        const val ARG_WEBSITE_BASE_URL = "web-base-url"

        @JvmStatic
        fun newInstance(baseUrl: String, interactionListener: InteractionListener) =
            PostsFragment()
                .apply {
                    arguments = Bundle()
                        .apply {
                            putString(ARG_WEBSITE_BASE_URL, baseUrl)
                        }
                    this.interactionListener = interactionListener
                }
    }

    interface InteractionListener {
        fun goToPostDetailPage(post: DisplayablePost)
    }
}
