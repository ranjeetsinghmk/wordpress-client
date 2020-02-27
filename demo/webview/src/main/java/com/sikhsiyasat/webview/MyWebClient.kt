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
import com.sikhsiyasat.logger.SSLogger.Companion.getLogger
import java.util.*

/**
 * Created by brinder.singh on 04/05/17.
 */
class MyWebClient : WebViewClient {
    private val webViewUri: Uri?
    private val pageProgressBar: ProgressBar?
    private val errorListener: ErrorListener?
    private var isError = false
    private var loadingFinished = false
    private var redirect = false
    private var currentLoadingUrl: String? = null
    private val urlsToHandle: MutableSet<String> =
        HashSet()
    private val logger = getLogger("MyWebClient")
    private var allowAll = false

    constructor(
        url: String?,
        pageProgressBar: ProgressBar?,
        errorListener: ErrorListener?
    ) {
        webViewUri = Uri.parse(url)
        this.pageProgressBar = pageProgressBar
        this.errorListener = errorListener
    }

    constructor(errorListener: ErrorListener?) {
        webViewUri = null
        pageProgressBar = null
        this.errorListener = errorListener
    }

    fun addUrlToHandle(handleUrl: String): MyWebClient {
        urlsToHandle.add(handleUrl)
        return this
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        if (!loadingFinished) {
            redirect = true
        }
        loadingFinished = false
        return handleUri(view, request.url)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        url: String
    ): Boolean {
        if (!loadingFinished) {
            redirect = true
        }
        loadingFinished = false
        val uri = Uri.parse(url)
        return handleUri(view, uri)
    }

    private fun handleUri(view: WebView, uri: Uri): Boolean {
        logger.info("handleUri uri: $uri")
        val host = uri.host
        return if (!uri.toString().startsWith("com.sikhsiyasat") && (allowAll
                    || null != webViewUri && host != null && host == webViewUri.host || canHandleAdditionalUrl(
                uri.toString()
            ))
        ) {
            false
        } else {
            logger.info("WebViewOpenURI $uri")
            view.context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
            )
            true
        }
    }

    private fun canHandleAdditionalUrl(url: String): Boolean {
        for (urlToHandle in urlsToHandle) {
            if (url.toLowerCase().startsWith(urlToHandle.toLowerCase())) {
                return true
            }
        }
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        logger.info("onPageStarted $url")
        loadingFinished = false
        if (!isError) {
            showError(false)
        }
        isError = false
        if (null != pageProgressBar) {
            pageProgressBar.progress = 0
            pageProgressBar.visibility = View.VISIBLE
        }
        currentLoadingUrl = url
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        logger.info("onReceivedHttpError $errorResponse")
    }

    override fun onPageFinished(
        view: WebView?,
        url: String?
    ) {
        logger.info("onPageFinished $loadingFinished redirect $redirect")
        if (!redirect) {
            loadingFinished = true
        }
        if (loadingFinished && !redirect) {
            showError(false)
        } else {
            redirect = false
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError
    ) {
        isError = true
        logger.info("onReceivedError " + request.toString() + " error: " + error.toString())
        val errorCode: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            errorCode = error.errorCode
            onInvalidState(view, errorCode)
        } else {
            showError(true)
        }
    }

    private fun showError(visible: Boolean) {
        errorListener?.onErrorStateChange(visible)
    }

    private fun onInvalidState(view: WebView?, errorCode: Int?): Boolean {
        logger.info("onInvalidState")
        if (errorCode == ERROR_TIMEOUT || errorCode == ERROR_CONNECT || errorCode == ERROR_UNKNOWN) {
            view?.stopLoading()
            showError(true)
            return true
        }
        return false
    }

    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
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

    interface ErrorListener {
        fun onErrorStateChange(visible: Boolean)
    }
}