package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SupervisedUserCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.DropDownPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.EditTextPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.Destination.Login
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_LITTLE_TAIL
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.LocalAllAccounts
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.launchUrl
import kotlinx.collections.immutable.toImmutableMap

@Composable
fun AccountManagePage(navigator: NavController) {
    Scaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_account_manage),
                navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
            )
        },
    ) { paddingValues ->
        val account = LocalAccount.current
        val context = LocalContext.current
        val accountUtil = remember { AccountUtil.getInstance() }

        PrefsScreen(contentPadding = paddingValues) {
            prefsItem {
                val accounts = LocalAllAccounts.current
                val accountsMap = remember(accounts) {
                    accounts.associate { it.id to (it.nameShow ?: it.name) }
                        .toImmutableMap()
                }
                if (account != null) {
                    DropDownPref(
                        key = null,
                        title = stringResource(id = R.string.title_switch_account),
                        summary = stringResource(
                            id = R.string.summary_now_account,
                            account.nameShow ?: account.name
                        ),
                        leadingIcon =  Icons.Outlined.AccountCircle,
                        onValueChange = accountUtil::switchAccount,
                        defaultValue = account.id,
                        options = accountsMap
                    )
                } else {
                    TextPref(
                        title = stringResource(id = R.string.title_switch_account),
                        summary = null,
                        leadingIcon = Icons.Outlined.AccountCircle,
                    )
                }
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_new_account),
                    onClick = { navigator.navigate(Login) },
                    leadingIcon = Icons.Outlined.AddCircleOutline,
                )
            }
            prefsItem {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(stringResource(id = R.string.tip_start))
                        }
                        append(stringResource(id = R.string.tip_account_error))
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color = ExtendedTheme.colors.chip)
                        .padding(12.dp),
                    color = ExtendedTheme.colors.onChip,
                    fontSize = 12.sp
                )
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_exit_account),
                    onClick = {
                        if (accountUtil.allAccounts.size <= 1) navigator.navigateUp()
                        accountUtil.exit(context)
                    },
                    leadingIcon = Icons.AutoMirrored.Outlined.Logout
                )
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_modify_username),
                    onClick = {
                        launchUrl(
                            context,
                            navigator,
                            "https://wappass.baidu.com/static/manage-chunk/change-username.html#/showUsername"
                        )
                    },
                    leadingIcon = Icons.Outlined.SupervisedUserCircle,
                )
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_copy_bduss),
                    summary = stringResource(id = R.string.summary_copy_bduss),
                    onClick = { TiebaUtil.copyText(context, account?.bduss, isSensitive = true) },
                    leadingIcon = Icons.Outlined.ContentCopy,
                )
            }
            prefsItem {
                val littleTail by rememberPreferenceAsState(
                    key = stringPreferencesKey(KEY_LITTLE_TAIL),
                    defaultValue = ""
                )

                EditTextPref(
                    key = KEY_LITTLE_TAIL,
                    title = stringResource(id = R.string.title_my_tail),
                    summary = littleTail.ifEmpty { stringResource(id = R.string.tip_no_little_tail) },
                    leadingIcon = Icons.Outlined.Edit,
                    enabled = true,
                    dialogTitle = stringResource(id = R.string.title_dialog_modify_little_tail),
                )
            }
        }
    }
}