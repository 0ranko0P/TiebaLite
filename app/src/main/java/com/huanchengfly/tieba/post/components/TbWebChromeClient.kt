package com.huanchengfly.tieba.post.components

import android.Manifest
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
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.components.dialogs.PermissionDialog
import com.huanchengfly.tieba.post.models.PermissionBean
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.AccompanistWebChromeClient
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.PermissionUtils.onDenied
import com.huanchengfly.tieba.post.utils.PermissionUtils.onGranted
import com.huanchengfly.tieba.post.utils.compose.launchActivityForResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.UUID

class TbWebChromeClient(
    val context: Context,
    coroutineScope: CoroutineScope,
) : AccompanistWebChromeClient() {

    private var uploadMessage: ValueCallback<Array<Uri>>? = null

    val id: String = UUID.randomUUID().toString()

    init {
        coroutineScope.onGlobalEvent<GlobalEvent.ActivityResult>(
            filter = { it.requesterId == id },
        ) {
            uploadMessage?.onReceiveValue(FileChooserParams.parseResult(it.resultCode, it.intent))
            uploadMessage = null
        }
    }

    private fun isEnabledLocationFunction(): Boolean {
        val locationManager = context.getSystemService<LocationManager>()
        return locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        if (origin == null || callback == null) return
        PermissionDialog(
            context,
            PermissionBean(
                PermissionDialog.CustomPermission.PERMISSION_LOCATION,
                origin,
                context.getString(R.string.title_ask_permission_location, origin),
                R.drawable.ic_round_location_on
            )
        )
            .setOnGrantedCallback { isForever: Boolean ->
                MainScope().launch {
                    context.askPermission(
                        desc = R.string.usage_webview_location_permission,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    .onGranted {
                        if (isEnabledLocationFunction()) {
                            callback.invoke(origin, true, isForever)
                        } else {
                            callback.invoke(origin, false, false)
                        }
                    }
                    .onDenied {
                        context.toastShort(R.string.tip_no_permission)
                        callback.invoke(origin, false, false)
                    }
                }
            }
            .setOnDeniedCallback {
                callback.invoke(origin, false, false)
            }
            .show()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        if (webView == null || filePathCallback == null || fileChooserParams == null) return false
        uploadMessage?.onReceiveValue(null)
        uploadMessage = filePathCallback
        launchActivityForResult(id, fileChooserParams.createIntent())
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?,
    ): Boolean {
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

    override fun onJsConfirm(
        view: WebView,
        url: String?,
        message: String?,
        result: JsResult,
    ): Boolean {
        if ("ClipboardGuardCopyRequest".equals(message, ignoreCase = true)) {
            val host = Uri.parse(url).host ?: return true
            PermissionDialog(
                context,
                PermissionBean(
                    PermissionDialog.CustomPermission.PERMISSION_CLIPBOARD_COPY,
                    host,
                    context.getString(R.string.title_ask_permission_clipboard_copy, host),
                    R.drawable.ic_round_file_copy
                )
            )
                .setOnGrantedCallback { result.confirm() }
                .setOnDeniedCallback { result.cancel() }
                .show()
        } else {
            AlertDialog.Builder(view.context)
                .setTitle("Confirm")
                .setMessage(message)
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
}
