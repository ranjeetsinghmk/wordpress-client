package com.sikhsiyasat.wordpress.ui.detail

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.sikhsiyasat.webview.MyWebClient
import com.sikhsiyasat.webview.VideoEnabledWebChromeClient
import com.sikhsiyasat.wordpress.AppScope
import com.sikhsiyasat.wordpress.R
import kotlinx.android.synthetic.main.post_fragment.*


class PostFragment : Fragment() {

    companion object {
        const val KEY_POST_URL = "post_url"

        fun newInstance(url: String): PostFragment {
            return PostFragment()
                .apply {
                    arguments = Bundle()
                        .apply {
                            putString(KEY_POST_URL, url)
                        }
                }
        }
    }


    private val viewModel: PostViewModel by viewModels(
        factoryProducer = {
            PostViewModelFactory(
                AppScope.repository(context!!),
                this,
                defaultArgs = arguments
            )
        }
    )


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
                toolbar.setTitle(R.string.app_name)
                it.setSupportActionBar(toolbar)
                it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }

        val webChromeClient = VideoEnabledWebChromeClient(webView = webview)
        val webClient = MyWebClient(object : MyWebClient.ErrorListener {
            override fun onErrorStateChange(visible: Boolean) {

            }
        })

        val webSettings: WebSettings = webview.settings
        webSettings.domStorageEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webview.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webview.isScrollbarFadingEnabled = false
        webview.setWebChromeClient(webChromeClient)
        webview.webViewClient = webClient

        viewModel.post
            .observe(viewLifecycleOwner) { post ->
                post?.asHtml()?.let { spannedHtml ->
                    webview.loadDataWithBaseURL(
                        baseUrl = null,
                        data = spannedHtml,
                        mimeType = "text/html; charset=utf-8",
                        encoding = "UTF-8",
                        historyUrl = null
                    )
                }
                toolbar.title = post?.title?.spannedText
            }
    }
}
