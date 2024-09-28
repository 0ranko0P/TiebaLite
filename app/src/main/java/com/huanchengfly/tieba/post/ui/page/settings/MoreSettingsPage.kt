package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Web
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.destinations.AboutPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination
@Composable
fun MoreSettingsPage(navigator: DestinationsNavigator) = MyScaffold(
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

    PrefsScreen(
        dataStore = LocalContext.current.dataStore,
        dividerThickness = 0.dp,
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
    ) {
        prefsItem {
            SwitchPref(
                key = AppPreferencesUtils.KEY_USE_WEB_VIEW,
                leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_chrome),
                title = R.string.title_use_webview,
                summaryOn = R.string.tip_use_webview_on,
                summaryOff = R.string.tip_use_webview,
                defaultChecked = true
            )
        }
        prefsItem {
            val useWebView by context.dataStore.collectPreferenceAsState(
                key = booleanPreferencesKey(AppPreferencesUtils.KEY_USE_WEB_VIEW),
                defaultValue = true
            )
            SwitchPref(
                key = AppPreferencesUtils.KEY_WEB_VIEW_CUSTOM_TAB,
                leadingIcon = Icons.Rounded.Web,
                enabled = !useWebView,
                title = R.string.title_use_custom_tabs,
                summary = { checked ->
                    if (checked) R.string.tip_use_custom_tab_on else R.string.tip_use_custom_tab
                },
                defaultChecked = true,
            )
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
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_picture_cache_success))
                        ImageCacheUtil.clearImageAllCache(context)
                    }
                },
                summary = stringResource(id = R.string.tip_cache, cacheSize ?: "..."),
                leadingIcon = Icons.Rounded.DeleteForever
            )
        }
        prefsItem {
            TextPref(
                leadingIcon = Icons.Outlined.Info,
                title = stringResource(id = R.string.title_about),
                onClick = {
                    navigator.navigate(AboutPageDestination)
                },
                summary = stringResource(id = R.string.tip_about, BuildConfig.VERSION_NAME)
            )
        }
    }
}
