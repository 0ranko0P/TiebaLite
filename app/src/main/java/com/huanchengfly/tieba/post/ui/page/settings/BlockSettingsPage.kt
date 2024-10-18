package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.BlockList
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils

@Composable
fun BlockSettingsPage(navigator: NavController) {
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_block_settings),
                navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
            )
        },
    ) { paddingValues ->
        PrefsScreen(
            contentPadding = paddingValues
        ) {
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_block_list),
                    leadingIcon = Icons.Outlined.Block,
                    onClick = {
                        navigator.navigate(BlockList)
                    }
                )
            }
            prefsItem {
                SwitchPref(
                    key = AppPreferencesUtils.KEY_POST_HIDE_BLOCKED,
                    title = R.string.settings_hide_blocked_content,
                    defaultChecked = false,
                    leadingIcon = Icons.Outlined.HideSource
                )
            }
            prefsItem {
                SwitchPref(
                    key = AppPreferencesUtils.KEY_POST_BLOCK_VIDEO,
                    title = R.string.settings_block_video,
                    summary = { R.string.settings_block_video_summary },
                    defaultChecked = false,
                    leadingIcon = Icons.Outlined.VideocamOff
                )
            }
        }
    }
}