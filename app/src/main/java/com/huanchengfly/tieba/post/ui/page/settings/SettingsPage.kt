package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TextPrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TextPref
import com.huanchengfly.tieba.post.ui.page.Destination.Login
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.AccountManage
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil

@Composable
private fun NowAccountItem(modifier: Modifier = Modifier, account: Account?) {
    val navigator = LocalNavController.current
    if (account != null) {
        TextPref(
            modifier = modifier,
            title = stringResource(id = R.string.title_account_manage),
            summary = stringResource(id = R.string.summary_now_account, account.nickname ?: account.name),
            onClick = {
                navigator.navigate(route = AccountManage)
            },
            leadingContent = {
                Avatar(
                    data = remember { StringUtil.getAvatarUrl(account.portrait) },
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
                navigator.navigate(route = Login)
            },
            leadingIcon = Icons.Rounded.AccountCircle
        )
    }
}

@Composable
fun SettingsPage(navigator: NavController) {
    ProvideNavigator(navigator = navigator) {
        Scaffold(
            topBar = {
                TitleCentredToolbar(
                    title = stringResource(id = R.string.title_settings),
                    navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { paddingValues ->
            TextPrefsScreen(contentPadding = paddingValues) {
                NowAccountItem(account = LocalAccount.current)

                TextPref(
                    title = stringResource(id = R.string.title_block_settings),
                    summary = stringResource(id = R.string.summary_block_settings),
                    onClick = {
                        navigator.navigate(SettingsDestination.BlockSettings)
                    },
                    leadingIcon = Icons.Rounded.DoNotDisturbOff
                )

                TextPref(
                    title = stringResource(id = R.string.title_settings_custom),
                    summary = stringResource(id = R.string.summary_settings_custom),
                    onClick = {
                        navigator.navigate(SettingsDestination.UI)
                    },
                    leadingIcon = Icons.Outlined.FormatPaint
                )

                TextPref(
                    title = stringResource(id = R.string.title_settings_read_habit),
                    summary = stringResource(id = R.string.summary_settings_habit),
                    onClick = {
                        navigator.navigate(SettingsDestination.Habit)
                    },
                    leadingIcon = Icons.Outlined.DashboardCustomize
                )

                TextPref(
                    title = stringResource(id = R.string.title_settings_privacy),
                    summary = stringResource(id = R.string.summary_settings_privacy),
                    onClick = {
                        navigator.navigate(SettingsDestination.Privacy)
                    },
                    leadingIcon = Icons.Outlined.Shield
                )

                TextPref(
                    title = stringResource(id = R.string.title_oksign),
                    summary = stringResource(id = R.string.summary_settings_oksign),
                    onClick = {
                        navigator.navigate(SettingsDestination.OKSign)
                    },
                    leadingIcon = Icons.Rounded.Checklist,
                    enabled = LocalAccount.current != null
                )

                TextPref(
                    title = stringResource(id = R.string.title_settings_more),
                    summary = stringResource(id = R.string.summary_settings_more),
                    onClick = {
                        navigator.navigate(SettingsDestination.More)
                    },
                    leadingIcon =  Icons.Rounded.MoreHoriz
                )
            }
        }
    }
}