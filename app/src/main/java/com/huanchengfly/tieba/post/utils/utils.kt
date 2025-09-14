package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.TiebaWebView
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.Destination
import java.io.IOException

fun launchUrl(
    context: Context,
    navigator: NavController,
    url: String,
) {
    val uri = Uri.parse(url)
    val host = uri.host
    val path = uri.path
    val scheme = uri.scheme
    if (host == null || scheme == null || path == null) {
        return
    }
    if (scheme == "tiebaclient") {
        when (uri.getQueryParameter("action")) {
            "preview_file" -> {
                val realUrl = uri.getQueryParameter("url")
                if (realUrl.isNullOrEmpty()) {
                    return
                }
                launchUrl(context, navigator, realUrl)
            }

            else -> {
                context.toastShort(R.string.toast_feature_unavailable)
            }
        }
        return
    }
    if (path.contains("android_asset", ignoreCase = true)) return

    if (path == "/mo/q/checkurl") {
        launchUrl(
            context,
            navigator,
            uri.getQueryParameter("url")?.replace("http://https://", "https://").orEmpty()
        )
        return
    }

    val blocked = TiebaWebView.interceptRequest(context, uri, onLaunchApp = null) { route ->
        navigator.navigate(route)
    }
    if (!blocked) {
        navigator.navigate(route = Destination.WebView(url))
    }
}

@SuppressLint("BatteryLife")
fun Context.requestIgnoreBatteryOptimizations() {
    val powerManager = (applicationContext as App).powerManager
    if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }
}

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    return (applicationContext as App).powerManager.isIgnoringBatteryOptimizations(packageName)
}

suspend fun requestPinShortcut(
    context: Context,
    shortcutId: String,
    iconImageUri: String,
    label: String,
    shortcutIntent: Intent
):Result<Unit> {
    if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        val imageResult = runCatching {
            GlideUtil.downloadCancelable(context, iconImageUri, null).inputStream().use {
                BitmapFactory.decodeStream(it)?: throw IOException("Decode $iconImageUri failed!")
            }
        }
        if (imageResult.isSuccess) {
            val shortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
                .setIcon(IconCompat.createWithBitmap(imageResult.getOrThrow()))
                .setIntent(shortcutIntent)
                .setShortLabel(label)
                .build()
            if (ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)) {
                return Result.success(Unit)
            }
        } else {
            val cause = imageResult.exceptionOrNull()
            val message = context.getString(R.string.load_shortcut_icon_fail)
            return Result.failure(UnsupportedOperationException(message, cause))
        }
    }
    val message = context.getString(R.string.launcher_not_support_pin_shortcut)
    return Result.failure(UnsupportedOperationException(message))
}