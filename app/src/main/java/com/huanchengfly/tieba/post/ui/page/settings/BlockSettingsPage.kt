package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.destinations.BlockListPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun BlockSettingsPage(navigator: DestinationsNavigator) {
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
            dataStore = LocalContext.current.dataStore,
            dividerThickness = 0.dp,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_block_list),
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Block,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = { navigator.navigate(BlockListPageDestination) }
                )
            }
            prefsItem {
                SwitchPref(
                    key = AppPreferencesUtils.KEY_POST_HIDE_BLOCKED,
                    title = R.string.settings_hide_blocked_content,
                    defaultChecked = false
                ) {
                    LeadingIcon {
                        AvatarIcon(
                            icon = Icons.Outlined.HideSource,
                            size = Sizes.Small,
                            contentDescription = null,
                        )
                    }
                }
            }
            prefsItem {
                SwitchPref(
                    key = AppPreferencesUtils.KEY_POST_BLOCK_VIDEO,
                    title = R.string.settings_block_video,
                    summary = { R.string.settings_block_video_summary },
                    defaultChecked = false,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.VideocamOff,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
        }
    }
}