package com.huanchengfly.tieba.post.ui.page.settings

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.models.settings.DarkPreference
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.AppFont
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.ListPref
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SwitchPref
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TextPref
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.LauncherIcons
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun UISettingsPage(
    settings: Settings<UISettings>,
    navigator: NavController
) {
    val context = LocalContext.current

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_settings_custom),
                navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
            )
        },
    ) { paddingValues ->
        PrefsScreen(
            settings = settings,
            initialValue = UISettings(),
            contentPadding = paddingValues
        ) {
            TextItem {
                TextPref(
                    modifier = Modifier.semantics { hideFromAccessibility() },
                    title = stringResource(id = R.string.title_custom_font_size),
                    leadingIcon = Icons.Outlined.FontDownload,
                    onClick = {
                        navigator.navigate(AppFont)
                    }
                )
            }

            DarkThemeModePreference()

            Item { uiSettings ->
                SwitchPref(
                    checked = uiSettings.darkAmoled,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(darkAmoled = it) }
                    },
                    title = R.string.title_settings_dark_amoled,
                    enabled = uiSettings.darkPreference != DarkPreference.DISABLED,
                    leadingIcon = Icons.Outlined.Contrast
                )
            }

            DarkImagePreference()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ReduceEffectPreference()
            }

            Item { uiSettings ->
                ListPref(
                    value = uiSettings.appIcon,
                    title = R.string.settings_app_icon,
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
                        Image(painter = painterResource(icon), null, Modifier.size(Sizes.Medium))
                    },
                    onValueChange = { newIcon ->
                        updatePreference { old -> old.copy(appIcon = newIcon) }
                        AppIconUtil.setIcon(newIcon, context)
                    },
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Item { uiSettings ->
                    SwitchPref(
                        checked = uiSettings.appIconThemed,
                        onCheckedChange = { checked ->
                            updatePreference { old -> old.copy(appIconThemed = checked) }
                            if (!uiSettings.appIcon.supportThemedIcon()) return@SwitchPref

                            if (checked) {
                                // Use mapped icon_name -> icon_name_themed when more themed icon added
                                AppIconUtil.setIcon(LauncherIcons.NEW_ICON_THEMED, context)
                            } else {
                                AppIconUtil.setIcon(uiSettings.appIcon, context)
                            }
                        },
                        title = R.string.title_settings_use_themed_icon,
                        // enabled = uiSettings.appIcon.supportThemedIcon(),
                        leadingIcon = Icons.Outlined.ColorLens,
                        summary = R.string.tip_themed_icon_unsupported.takeIf {
                            !uiSettings.appIcon.supportThemedIcon()
                        }
                    )
                }
            }

            ForumListPreference()

            Item { uiSettings ->
                SwitchPref(
                    checked = uiSettings.liftBottomBar,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(liftBottomBar = it) }
                    },
                    title = R.string.title_lift_up_bottom_bar,
                    leadingIcon = Icons.Outlined.Upcoming,
                    summary = R.string.summary_lift_up_bottom_bar
                )
            }
        }
    }
}

@Composable
fun PrefsScope<UISettings>.DarkImagePreference(modifier: Modifier = Modifier) = Item { uiSettings ->
    SwitchPref(
        modifier = modifier,
        checked = uiSettings.darkenImage,
        onCheckedChange = {
            updatePreference { old -> old.copy(darkenImage = it)}
        },
        title = R.string.settings_image_darken_when_night_mode,
        leadingIcon = Icons.Outlined.NightsStay,
        enabled = uiSettings.darkPreference != DarkPreference.DISABLED
    )
}

@Composable
fun PrefsScope<UISettings>.DarkThemeModePreference(modifier: Modifier = Modifier) = Item { uiSettings ->
    ListPref(
        modifier = modifier,
        value = uiSettings.darkPreference,
        title = R.string.title_settings_night_mode,
        onValueChange = {
            updatePreference { old -> old.copy(darkPreference = it) }
        },
        leadingIcon = Icons.Outlined.DarkMode,
        options = persistentMapOf(
            DarkPreference.ALWAYS to R.string.summary_night_mode_always,
            DarkPreference.DISABLED to R.string.summary_night_mode_disabled,
            DarkPreference.FOLLOW_SYSTEM to R.string.summary_night_mode_system
        )
    )
}

@Composable
fun PrefsScope<UISettings>.ForumListPreference(modifier: Modifier = Modifier) = Item { uiSettings ->
    SwitchPref(
        modifier = modifier,
        checked = uiSettings.homeForumList,
        onCheckedChange = {
            updatePreference { old -> old.copy(homeForumList = it) }
        },
        title = R.string.settings_forum_single,
        leadingIcon = Icons.Outlined.ViewAgenda
    )
}

@Composable
fun PrefsScope<UISettings>.ReduceEffectPreference(modifier: Modifier = Modifier) = Item { uiSettings ->
    SwitchPref(
        modifier = modifier,
        checked = uiSettings.reduceEffect,
        onCheckedChange = {
            updatePreference { old -> old.copy(reduceEffect = it) }
        },
        title = R.string.title_reduce_effect,
        leadingIcon = Icons.Outlined.BlurOn,
    )
}
