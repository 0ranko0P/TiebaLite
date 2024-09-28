package com.huanchengfly.tieba.post.ui.page.settings

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Brightness2
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.destinations.AppFontPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import com.huanchengfly.tieba.post.utils.LauncherIcons
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentMapOf

@OptIn(ExperimentalGlideComposeApi::class)
@Destination
@Composable
fun CustomSettingsPage(navigator: DestinationsNavigator) = MyScaffold(
    backgroundColor = Color.Transparent,
    topBar = {
        TitleCentredToolbar(
            title = stringResource(id = R.string.title_settings_custom),
            navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
        )
    },
) { paddingValues ->
    val context = LocalContext.current
    PrefsScreen(
        dataStore = context.dataStore,
        dividerThickness = 0.dp,
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
    ) {
        prefsItem {
            TextPref(
                title = stringResource(id = R.string.title_custom_font_size),
                leadingIcon = Icons.Outlined.FontDownload,
                onClick = {
                    navigator.navigate(AppFontPageDestination)
                }
            )
        }
        prefsItem {
            ListPref(
                key = stringPreferencesKey(ThemeUtil.KEY_DARK_THEME),
                title = R.string.settings_night_mode,
                defaultValue = ThemeUtil.THEME_AMOLED_DARK,
                leadingIcon = Icons.Outlined.Brightness2,
                options = persistentMapOf(
                    ThemeUtil.THEME_BLUE_DARK to R.string.theme_blue_dark,
                    ThemeUtil.THEME_GREY_DARK to R.string.theme_grey_dark,
                    ThemeUtil.THEME_AMOLED_DARK to R.string.theme_amoled_dark
                ),
                useSelectedAsSummary = true,
            )
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
                key = AppPreferencesUtils.KEY_FOLLOW_SYSTEM_NIGHT,
                title = R.string.title_settings_follow_system_night,
                defaultChecked = true,
                leadingIcon = Icons.Outlined.BrightnessAuto
            )
        }
        prefsItem {
            SwitchPref(
                key = ThemeUtil.KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR,
                title = R.string.tip_toolbar_primary_color,
                defaultChecked = false,
                leadingIcon = Icons.Outlined.FormatColorFill,
                summary = { R.string.tip_toolbar_primary_color_summary },
            )
        }
        prefsItem {
            SwitchPref(
                key = AppPreferencesUtils.KEY_HOME_SINGLE_FORUM_LIST,
                title = R.string.settings_forum_single,
                defaultChecked = false,
                leadingIcon = Icons.Outlined.ViewAgenda
            )
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