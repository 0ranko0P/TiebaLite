package com.huanchengfly.tieba.post.ui.page.settings

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

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
            var cacheSize: String? by remember { mutableStateOf(null) }
            if (cacheSize == null) {
                LaunchedEffect(Unit) {
                    coroutineScope.launch { cacheSize = ImageCacheUtil.getCacheSize(context) }
                }
            }

            TextPref(
                title = stringResource(id = R.string.title_clear_picture_cache),
                onClick = {
                    cacheSize = "0.0B"
                    diskCacheJob = MainScope().launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_picture_cache_success))
                        ImageCacheUtil.clearImageAllCache(context)
                    }
                },
                summary = stringResource(id = R.string.tip_cache, cacheSize ?: "..."),
                leadingIcon = Icons.Rounded.DeleteForever,
                enabled = diskCacheJob?.isCompleted != false
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
