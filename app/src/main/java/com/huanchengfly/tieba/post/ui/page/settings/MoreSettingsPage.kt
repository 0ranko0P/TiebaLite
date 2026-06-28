package com.huanchengfly.tieba.post.ui.page.settings

import android.annotation.SuppressLint
import android.os.Build
import android.text.format.Formatter
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.MediaCache
import com.huanchengfly.tieba.post.components.TiebaWebView.Companion.dumpWebViewVersion
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.preference
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.buildAppSettingsIntent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("WebViewApiAvailability")
@OptIn(UnstableApi::class)
@Composable
fun MoreSettingsPage(navigator: NavController) {
    val context = LocalContext.current

    SettingsScaffold(
        titleRes = R.string.title_settings_more,
        onBack = navigator::navigateUp,
    ) {
        group(title = R.string.summary_settings_more) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                preference(
                    title = { Text(text = stringResource(R.string.title_use_webview)) },
                    summary = {
                        val version = remember { dumpWebViewVersion(context) }
                        Text(text = version ?: stringResource(id = R.string.toast_load_failed))
                    },
                    onClick = {
                        runCatching {
                            val pkg = WebView.getCurrentWebViewPackage() ?: return@runCatching
                            context.startActivity(buildAppSettingsIntent(pkg.packageName))
                        }
                    },
                    icon = {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_chrome), contentDescription = null)
                    },
                )
            }

            preference(
                title = R.string.title_settings_worker,
                onClick = {
                    navigator.navigate(route = SettingsDestination.WorkInfo)
                },
                leadingIcon = Icons.Outlined.Analytics,
            )
        }

        group(title = R.string.settings_group_cache) {
            customPreference {
                CacheClearPreference(
                    title = stringResource(id = R.string.title_clear_picture_cache),
                    getCacheSize = { ImageCacheUtil.getCacheSize(context.applicationContext) },
                    clearCache = { ImageCacheUtil.clearImageAllCache(context.applicationContext) },
                    successToastRes = R.string.toast_clear_picture_cache_success,
                    failureToastRes = R.string.toast_clear_picture_cache_failed,
                    shapes = it,
                )
            }

            customPreference {
                CacheClearPreference(
                    title = stringResource(id = R.string.title_clear_video_cache),
                    getCacheSize = { MediaCache.sizeBytes(context.applicationContext) },
                    clearCache = {
                        MediaCache.clear(
                            context.applicationContext,
                            excludeCacheKeys = MediaCache.activeKeys(),
                        )
                    },
                    successToastRes = R.string.toast_clear_video_cache_success,
                    failureToastRes = R.string.toast_clear_video_cache_failed,
                    shapes = it,
                )
            }
        }
    }
}

@Composable
private fun CacheClearPreference(
    title: String,
    getCacheSize: suspend () -> Long,
    clearCache: suspend () -> Unit,
    @StringRes successToastRes: Int,
    @StringRes failureToastRes: Int,
    modifier: Modifier = Modifier,
    shapes: ListItemShapes,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var cacheJob: Job? by retain { mutableStateOf(null) }
    BackHandler(cacheJob != null) {
        context.toastShort(context.getString(R.string.tip_clearing_cache))
    }

    var cacheSizeText: String? by rememberSaveable { mutableStateOf(null) }

    fun refreshCacheSize() {
        coroutineScope.launch {
            val size = withContext(Dispatchers.IO) { getCacheSize() }
            val formattedSize = Formatter.formatShortFileSize(context, size)
            cacheSizeText = context.getString(R.string.tip_cache, formattedSize)
        }
    }

    LaunchedEffect(Unit) {
        refreshCacheSize()
    }

    SegmentedPreference(
        modifier = modifier,
        title = title,
        shapes = shapes,
        leadingIcon = Icons.Rounded.DeleteForever,
        summary = cacheSizeText ?: stringResource(id = R.string.text_loading),
        onClick = {
            context.toastShort(context.getString(R.string.tip_clearing_cache))
            cacheJob =
                coroutineScope
                    .launch {
                        withContext(Dispatchers.IO) { clearCache() }
                    }.also {
                        it.invokeOnCompletion { error ->
                            coroutineScope.launch {
                                cacheJob = null
                                if (error == null) {
                                    refreshCacheSize()
                                    delay(300)
                                    context.toastShort(context.getString(successToastRes))
                                } else if (error !is CancellationException) {
                                    context.toastShort(
                                        context.getString(
                                            R.string.toast_clear_failure,
                                            error.message ?: context.getString(failureToastRes),
                                        ),
                                    )
                                }
                            }
                        }
                    }
        },
        enabled = cacheSizeText != null && cacheJob == null,
    )
}
