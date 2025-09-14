package com.huanchengfly.tieba.post.ui.page.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.format.Formatter
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.TiebaWebView.Companion.dumpWebViewVersion
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.About
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.buildAppSettingsIntent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.math.max

@SuppressLint("WebViewApiAvailability")
@Composable
fun MoreSettingsPage(navigator: NavController) = MyScaffold(
    backgroundColor = Color.Transparent,
    topBar = {
        TitleCentredToolbar(
            title = stringResource(id = R.string.title_settings_more),
            navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
        )
    },
) { paddingValues ->
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var diskCacheJob: Job? by remember { mutableStateOf(null) }

    BackHandler(diskCacheJob != null) { // 硬控用户直到清除完成
        coroutineScope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.tip_clearing_cache))
        }
    }

    PrefsScreen(
        contentPadding = paddingValues
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prefsItem {
                TextPref(
                    title = stringResource(R.string.title_use_webview),
                    summary = remember {
                        dumpWebViewVersion(context) ?: context.getString(R.string.toast_load_failed)
                    },
                    onClick = {
                        WebView.getCurrentWebViewPackage()?.packageName?.let {
                            runCatching { context.startActivity(buildAppSettingsIntent(it)) }
                        }
                    },
                    leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_chrome),
                )
            }
        }

        prefsItem {
            TextPref(
                leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                title = stringResource(id = R.string.title_settings_app_link),
                summary = stringResource(id = R.string.summary_app_link),
                onClick = {
                    runCatching {
                        context.startActivity(buildAppSettingsIntent(BuildConfig.APPLICATION_ID))
                    }
                    .onFailure {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.error_open_settings))
                        }
                    }
                }
            )
        }

        prefsItem {
            var cacheSize: Long by rememberSaveable { mutableLongStateOf(-1) }
            var summary: String by rememberSaveable { mutableStateOf("...") }
            val enabled by remember { derivedStateOf { cacheSize > 0 && diskCacheJob == null } }

            if (cacheSize == -1L) {
                LaunchedEffect(Unit) {
                    cacheSize = ImageCacheUtil.getCacheSize(context)
                    summary = context.getString(R.string.tip_cache, Formatter.formatShortFileSize(context, cacheSize))
                }
            }

            TextPref(
                title = stringResource(id = R.string.title_clear_picture_cache),
                onClick = {
                    diskCacheJob = coroutineScope
                        .launch { ImageCacheUtil.clearImageAllCache(context) }
                        .also {
                            val progressJob = coroutineScope.launch {
                                sizeProgress(context, cacheSize, onUpdate = { summary = it })
                            }
                            it.invokeOnCompletion {
                                diskCacheJob = null
                                progressJob.cancel()
                                cacheSize = 0
                                summary = context.getString(R.string.toast_clear_picture_cache_success)
                            }
                        }
                },
                summary = summary,
                leadingIcon = Icons.Rounded.DeleteForever,
                enabled = enabled
            )
        }

        prefsItem {
            TextPref(
                leadingIcon = Icons.Outlined.Info,
                title = stringResource(id = R.string.title_about),
                onClick = {
                    navigator.navigate(About)
                },
                summary = stringResource(id = R.string.tip_about, BuildConfig.VERSION_NAME)
            )
        }
    }
}

// Simulate progress for Glide#clearDiskCache
private suspend fun sizeProgress(context: Context, cache: Long, onUpdate: (String) -> Unit) {
    var cacheSize = cache
    val tipMessage = context.getString(R.string.tip_clearing_cache)

    while (cacheSize > 0) {
        onUpdate("$tipMessage ${cacheSize / 1024} KiB")
        cacheSize = max(0, cacheSize - 10240)
        delay(10)
    }
    yield()
    onUpdate(context.getString(R.string.tip_cache, "0 B"))
}
