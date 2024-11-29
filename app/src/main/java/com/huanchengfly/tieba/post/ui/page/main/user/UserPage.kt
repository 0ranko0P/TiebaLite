package com.huanchengfly.tieba.post.ui.page.main.user

import android.graphics.Typeface
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.placeholder.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.setNightMode
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.BottomNavigationHeight
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.ListMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.Switch
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.launch

@Composable
private fun StatCardPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .placeholder(visible = true, color = ExtendedTheme.colors.chip)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatCardItem(
            statNum = 0,
            statText = stringResource(id = R.string.text_stat_follow)
        )
        HorizontalDivider(color = Color(if (ExtendedTheme.colors.isNightMode) 0xFF808080 else 0xFFDEDEDE))
        StatCardItem(
            statNum = 0,
            statText = stringResource(id = R.string.text_stat_fans)
        )
        HorizontalDivider(color = Color(if (ExtendedTheme.colors.isNightMode) 0xFF808080 else 0xFFDEDEDE))
        StatCardItem(
            statNum = 0,
            statText = stringResource(id = R.string.title_stat_posts_num)
        )
    }
}

@Composable
private fun StatCard(
    account: Account,
    modifier: Modifier = Modifier
) {
    val postNum by animateIntAsState(targetValue = account.postNum?.toIntOrNull() ?: 0)
    val fansNum by animateIntAsState(targetValue = account.fansNum?.toIntOrNull() ?: 0)
    val concernNum by animateIntAsState(targetValue = account.concernNum?.toIntOrNull() ?: 0)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatCardItem(
            statNum = concernNum,
            statText = stringResource(id = R.string.text_stat_follow)
        )
        HorizontalDivider(color = Color(if (ExtendedTheme.colors.isNightMode) 0xFF808080 else 0xFFDEDEDE))
        StatCardItem(
            statNum = fansNum,
            statText = stringResource(id = R.string.text_stat_fans)
        )
        HorizontalDivider(color = Color(if (ExtendedTheme.colors.isNightMode) 0xFF808080 else 0xFFDEDEDE))
        StatCardItem(
            statNum = postNum,
            statText = stringResource(id = R.string.title_stat_posts_num)
        )
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    userName: String = "",
    userIntro: String = "",
    avatar: String? = null,
    isPlaceholder: Boolean = false
) {
    Row(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Bottom)
        ) {
            Text(
                text = userName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ExtendedTheme.colors.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(visible = isPlaceholder, color = ExtendedTheme.colors.chip),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = userIntro,
                fontSize = 12.sp,
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(visible = isPlaceholder, color = ExtendedTheme.colors.chip),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        if (avatar != null) {
            Avatar(
                data = avatar,
                size = Sizes.Large,
                contentDescription = stringResource(id = R.string.desc_user_avatar),
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .placeholder(visible = isPlaceholder, color = ExtendedTheme.colors.chip),
            )
        }
    }
}

@Composable
private fun RowScope.StatCardItem(
    statNum: Int,
    statText: String
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$statNum",
            fontSize = 20.sp,
            fontFamily = FontFamily(Typeface.createFromAsset(LocalContext.current.assets, "bebas.ttf")),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = statText,
            fontSize = 12.sp,
            color = ExtendedTheme.colors.textSecondary
        )
    }
}

@Composable
private fun LoginTipCard(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Bottom)
        ) {
            Text(
                text = stringResource(id = R.string.tip_login),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ExtendedTheme.colors.text,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = null,
            tint = ExtendedTheme.colors.onChip,
            modifier = Modifier
                .clip(CircleShape)
                .size(Sizes.Large)
                .background(color = ExtendedTheme.colors.chip)
                .padding(16.dp),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserPage(viewModel: UserViewModel = viewModel()) {
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val isLoading by viewModel.isLoading
    val account = LocalAccount.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        backgroundColor = Color.Transparent,
        modifier = Modifier
            .systemBarsPadding()
            .padding(bottom = BottomNavigationHeight)
            .fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { contentPaddings ->
        val pullRefreshState = rememberPullRefreshState(isLoading, viewModel::onRefresh)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPaddings)
                .pullRefresh(pullRefreshState),
        ) {
            Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
                Spacer(modifier = Modifier.height(8.dp))
                if (account != null) {
                    InfoCard(
                        modifier = Modifier
                            .clickable {
                                navigator.navigate(Destination.UserProfile(account.uid.toLong()))
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        userName = account.nameShow ?: account.name,
                        userIntro = account.intro ?: stringResource(id = R.string.tip_no_intro),
                        avatar = StringUtil.getAvatarUrl(account.portrait),
                    )
                    StatCard(
                        account = account,
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = ExtendedTheme.colors.chip)
                            .padding(vertical = 18.dp)
                    )
                } else if (isLoading) {
                    InfoCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        isPlaceholder = true,
                    )
                    StatCardPlaceholder(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = ExtendedTheme.colors.chip)
                            .padding(vertical = 18.dp)
                    )
                } else {
                    LoginTipCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
                }
                if (account != null) {
                    ListMenuItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_favorite),
                        text = stringResource(id = R.string.title_my_collect),
                        onClick = {
                            navigator.navigate(Destination.ThreadStore)
                        }
                    )
                }
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_outline_watch_later_24),
                    text = stringResource(id = R.string.title_history),
                    onClick = {
                        navigator.navigate(Destination.History)
                    }
                )
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_brush_24),
                    text = stringResource(id = R.string.title_theme),
                    onClick = {
                        navigator.navigate(Destination.AppTheme)
                    }
                ) {
                    Text(text = stringResource(id = R.string.my_info_night), fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = ExtendedTheme.colors.isNightMode,
                        onCheckedChange = { checked ->
                            // Override night mode temporary
                            context.setNightMode(checked)
                            // Show night mode settings tip
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    context.getString(R.string.message_find_tip),
                                    actionLabel = context.getString(R.string.title_settings_night_mode)
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    navigator.navigate(SettingsDestination.Custom)
                                }
                            }
                        }
                    )
                }
                if (account != null) {
                    ListMenuItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_help_outline_black_24),
                        text = stringResource(id = R.string.my_info_service_center),
                        onClick = {
                            navigator.navigate(
                                Destination.WebView(
                                    initialUrl = "https://tieba.baidu.com/mo/q/hybrid-main-service/uegServiceCenter?cuid=${CuidUtils.getNewCuid()}&cuid_galaxy2=${CuidUtils.getNewCuid()}&cuid_gid=&timestamp=${System.currentTimeMillis()}&_client_version=12.52.1.0&nohead=1"
                                )
                            )
                        },
                    )
                }
                VerticalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_settings_24),
                    text = stringResource(id = R.string.my_info_settings),
                    onClick = { navigator.navigate(SettingsDestination.Settings) },
                )
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_info_black_24),
                    text = stringResource(id = R.string.my_info_about),
                    onClick = { navigator.navigate(SettingsDestination.About) },
                )
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                contentColor = ExtendedTheme.colors.primary,
            )
        }
    }
}
