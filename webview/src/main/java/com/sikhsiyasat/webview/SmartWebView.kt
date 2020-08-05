package com.sikhsiyasat.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.view.MotionEventCompat
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat

/**
 * This class serves as a WebView to be used in conjunction with a VideoEnabledWebChromeClient.
 * It makes possible:
 * - To detect the HTML5 video ended event so that the VideoEnabledWebChromeClient can exit full-app_logo.
 *
 *
 * Important notes:
 * - Javascript is enabled by default and must not be disabled with getSettings().setJavaScriptEnabled(false).
 * - setWebChromeClient() must be called before any loadData(), loadDataWithBaseURL() or loadUrl() method.
 *
 * @author Cristian Perez (http://cpr.name)
 */
/*
 * TODO https://firebase.google.com/docs/analytics/android/webview
 * */
class SmartWebView : WebView, NestedScrollingChild {
    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)
    private var mLastY = 0
    private var mNestedOffsetY = 0
    private var firstScroll = true
    private var mChildHelper: NestedScrollingChildHelper? = null
    private var videoEnabledWebChromeClient: VideoEnabledWebChromeClient? = null
    private var addedJavascriptInterface: Boolean

    constructor(context: Context?) : super(context) {
        addedJavascriptInterface = false
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
            context,
            attrs
    ) {
        addedJavascriptInterface = false
        mChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true
    }

    constructor(
            context: Context?,
            attrs: AttributeSet?,
            defStyle: Int
    ) : super(context, attrs, defStyle) {
        addedJavascriptInterface = false
        mChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true
    }

    /**
     * Indicates if the video is being displayed using a custom view (typically full-app_logo)
     *
     * @return true it the video is being displayed using a custom view (typically full-app_logo)
     */
    val isVideoFullscreen: Boolean
        get() = videoEnabledWebChromeClient != null && videoEnabledWebChromeClient?.isVideoFullscreen ?: false

    /**
     * Pass only a VideoEnabledWebChromeClient instance.
     */
    @SuppressLint("SetJavaScriptEnabled")
    override fun setWebChromeClient(client: WebChromeClient) {
        settings.javaScriptEnabled = true
        if (client is VideoEnabledWebChromeClient) {
            videoEnabledWebChromeClient = client
        }
        super.setWebChromeClient(client)
    }

    override fun loadData(
            data: String,
            mimeType: String?,
            encoding: String?
    ) {
        addJavascriptInterface()
        super.loadData(data, mimeType, encoding)
    }

    override fun loadDataWithBaseURL(
            baseUrl: String?,
            data: String,
            mimeType: String?,
            encoding: String?,
            historyUrl: String?
    ) {
        addJavascriptInterface()
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    override fun loadUrl(url: String) {
        addJavascriptInterface()
        super.loadUrl(url)
    }

    override fun loadUrl(
            url: String,
            additionalHttpHeaders: Map<String, String>
    ) {
        addJavascriptInterface()
        super.loadUrl(url, additionalHttpHeaders)
    }

    private fun addJavascriptInterface() {
        if (!addedJavascriptInterface) { // Add javascript interface to be called when the video ends (must be done before page load)
            addJavascriptInterface(
                    JavascriptInterface(),
                    "_VideoEnabledWebView"
            ) // Must match Javascript interface name of VideoEnabledWebChromeClient
            addedJavascriptInterface = true
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val returnValue: Boolean
        val event = MotionEvent.obtain(ev)
        val action = MotionEventCompat.getActionMasked(event)
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsetY = 0
        }
        val eventY = event.y.toInt()
        event.offsetLocation(0f, mNestedOffsetY.toFloat())
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                var deltaY = mLastY - eventY
                // NestedPreScroll
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1]
                    mLastY = eventY - mScrollOffset[1]
                    event.offsetLocation(0f, (-mScrollOffset[1]).toFloat())
                    mNestedOffsetY += mScrollOffset[1]
                }
                returnValue = super.onTouchEvent(event)
                // NestedScroll
                if (dispatchNestedScroll(0, mScrollOffset[1], 0, deltaY, mScrollOffset)) {
                    event.offsetLocation(0f, mScrollOffset[1].toFloat())
                    mNestedOffsetY += mScrollOffset[1]
                    mLastY -= mScrollOffset[1]
                }
            }
            MotionEvent.ACTION_DOWN -> {
                returnValue = super.onTouchEvent(event)
                if (firstScroll) { // dispatching first down scrolling properly by making sure that first deltaY will be -ve
                    mLastY = eventY - 5
                    firstScroll = false
                } else {
                    mLastY = eventY
                }
                // start NestedScroll
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
            }
            else -> {
                returnValue = super.onTouchEvent(event)
                // end NestedScroll
                stopNestedScroll()
            }
        }
        return returnValue
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mChildHelper?.isNestedScrollingEnabled ?: false
    }

    // Nested Scroll implements
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mChildHelper?.isNestedScrollingEnabled = enabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mChildHelper?.startNestedScroll(axes) ?: false
    }

    override fun stopNestedScroll() {
        mChildHelper?.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mChildHelper?.hasNestedScrollingParent() ?: false
    }

    override fun dispatchNestedScroll(
            dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int,
            offsetInWindow: IntArray?
    ): Boolean {
        return mChildHelper?.dispatchNestedScroll(
                dxConsumed,
                dyConsumed,
                dxUnconsumed,
                dyUnconsumed,
                offsetInWindow
        ) ?: false
    }

    override fun dispatchNestedPreScroll(
            dx: Int,
            dy: Int,
            consumed: IntArray?,
            offsetInWindow: IntArray?
    ): Boolean {
        return mChildHelper?.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow) ?: false
    }

    override fun dispatchNestedFling(
            velocityX: Float,
            velocityY: Float,
            consumed: Boolean
    ): Boolean {
        return mChildHelper?.dispatchNestedFling(velocityX, velocityY, consumed) ?: false
    }

    override fun dispatchNestedPreFling(
            velocityX: Float,
            velocityY: Float
    ): Boolean {
        return mChildHelper?.dispatchNestedPreFling(velocityX, velocityY) ?: false
    }

    fun canScrollHor(direction: Int): Boolean {
        val offset = computeHorizontalScrollOffset()
        val range = computeHorizontalScrollRange() - computeHorizontalScrollExtent()
        if (range == 0) return false
        return if (direction < 0) {
            offset > 0
        } else {
            offset < range - 1
        }
    }

    inner class JavascriptInterface {
        @android.webkit.JavascriptInterface
        fun notifyVideoEnd() // Must match Javascript interface method of VideoEnabledWebChromeClient
        {
            Log.d("___", "GOT IT")
            // This code is not executed in the UI thread, so we must force that to happen
            Handler(Looper.getMainLooper()).post {
                if (videoEnabledWebChromeClient != null) {
                    videoEnabledWebChromeClient?.onHideCustomView()
                }
            }
        }
    }
}