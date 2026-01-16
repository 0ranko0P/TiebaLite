package com.huanchengfly.tieba.post.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebBackForwardList
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector.isBaidu
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector.isHttp
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector.isLogin
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.loadPackageLabel
import com.huanchengfly.tieba.post.utils.queryDeepLink
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.full.createType

class TiebaWebView(context: Context): WebView(context) {

    /**
     * Record last navigation route to avoid redirect loop when back pressed
     * */
    private var lastValidRoute: String? = null

    val isContentVerticalScrollable: Boolean
        get() = measuredHeight < computeVerticalScrollRange()

    fun canNavigate(route: Destination): Boolean {
        val jsonRoute = Json.encodeToString(serializer(route::class.createType()), route)
        if (lastValidRoute == jsonRoute) {
            lastValidRoute = null
            if (canGoBack()) goBack()
            return false
        } else {
            lastValidRoute = jsonRoute
            return true
        }
    }

    override fun saveState(outState: Bundle): WebBackForwardList? {
        lastValidRoute?.let { outState.putString(KEY_LAST_NAV_ROUTE, it) }
        return super.saveState(outState)
    }

    override fun restoreState(inState: Bundle): WebBackForwardList? {
        lastValidRoute = inState.getString(KEY_LAST_NAV_ROUTE)
        return super.restoreState(inState)
    }

    override fun destroy() {
        dispose(this)
        super.destroy()
    }

    companion object {

        private const val KEY_LAST_NAV_ROUTE = "WEB_ROUTE"

        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("WebViewApiAvailability")
        fun dumpWebViewVersion(context: Context): String? {
            return getCurrentWebViewPackage()?.let {
                val appInfo = it.applicationInfo?: return null
                val name = context.packageManager.getApplicationLabel(appInfo)
                val version = "${it.versionName} (${it.versionCode})"
                "$name\n$version"
            }
        }

        fun launchCustomTab(context: Context, url: Uri): Result<Unit> = runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
                .build()
                .launchUrl(context, url)
        }.onFailure {
            Intent(Intent.ACTION_VIEW, url).apply {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(this)
            }
        }

        /**
         * Intercept incoming web request
         *
         * @param context Context
         * @param url URL
         * @param onLaunchApp Callback to be invoked when [url] request a valid DeepLink
         * @param onNavigate Callback to be invoked when [url] converted to the destination route
         *
         * @return True to cancel this request
         *
         * @see [ClipBoardLinkDetector.parseDeepLink]
         * */
        fun interceptRequest(
            context: Context,
            url: Uri,
            onLaunchApp: ((Uri, String) -> Unit)?,
            onNavigate: ((Destination) -> Unit)?
        ): Boolean {
            // Check is tieba link & deeplink
            val tiebaDeepLink = ClipBoardLinkDetector.parseDeepLink(url)
            val isTieba = tiebaDeepLink != null

            when {
                isTieba -> {
                    onNavigate?.invoke(tiebaDeepLink.toRoute())
                    return true
                }

                url.isLogin() -> {
                    onNavigate?.invoke(Destination.Login)
                    return true
                }

                url.isBaidu() -> return false

                !url.isHttp() -> { // Third party DeepLink
                    if (onLaunchApp == null) return true
                    context.queryDeepLink(url).loadPackageLabel(context)?.let { appName ->
                        onLaunchApp(url, appName.toString())
                    }
                    return true
                }

                // Load all external site in CustomTab
                else -> {
                    // if (context.appPreferences.useWebView) return false
                    launchCustomTab(context, url)
                    return true
                }
            }
        }

        // Dispose this webView, not destroy it
        fun dispose(webView: WebView) {
            with(webView) {
                clearHistory()
                clearCache(false)
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                settings.javaScriptEnabled = false
            }
        }
    }
}