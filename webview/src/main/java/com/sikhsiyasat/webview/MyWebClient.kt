package com.sikhsiyasat.webview

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import com.sikhsiyasat.logger.SSLogger
import java.util.*

/**
 * Created by brinder.singh on 04/05/17.
 */
class MyWebClient : WebViewClient {
    private val webViewUri: Uri?
    private val pageProgressBar: ProgressBar?
    private val webViewListener: WebViewListener?
    private var isError = false
    private var loadingFinished = false
    private var redirect = false
    private var currentLoadingUrl: String? = null
    private val urlsToHandle: MutableSet<String> = HashSet()
    private val urlsToOpenInDetails: MutableSet<String> = HashSet()
    private val urlsToOpenOutside: MutableSet<String> = HashSet()
    private val log = SSLogger.getLogger("MyWebClient")
    private var allowAll = false

    constructor(url: String?,
                pageProgressBar: ProgressBar?,
                webViewListener: WebViewListener?) {
        webViewUri = Uri.parse(url)
        this.pageProgressBar = pageProgressBar
        this.webViewListener = webViewListener
    }

    constructor(webViewListener: WebViewListener?) {
        webViewUri = null
        pageProgressBar = null
        this.webViewListener = webViewListener
    }

    fun addUrlToHandle(handleUrl: String): MyWebClient {
        urlsToHandle.add(handleUrl)
        return this
    }

    fun addUrlToHandleOutside(handleUrl: String): MyWebClient {
        urlsToOpenOutside.add(handleUrl)
        return this
    }

    fun addUrlToOpenDetails(handleUrl: String): MyWebClient {
        urlsToOpenInDetails.add(handleUrl)
        return this
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!loadingFinished) {
            redirect = true
        }
        loadingFinished = false
        return handleUri(view, request.url)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (!loadingFinished) {
            redirect = true
        }
        loadingFinished = false
        val uri = Uri.parse(url)
        return view?.let { handleUri(it, uri) } ?: false
    }

    private fun handleUri(view: WebView, uri: Uri): Boolean {
        log.info("handleUri uri: $uri")
        return when {
            urlIsADetailPage(uri) -> {
                webViewListener?.onDetailsLinkArrived(uri)
                true
            }
            allowWebViewToHandleUrl(uri, webViewUri) -> {
                false
            }
            else -> {
                log.info("WebViewOpenURI $uri")
                view.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            }
        }
    }

    private fun allowWebViewToHandleUrl(uri: Uri, webViewUri: Uri?): Boolean {
        return !isOpenOutsideUrl(uri.toString())
                && (
                allowAll
                        || null != webViewUri && null != uri.host && uri.host == webViewUri.host
                        || canHandleAdditionalUrl(uri.toString())
                )
    }

    private fun isOpenOutsideUrl(urlString: String): Boolean {
        for (urlToHandle in urlsToOpenOutside) {
            if (urlString.replace("https", "http")
                            .toLowerCase(Locale.getDefault()).startsWith(urlToHandle.replace("https", "http")
                                    .toLowerCase(Locale.getDefault()))) {
                return true
            }
        }
        return false
    }

    private fun isOpenDetailsUrl(urlString: String): Boolean {
        for (urlToHandle in urlsToOpenInDetails) {
            if (urlString.replace("https", "http").toLowerCase(Locale.getDefault())
                            .startsWith(urlToHandle.replace("https", "http").toLowerCase(Locale.getDefault()))) {
                return true
            }
        }
        return false
    }

    private fun urlIsADetailPage(uri: Uri) = uri.queryParameterNames.contains("submit").not()
            && webViewUri.toString().replace("https", "http") != uri.toString().replace("https", "http")
            && isOpenDetailsUrl(uri.toString())

    private fun canHandleAdditionalUrl(url: String): Boolean {
        for (urlToHandle in urlsToHandle) {
            if (url.replace("https", "http").toLowerCase(Locale.getDefault())
                            .startsWith(urlToHandle.replace("https", "http").toLowerCase(Locale.getDefault()))) {
                return true
            }
        }
        return false
    }


    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        log.info("onPageStarted $url")
        loadingFinished = false
        if (!isError) {
            showError(false)
        }
        isError = false
        webViewListener?.onLoading(false)

        pageProgressBar?.progress = 0
        pageProgressBar?.visibility = View.VISIBLE

        currentLoadingUrl = url
    }

    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
        super.onReceivedHttpError(view, request, errorResponse)
        log.info("onReceivedHttpError $errorResponse")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        log.info("onPageFinished $loadingFinished redirect $redirect")
        if (!redirect) {
            loadingFinished = true
        }
        if (loadingFinished && !redirect) {
            showError(false)
        } else {
            redirect = false
        }
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        isError = true
        log.info("onReceivedError " + request.toString() + " error: " + error?.toString())
        val errorCode: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            errorCode = error?.errorCode ?: 0
            view?.let { onInvalidState(it, errorCode) }
        } else {
            showError(true)
        }
    }

    private fun showError(visible: Boolean) {
        webViewListener?.onErrorStateChange(visible)
    }

    private fun onInvalidState(view: WebView, errorCode: Int): Boolean {
        log.info("onInvalidState")
        if (errorCode == ERROR_TIMEOUT || errorCode == ERROR_CONNECT || errorCode == ERROR_UNKNOWN) {
            view.stopLoading()
            showError(true)
            return true
        }
        return false
    }

    override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
        if (!onInvalidState(view, errorCode)) {
            super.onReceivedError(view, errorCode, description, failingUrl)
        }
    }

    fun getCurrentLoadingUrl(): String? {
        return if (null == currentLoadingUrl && null != webViewUri) {
            webViewUri.path
        } else currentLoadingUrl
    }

    fun allowAllUrls(): WebViewClient {
        allowAll = true
        return this
    }

    interface WebViewListener {
        fun onErrorStateChange(visible: Boolean)
        fun onDetailsLinkArrived(uri: Uri)
        fun onLoading(refreshing: Boolean)
    }
}