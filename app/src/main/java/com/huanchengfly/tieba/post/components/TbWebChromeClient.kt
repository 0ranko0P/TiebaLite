package com.huanchengfly.tieba.post.components

import android.content.Context
import android.location.LocationManager
import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.dialogs.WebPermissionDialog
import com.huanchengfly.tieba.post.components.dialogs.WebPermissionDialog.Companion.WebPermission
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.AccompanistWebChromeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.launch

class TbWebChromeClient(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) : AccompanistWebChromeClient() {

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        val host = Uri.parse(origin).host
        if (host.isNullOrEmpty()) {
            callback.deny(origin); return
        }

        coroutineScope.launch {
            when (WebPermissionDialog.getState(context, host, WebPermission.LOCATION)) {

                WebPermissionDialog.STATE_ALLOW -> callback.grant(origin)

                WebPermissionDialog.STATE_DENY -> callback.deny(origin)

                else -> {
                    val message = context.getString(R.string.title_ask_permission_location, host)
                    WebPermissionDialog.newInstance(WebPermission.LOCATION, host, message)
                        .show(context)
                        .receiveResult()
                        .onFailure { callback.deny(origin) }
                        .onSuccess { granted ->
                            if (granted) {
                                callback.grant(origin)
                            } else {
                                callback.deny(origin)
                                context.toastShort(R.string.tip_no_permission)
                            }
                        }
                }
            }
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (webView == null || filePathCallback == null || fileChooserParams == null) return false

        coroutineScope.launch {
            val host = Uri.parse(webView.url).host ?: "?"
            val message = context.getString(R.string.title_ask_permission_file, host)
            val intent = fileChooserParams.createIntent()

            WebPermissionDialog.newInstance(WebPermission.File(intent), host, message)
                .show(context)
                .receiveResult()
                .onFailure { filePathCallback.onReceiveValue(null) }
                .onSuccess {
                    filePathCallback.onReceiveValue(FileChooserParams.parseResult(it.resultCode, it.data))
                }
        }
        return true
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        AlertDialog.Builder(view?.context ?: context)
            .setMessage(message)
            .setPositiveButton(R.string.button_sure_default) { _, _ ->
                result?.confirm()
            }
            .setCancelable(false)
            .create()
            .show()
        return true
    }

    override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        if ("ClipboardGuardCopyRequest".equals(message, ignoreCase = true)) {
            val host = Uri.parse(url).host ?: return false
            onClipboardGuardRequest(host, result)
        } else {
            AlertDialog.Builder(view.context)
                .setTitle("Confirm")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    result.confirm()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    result.cancel()
                }
                .create()
                .show()
        }
        return true
    }

    private fun onClipboardGuardRequest(host: String, result: JsResult) = coroutineScope.launch {
        when (WebPermissionDialog.getState(context, host, WebPermission.CLIPBOARD)) {

            WebPermissionDialog.STATE_ALLOW -> result.confirm()

            WebPermissionDialog.STATE_DENY -> result.cancel()

            else -> {
                val msg = context.getString(R.string.title_ask_permission_clipboard, host)

                WebPermissionDialog.newInstance(WebPermission.CLIPBOARD, host, msg)
                    .show(context)
                    .receiveResult()
                    .onFailure { result.cancel() }
                    .onSuccess { grant -> if (grant) result.confirm() else result.cancel() }
            }
        }
    }

    companion object {

        private fun isEnabledLocationFunction(): Boolean {
            val locationManager = App.INSTANCE.getSystemService<LocationManager>()
            return locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager)
        }

        // Do not retain state in callback
        private fun GeolocationPermissions.Callback.deny(origin: String) = invoke(origin, false, false)

        // Do not retain state in callback
        private fun GeolocationPermissions.Callback.grant(origin: String) {
            invoke(origin, isEnabledLocationFunction(), false)
        }
    }
}
