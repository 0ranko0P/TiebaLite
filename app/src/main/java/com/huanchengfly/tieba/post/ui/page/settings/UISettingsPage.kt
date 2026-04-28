package com.huanchengfly.tieba.post.ui.page.settings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.LabelImportant
import androidx.compose.material.icons.automirrored.outlined.LabelOff
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Houseboat
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.models.settings.DarkPreference
import com.huanchengfly.tieba.post.ui.models.settings.NavigationLabel
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.AppFont
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SettingsSegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.preference
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.LauncherIcons
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun UISettingsPage(
    settings: Settings<UISettings>,
    navigator: NavController
) {
    val context = LocalContext.current
    val toyFansIcon = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_toy_fans)

    SettingsScaffold(
        titleRes = R.string.title_settings_custom,
        onBack = navigator::navigateUp,
        settings = settings,
        initialValue = UISettings(),
    ) {
        group(title = R.string.settings_group_display) {
            preference(
                title = R.string.title_custom_font_size,
                leadingIcon = Icons.Outlined.FontDownload,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = {
                    navigator.navigate(AppFont)
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                reduceEffectPreference()
            }
        }

        group(title = R.string.settings_group_dark_mode) {
            darkThemeModePreference()

            toggleablePreference(
                property = UISettings::darkAmoled,
                title = R.string.title_settings_dark_amoled,
                summary = R.string.summary_dark_amoled,
                enabled = currentPreference.darkPreference != DarkPreference.DISABLED,
                leadingIcon = Icons.Outlined.Contrast
            )

            darkImagePreference()
        }

        group(title = R.string.settings_group_icon) {
            listPref(
                value = currentPreference.appIcon,
                title = context.getString(R.string.settings_app_icon),
                leadingIcon = Icons.Outlined.Apps,
                options = persistentMapOf(
                    LauncherIcons.NEW_ICON to context.getString(R.string.icon_new),
                    LauncherIcons.NEW_ICON_INVERT to context.getString(R.string.icon_new_invert),
                    LauncherIcons.OLD_ICON to context.getString(R.string.icon_old)
                ),
                optionsIconSupplier = { option ->
                    val icon = when (option) {
                        LauncherIcons.NEW_ICON -> R.drawable.ic_launcher_new_round
                        LauncherIcons.NEW_ICON_INVERT -> R.drawable.ic_launcher_new_invert_round
                        LauncherIcons.OLD_ICON -> R.drawable.ic_launcher_round
                        else -> throw RuntimeException("Invalid icon option: $option")
                    }
                    Image(painter = painterResource(icon), null, Modifier.size(Sizes.Medium))
                },
                onValueChange = { newIcon ->
                    settings.save { old -> old.copy(appIcon = newIcon) }
                    AppIconUtil.setIcon(newIcon, context)
                },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                toggleablePreference(
                    property = UISettings::appIconThemed,
                    onCheckedChange = { checked ->
                        val newIcon = when {
                            !currentPreference.appIcon.supportThemedIcon() -> null
                            checked -> LauncherIcons.NEW_ICON_THEMED
                            else -> currentPreference.appIcon
                        }
                        if (newIcon != null) {
                            AppIconUtil.setIcon(newIcon, context.applicationContext)
                        }
                    },
                    title = R.string.title_settings_use_themed_icon,
                    // enabled = uiSettings.appIcon.supportThemedIcon(),
                    leadingIcon = Icons.Outlined.ColorLens,
                    summary = R.string.tip_themed_icon_unsupported.takeIf {
                        !currentPreference.appIcon.supportThemedIcon()
                    }
                )
            }
        }

        group(title = R.string.settings_group_main_page) {
            toggleablePreference(
                property = UISettings::bottomNavFloating,
                title = R.string.settings_nav_floating,
                summary = R.string.summary_nav_floating,
                leadingIcon = if (currentPreference.bottomNavFloating) {
                    Icons.Outlined.Houseboat
                } else  {
                    Icons.Outlined.House
                }
            )

            listPref(
                property = UISettings::bottomNavLabel,
                title = R.string.settings_nav_label,
                leadingIcon = when (currentPreference.bottomNavLabel) {
                    NavigationLabel.ALWAYS -> Icons.AutoMirrored.Outlined.Label
                    NavigationLabel.SELECTED -> Icons.AutoMirrored.Outlined.LabelImportant
                    NavigationLabel.NONE -> Icons.AutoMirrored.Outlined.LabelOff
                },
                options = persistentMapOf(
                    NavigationLabel.ALWAYS to R.string.title_nav_label_always,
                    NavigationLabel.SELECTED to R.string.title_nav_label_selected,
                    NavigationLabel.NONE to R.string.title_nav_label_none
                ),
            )

            forumListPreference()

            toggleablePreference(
                property = UISettings::showHistoryInHome,
                title = R.string.settings_home_page_show_history_forum,
                leadingIcon = Icons.Outlined.WatchLater
            )

            toggleablePreference(
                property = UISettings::hideExplore,
                title = R.string.title_hide_explore,
                leadingIcon = toyFansIcon.imageVector,
            )
        }
    }
}

fun SettingsSegmentedPrefsScope<UISettings>.darkImagePreference() {
    toggleablePreference(
        property = UISettings::darkenImage,
        title = R.string.settings_image_darken_when_night_mode,
        leadingIcon = Icons.Outlined.NightsStay,
        enabled = currentPreference.darkPreference != DarkPreference.DISABLED
    )
}

fun SettingsSegmentedPrefsScope<UISettings>.darkThemeModePreference() {
    listPref(
        property = UISettings::darkPreference,
        title = R.string.title_settings_night_mode,
        leadingIcon = Icons.Outlined.DarkMode,
        options = persistentMapOf(
            DarkPreference.ALWAYS to R.string.summary_night_mode_always,
            DarkPreference.DISABLED to R.string.summary_night_mode_disabled,
            DarkPreference.FOLLOW_SYSTEM to R.string.summary_night_mode_system
        )
    )
}

fun SettingsSegmentedPrefsScope<UISettings>.forumListPreference() {
    toggleablePreference(
        property = UISettings::homeForumList,
        title = R.string.settings_forum_single,
        leadingIcon = if (currentPreference.homeForumList) {
            Icons.Outlined.ViewAgenda
        } else {
            Icons.Outlined.ViewColumn
        }
    )
}

@RequiresApi(Build.VERSION_CODES.S)
fun SettingsSegmentedPrefsScope<UISettings>.reduceEffectPreference() {
    toggleablePreference(
        property = UISettings::reduceEffect,
        title = R.string.title_reduce_effect,
        leadingIcon = Icons.Outlined.BlurOn,
    )
}
