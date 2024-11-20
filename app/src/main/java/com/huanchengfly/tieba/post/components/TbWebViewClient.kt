package com.huanchengfly.tieba.post.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.core.net.toUri
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector.isBaidu
import com.huanchengfly.tieba.post.components.dialogs.PermissionDialog
import com.huanchengfly.tieba.post.models.PermissionBean
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.AccompanistWebViewClient
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.FileUtil
import java.io.IOException

open class TbWebViewClient(
    protected val context: Context,
    protected val onNavigate: ((Destination) -> Unit)? = null
) : AccompanistWebViewClient() {

    private val clipboardGuardJs by unsafeLazy {
        FileUtil.readAssetFile(context, "ClipboardGuard.js") ?: throw IOException()
    }

    private val cookieManager by unsafeLazy { CookieManager.getInstance() }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (url?.startsWith("http") == true) {
            view.evaluateJavascript(clipboardGuardJs, null)
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return TiebaWebView.interceptRequest(context, request.url, this::onLaunchApp, onNavigate)
    }

    // Show Confirm dialog when web request invokes a DeepLink
    private fun onLaunchApp(uri: Uri, name: String) {
        val scheme = uri.scheme ?: ""
        PermissionDialog(
            context,
            PermissionBean(
                PermissionDialog.CustomPermission.PERMISSION_START_APP,
                "${uri.host}_${scheme}",
                context.getString(R.string.title_start_app_permission, uri.host, name),
                R.drawable.ic_round_exit_to_app
            )
        )
            .setOnGrantedCallback {
                val intent = if (uri.scheme.equals("intent", ignoreCase = true)) {
                    Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                } else {
                    Intent(Intent.ACTION_VIEW, uri)
                }
                runCatching {
                    context.startActivity(intent)
                }
            }
            .show()
    }

    open fun injectCookies(url: String) {
        val cookieStr = cookieManager.getCookie(url) ?: ""
        val cookies = AccountUtil.parseCookie(cookieStr)
        val BDUSS = cookies["BDUSS"]
        val currentAccountBDUSS = AccountUtil.getBduss()
        if (currentAccountBDUSS != null && BDUSS != currentAccountBDUSS) {
            cookieManager.setCookie(url, AccountUtil.getBdussCookie(currentAccountBDUSS))
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url?.toUri()?.isBaidu() == true) {
            injectCookies(url = url)
        }
    }
}
