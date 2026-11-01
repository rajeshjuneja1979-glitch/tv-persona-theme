package com.tvtheme.p3launcher

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import java.io.ByteArrayInputStream

class WebActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var holder: FrameLayout
    private var custom: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        holder = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        web = WebView(this)
        holder.addView(web, FrameLayout.LayoutParams(-1, -1))
        setContentView(holder)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = userAgentString.replace("; wv", "").replace(" Mobile", "")
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                return if (AdBlock.blocked(request?.url?.host))
                    WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                else null
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null) return
                custom = view; customCb = callback
                holder.addView(view, FrameLayout.LayoutParams(-1, -1))
                web.visibility = View.GONE
            }
            override fun onHideCustomView() { hideCustom() }
        }
        web.loadUrl(intent.getStringExtra("url") ?: "about:blank")
        web.requestFocus()
    }

    private fun hideCustom() {
        custom?.let { holder.removeView(it) }
        custom = null
        customCb?.onCustomViewHidden(); customCb = null
        web.visibility = View.VISIBLE
        web.requestFocus()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        when {
            custom != null -> hideCustom()
            web.canGoBack() -> web.goBack()
            else -> finish()
        }
    }

    override fun onDestroy() { web.destroy(); super.onDestroy() }
}
