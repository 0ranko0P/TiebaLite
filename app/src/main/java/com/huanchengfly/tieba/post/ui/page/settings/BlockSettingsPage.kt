package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SwitchPref
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TextPref
import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.BlockList
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar

@Composable
fun BlockSettingsPage(
    settings: Settings<BlockSettings>,
    navigator: NavController,
) {
    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_block_settings),
                navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
            )
        },
    ) { paddingValues ->
        PrefsScreen(
            settings = settings,
            initialValue = BlockSettings(),
            contentPadding = paddingValues
        ) {
            TextItem {
                TextPref(
                    title = stringResource(id = R.string.settings_block_user),
                    leadingIcon = Icons.Outlined.NoAccounts,
                    onClick = {
                        navigator.navigate(route = BlockList(isUser = true))
                    }
                )
            }

            TextItem {
                TextPref(
                    title = stringResource(id = R.string.settings_block_keyword),
                    leadingIcon = Icons.Outlined.Block,
                    onClick = {
                        navigator.navigate(route = BlockList(isUser = false))
                    }
                )
            }

            Item { block ->
                SwitchPref(
                    checked = block.hideBlocked,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(hideBlocked = it) }
                    },
                    title = R.string.settings_hide_blocked_content,
                    leadingIcon = Icons.Outlined.HideSource
                )
            }

            Item { block ->
                SwitchPref(
                    checked = block.blockVideo,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(blockVideo = it) }
                    },
                    title = R.string.settings_block_video,
                    summary = R.string.settings_block_video_summary,
                    leadingIcon = Icons.Outlined.VideocamOff
                )
            }
        }
    }
}