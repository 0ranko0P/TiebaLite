package com.huanchengfly.tieba.post.ui.page.settings

import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Brightness2
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.setNightMode
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.AppFont
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import com.huanchengfly.tieba.post.utils.LauncherIcons
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CustomSettingsPage(navigator: NavController) = MyScaffold(
    backgroundColor = Color.Transparent,
    topBar = {
        TitleCentredToolbar(
            title = stringResource(id = R.string.title_settings_custom),
            navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
        )
    },
) { paddingValues ->
    PrefsScreen(
        contentPadding = paddingValues
    ) {
        prefsItem {
            TextPref(
                title = stringResource(id = R.string.title_custom_font_size),
                leadingIcon = Icons.Outlined.FontDownload,
                onClick = {
                    navigator.navigate(AppFont)
                }
            )
        }
        prefsItem {
            DarkThemeModePreference()
        }

        prefsItem {
            DarkThemePreference()
        }
        prefsItem {
            ListPref(
                key = stringPreferencesKey(AppIconUtil.KEY_APP_ICON),
                title = R.string.settings_app_icon,
                defaultValue = LauncherIcons.NEW_ICON,
                leadingIcon = Icons.Outlined.Apps,
                options = persistentMapOf(
                    LauncherIcons.NEW_ICON to R.string.icon_new,
                    LauncherIcons.NEW_ICON_INVERT to R.string.icon_new_invert,
                    LauncherIcons.OLD_ICON to R.string.icon_old
                ),
                optionsIconSupplier = { option ->
                    val icon = when (option) {
                        LauncherIcons.NEW_ICON -> R.drawable.ic_launcher_new_round
                        LauncherIcons.NEW_ICON_INVERT -> R.drawable.ic_launcher_new_invert_round
                        LauncherIcons.OLD_ICON -> R.drawable.ic_launcher_round
                        else -> throw RuntimeException("Invalid icon option: $option")
                    }

                    @Composable {
                        GlideImage(icon, contentDescription = null, Modifier.size(Sizes.Medium))
                    }
                },
                onValueChange = AppIconUtil::setIcon,
                useSelectedAsSummary = true
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            prefsItem {
                val currentLauncherIcon by rememberPreferenceAsState(
                    key = stringPreferencesKey(AppIconUtil.KEY_APP_ICON),
                    defaultValue = LauncherIcons.DEFAULT_ICON
                )
                val isCurrentSupportThemedIcon by remember { derivedStateOf {
                    currentLauncherIcon == LauncherIcons.SUPPORT_THEMED_ICON
                } }

                SwitchPref(
                    key = AppIconUtil.KEY_APP_THEMED_ICON,
                    title = R.string.title_settings_use_themed_icon,
                    defaultChecked = false,
                    enabled = isCurrentSupportThemedIcon,
                    leadingIcon = Icons.Outlined.ColorLens,
                    onCheckedChange = { checked: Boolean ->
                        if (checked) {
                            // Use mapped icon_name -> icon_name_themed when more themed icon added
                            AppIconUtil.setIcon(LauncherIcons.NEW_ICON_THEMED)
                        } else {
                            AppIconUtil.setIcon(currentLauncherIcon)
                        }
                    },
                    summary = {
                        R.string.tip_settings_use_themed_icon_summary_not_supported.takeUnless {
                            isCurrentSupportThemedIcon
                        }
                    }
                )
            }
        }
        prefsItem {
            SwitchPref(
                key = ThemeUtil.KEY_TINT_TOOLBAR,
                title = R.string.tip_toolbar_primary_color,
                defaultChecked = false,
                leadingIcon = Icons.Outlined.FormatColorFill,
                summary = { R.string.tip_toolbar_primary_color_summary },
            )
        }
        prefsItem {
            ForumListPreference()
        }
        prefsItem {
            SwitchPref(
                key = AppPreferencesUtils.KEY_LIFT_BOTTOM_BAR,
                title = R.string.title_lift_up_bottom_bar,
                defaultChecked = true,
                leadingIcon = Icons.Outlined.Upcoming,
                summary = { R.string.summary_lift_up_bottom_bar }
            )
        }
    }
}

@NonRestartableComposable
@Composable
fun DarkThemeModePreference(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ListPref(
        key = intPreferencesKey(ThemeUtil.KEY_DARK_THEME_MODE),
        title = R.string.title_settings_night_mode,
        modifier = modifier,
        defaultValue = ThemeUtil.DARK_MODE_FOLLOW_SYSTEM,
        onValueChange = { value ->
            // Notify changes manually instead of observe DataStore in Activity
            context.setNightMode(ThemeUtil.shouldUseNightMode(value))
        },
        leadingIcon = Icons.Outlined.DarkMode,
        options = persistentMapOf(
            ThemeUtil.DARK_MODE_ALWAYS to R.string.summary_night_mode_always,
            ThemeUtil.DARK_MODE_DISABLED to R.string.summary_night_mode_disabled,
            ThemeUtil.DARK_MODE_FOLLOW_SYSTEM to R.string.summary_night_mode_system
        )
    )
}

@NonRestartableComposable
@Composable
fun DarkThemePreference(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val enabled by context.dataStore.data
        .map { it[intPreferencesKey(ThemeUtil.KEY_DARK_THEME_MODE)] != ThemeUtil.DARK_MODE_DISABLED }
        .collectAsState(true)

    ListPref(
        key = stringPreferencesKey(ThemeUtil.KEY_DARK_THEME),
        title = R.string.settings_night_mode,
        modifier = modifier,
        defaultValue = ThemeUtil.THEME_AMOLED_DARK,
        leadingIcon = Icons.Outlined.Brightness2,
        enabled = enabled,
        options = persistentMapOf(
            ThemeUtil.THEME_BLUE_DARK to R.string.theme_blue_dark,
            ThemeUtil.THEME_GREY_DARK to R.string.theme_grey_dark,
            ThemeUtil.THEME_AMOLED_DARK to R.string.theme_amoled_dark
        ),
        useSelectedAsSummary = true,
    )
}

@NonRestartableComposable
@Composable
fun ForumListPreference(modifier: Modifier = Modifier) {
    SwitchPref(
        key = AppPreferencesUtils.KEY_HOME_SINGLE_FORUM_LIST,
        title = R.string.settings_forum_single,
        modifier = modifier,
        defaultChecked = false,
        leadingIcon = Icons.Outlined.ViewAgenda
    )
}