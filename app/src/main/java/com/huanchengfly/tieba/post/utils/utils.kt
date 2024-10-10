package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.WebViewPageDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.io.IOException

fun launchUrl(
    context: Context,
    navigator: DestinationsNavigator,
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
    if (!path.contains("android_asset")) {
        if (path == "/mo/q/checkurl") {
            launchUrl(
                context,
                navigator,
                uri.getQueryParameter("url")?.replace("http://https://", "https://").orEmpty()
            )
            return
        }
        if (host == "tieba.baidu.com" && path.startsWith("/p/")) {
            val threadId = path.substring(3).toLongOrNull()
            if (threadId != null) {
                navigator.navigate(
                    ThreadPageDestination(threadId)
                )
            }
            return
        }
        val isTiebaLink =
            host.contains("tieba.baidu.com") || host.contains("wappass.baidu.com") || host.contains(
                "ufosdk.baidu.com"
            ) || host.contains("m.help.baidu.com")
        if (isTiebaLink || context.appPreferences.useWebView) {
            navigator.navigate(
                WebViewPageDestination(url)
            )
        } else {
            if (context.appPreferences.useCustomTabs) {
                val theme by ThemeUtil.themeState
                val intentBuilder = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setDefaultColorSchemeParams(
                        CustomTabColorSchemeParams.Builder()
                            .setToolbarColor(theme.topBar.toArgb())
                            .build()
                    )
                try {
                    intentBuilder.build().launchUrl(context, uri)
                } catch (e: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            } else {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }
}

val Context.powerManager: PowerManager
    get() = getSystemService(Context.POWER_SERVICE) as PowerManager

@SuppressLint("BatteryLife")
fun Context.requestIgnoreBatteryOptimizations() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${packageName}")
            startActivity(intent)
        }
    }
}

fun Context.isIgnoringBatteryOptimizations(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(packageName)
    } else {
        true
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
            DownloadUtil.downloadCancelable(url = iconImageUri, onProgress = null).use {
                BitmapFactory.decodeStream(it.byteStream())?: throw IOException("Decode $iconImageUri failed!")
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