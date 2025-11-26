package com.huanchengfly.tieba.post.ui.page.settings

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentPasteSearch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettings
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SwitchPref
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TextPref
import com.huanchengfly.tieba.post.utils.buildAppSettingsIntent
import kotlinx.coroutines.launch

@Composable
fun PrivacySettingsPage(settings: Settings<PrivacySettings>, onBack: () -> Unit) {
    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_settings_privacy),
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) }
            )
        },
    ) { paddingValues ->
        PrefsScreen(
            settings = settings,
            initialValue = PrivacySettings(),
            contentPadding = paddingValues
        ) {
            AppLinkPreference()

            ClipboardPreference()
        }
    }
}

@Composable
fun PrefsScope<PrivacySettings>.AppLinkPreference(modifier: Modifier = Modifier) = TextItem {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current

    TextPref(
        modifier = modifier,
        leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
        title = stringResource(id = R.string.title_settings_app_link),
        summary = stringResource(id = R.string.summary_app_link),
        onClick = {
            runCatching {
                context.startActivity(
                    buildAppSettingsIntent(BuildConfig.APPLICATION_ID).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            action = android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                        }
                    }
                )
            }
            .onFailure {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.error_open_settings))
                }
            }
        }
    )
}

@Composable
fun PrefsScope<PrivacySettings>.ClipboardPreference(modifier: Modifier = Modifier) = Item { settings ->
    SwitchPref(
        modifier = modifier,
        checked = settings.readClipBoardLink,
        onCheckedChange = {
            updatePreference { old -> old.copy(readClipBoardLink = it) }
        },
        title = R.string.title_settings_clipboard_link,
        summary = R.string.summary_clipboard_link,
        leadingIcon = Icons.Outlined.ContentPasteSearch
    )
}
