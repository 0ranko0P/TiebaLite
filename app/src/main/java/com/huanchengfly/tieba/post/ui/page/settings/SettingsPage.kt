package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AccountManagePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.CustomSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HabitSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.MoreSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.OKSignSettingsPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
private fun NowAccountItem(modifier: Modifier = Modifier, account: Account?) {
    val navigator = LocalNavigator.current
    if (account != null) {
        TextPref(
            modifier = modifier,
            title = stringResource(id = R.string.title_account_manage),
            summary = stringResource(id = R.string.summary_now_account, account.nameShow ?: account.name),
            onClick = {navigator.navigate(AccountManagePageDestination) },
            leadingIcon = {
                Avatar(
                    data = StringUtil.getAvatarUrl(account.portrait),
                    contentDescription = stringResource(id = R.string.title_account_manage),
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    } else {
        TextPref(
            modifier = modifier,
            title = stringResource(id = R.string.title_account_manage),
            summary = stringResource(id = R.string.summary_not_logged_in),
            onClick = {
                navigator.navigate(LoginPageDestination)
            },
            leadingIcon = Icons.Rounded.AccountCircle
        )
    }
}

@Destination
@Composable
fun SettingsPage(
    navigator: DestinationsNavigator,
) {
    ProvideNavigator(navigator = navigator) {
        Scaffold(
            backgroundColor = Color.Transparent,
            topBar = {
                TitleCentredToolbar(
                    title = stringResource(id = R.string.title_settings),
                    navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
                )
            }
        ) { paddingValues ->
            PrefsScreen(contentPadding = paddingValues) {
                prefsItem {
                    NowAccountItem(account = LocalAccount.current)
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_block_settings),
                        summary = stringResource(id = R.string.summary_block_settings),
                        onClick = {
                            navigator.navigate(BlockSettingsPageDestination)
                        },
                        leadingIcon = Icons.Rounded.DoNotDisturbOff
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_custom),
                        summary = stringResource(id = R.string.summary_settings_custom),
                        onClick = {
                            navigator.navigate(CustomSettingsPageDestination)
                        },
                        leadingIcon = Icons.Outlined.FormatPaint
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_read_habit),
                        summary = stringResource(id = R.string.summary_settings_habit),
                        onClick = {
                            navigator.navigate(HabitSettingsPageDestination)
                        },
                        leadingIcon = Icons.Outlined.DashboardCustomize
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_oksign),
                        summary = stringResource(id = R.string.summary_settings_oksign),
                        onClick = {
                            navigator.navigate(OKSignSettingsPageDestination)
                        },
                        leadingIcon = Icons.Rounded.Checklist
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_more),
                        summary = stringResource(id = R.string.summary_settings_more),
                        onClick = {
                            navigator.navigate(MoreSettingsPageDestination)
                        },
                        leadingIcon =  Icons.Rounded.MoreHoriz
                    )
                }
            }
        }
    }
}