package com.huanchengfly.tieba.post.ui.page.settings

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentPasteSearch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettings
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SettingsSegmentedPrefsScope
import com.huanchengfly.tieba.post.utils.buildAppSettingsIntent
import kotlinx.coroutines.launch

@Composable
fun PrivacySettingsPage(settings: Settings<PrivacySettings>, onBack: () -> Unit) {
    SettingsScaffold(
        titleRes = R.string.title_settings_privacy,
        onBack = onBack,
        settings = settings,
        initialValue = PrivacySettings(),
    ) {
        appLinkPreference()

        clipboardPreference()
    }
}

fun SettingsSegmentedPrefsScope<PrivacySettings>.appLinkPreference() = customPreference { shapes ->
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current

    SegmentedPreference(
        title = R.string.title_settings_app_link,
        shapes = shapes,
        summary = R.string.summary_app_link,
        leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
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

fun SettingsSegmentedPrefsScope<PrivacySettings>.clipboardPreference() {
    toggleablePreference(
        property = PrivacySettings::readClipBoardLink,
        title = R.string.title_settings_clipboard_link,
        summary = R.string.summary_clipboard_link,
        leadingIcon = Icons.Outlined.ContentPasteSearch
    )
}
