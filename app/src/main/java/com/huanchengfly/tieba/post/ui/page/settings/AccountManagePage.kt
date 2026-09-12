package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SupervisedUserCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.page.Destination.Login
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.preference
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.StringUtil.normalized
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.launchUrl

@Composable
fun AccountManagePage(
    myLittleTailSettings: Settings<String>,
    navigator: NavController,
    viewModel: AccountManageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val account = viewModel.currentAccount.collectAsStateWithLifecycle().value ?: return
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val accountName = account.nickname ?: account.name

    SettingsScaffold(
        titleRes = R.string.title_account_manage,
        onBack = navigator::navigateUp
    ) {
        group(verticalPadding = 16.dp) {
            if (accounts.size > 1) {
                listPref(
                    value = account.uid,
                    title = context.getString(R.string.title_switch_account),
                    summary = context.getString(R.string.summary_now_account, accountName),
                    leadingIcon = Icons.Outlined.AccountCircle,
                    onValueChange = viewModel::switchAccount,
                    options = accounts,
                )
            } else {
                preference(
                    title = accountName,
                    icon = Icons.Outlined.AccountCircle,
                )
            }

            preference(
                title = R.string.title_new_account,
                onClick = { navigator.navigate(Login) },
                leadingIcon = Icons.Outlined.AddCircleOutline,
            )

            customPreference { shapes ->
                val logoutDialogState = rememberDialogState()
                SegmentedPreference(
                    title = R.string.title_exit_account,
                    shapes = shapes,
                    onClick = logoutDialogState::show,
                    leadingIcon = Icons.AutoMirrored.Outlined.Logout
                )

                ConfirmDialog(
                    title = { Text(text = stringResource(R.string.title_exit_account)) },
                    dialogState = logoutDialogState,
                    onConfirm = {
                        if (accounts.size == 1) {
                            navigator.navigateUp()
                        }
                        viewModel.logout()
                    }
                )
            }
        }

        group(title = R.string.settings_group_account_expire, titleVerticalPadding = Dp.Hairline) {
            customPreference {
                Text(
                    text = stringResource(id = R.string.tip_account_error),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        group(title = R.string.settings_group_account_edit) {
            preference(
                title = R.string.title_modify_username,
                onClick = {
                    val url = "https://wappass.baidu.com/static/manage-chunk/change-username.html#/showUsername"
                    launchUrl(context, navigator, url)
                },
                leadingIcon = Icons.Outlined.SupervisedUserCircle,
            )

            preference(
                title = R.string.title_copy_bduss,
                summary = R.string.summary_copy_bduss,
                onClick = { TiebaUtil.copyText(context, account.bduss, isSensitive = true) },
                leadingIcon = Icons.Outlined.ContentCopy,
            )

            customPreference(key = R.string.title_my_tail) { shapes ->
                val dialogState = rememberDialogState()
                val myLittleTail by myLittleTailSettings.collectAsStateWithLifecycle(initialValue = "")

                SegmentedPreference(
                    title = stringResource(id = R.string.title_my_tail),
                    summary = myLittleTail.ifEmpty { stringResource(R.string.tip_no_little_tail) },
                    shapes = shapes,
                    leadingIcon = Icons.Outlined.Edit,
                    onClick = dialogState::show
                )

                if (dialogState.show) {
                    PromptDialog(
                        onConfirm = {
                            val newValue = it.normalized().trim()
                            if (newValue != myLittleTail) {
                                myLittleTailSettings.set(newValue)
                            }
                        },
                        dialogState = dialogState,
                        initialValue = myLittleTail,
                        title = { Text(text = stringResource(id = R.string.title_dialog_modify_little_tail)) },
                    )
                }
            }
        }
    }
}