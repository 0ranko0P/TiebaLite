package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.isDarkScheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.common.theme.compose.BebasFamily
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.emptyBlurBottomNavigation
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ListMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.coroutines.launch

@Composable
private fun StatCardPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(76.dp)
            .fillMaxWidth()
            .placeholder()
    )
}

/**
 * User status card
 * */
@Composable
fun StatCard(posts: String?, fans: String?, concerned: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(vertical = 16.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatCardItem(title = stringResource(R.string.title_stat_posts_num), stat = posts)
        VerticalDivider(color = MaterialTheme.colorScheme.outline)

        StatCardItem(title = stringResource(R.string.text_stat_fans), stat = fans)
        VerticalDivider(color = MaterialTheme.colorScheme.outline)

        StatCardItem(title = stringResource(R.string.text_stat_follow), stat = concerned)
    }
}

@Composable
private fun InfoCardPlaceHolder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "",
                fontSize = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(),
            )
            Text(
                text = "",
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(Sizes.Large)
                .placeholder(shape = CircleShape),
        )
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    userName: String = "",
    userIntro: String? = null,
    avatar: String? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = userName, style = MaterialTheme.typography.titleLarge)

            Text(
                text = userIntro ?: stringResource(id = R.string.tip_no_intro),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (avatar != null) {
            Spacer(modifier = Modifier.width(16.dp))
            Avatar(
                data = avatar,
                size = Sizes.Large,
                contentDescription = stringResource(id = R.string.desc_user_avatar),
            )
        }
    }
}

@Composable
private fun RowScope.StatCardItem(title: String, stat: String?) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stat ?: 0.toString(), fontSize = 20.sp, fontFamily = BebasFamily)

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = title,
            color = LocalContentColor.current.copy(0.84f),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun LoginTipCard(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Bottom),
            text = stringResource(id = R.string.tip_login),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .size(Sizes.Large)
                .background(color = MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                .padding(16.dp),
        )
    }
}

@Composable
fun UserPage(viewModel: UserViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navigator = LocalNavController.current
    val colorScheme = MaterialTheme.colorScheme
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    MyScaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = emptyBlurBottomNavigation, // MainPage workaround
    ) { contentPaddings ->
        val account = LocalAccount.current
        val snackbarHostState = LocalSnackbarHostState.current

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPaddings,
        ) {
            Column(
                modifier = Modifier
                    .padding(contentPaddings)
                    .verticalScroll(state = rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                if (account != null) {
                    InfoCard(
                        modifier = Modifier
                            .clickable {
                                navigator.navigate(Destination.UserProfile(uid = account.uid))
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        userName = account.nickname ?: account.name,
                        userIntro = account.intro?.takeUnless { it.isEmpty() },
                        avatar = remember { StringUtil.getAvatarUrl(account.portrait) },
                    )

                    Surface(
                        modifier = Modifier.padding(16.dp),
                        shape = MaterialTheme.shapes.small,
                        color = colorScheme.secondaryContainer,
                    ) {
                        StatCard(account.posts, account.fans, account.concerned)
                    }
                } else if (isLoading) {
                    InfoCardPlaceHolder(modifier = Modifier.padding(16.dp))
                    StatCardPlaceholder(modifier = Modifier.padding(16.dp))
                } else {
                    LoginTipCard(modifier = Modifier.padding(16.dp))
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
                    onClick = { navigator.navigate(Destination.AppTheme) }
                ) {
                    if (colorScheme.isTranslucent) return@ListMenuItem // Translucent theme has no dark/light mode
                    val isDarkMode = colorScheme.isDarkScheme

                    val switchEnabled by remember {
                        derivedStateOf { snackbarHostState.currentSnackbarData == null }
                    }

                    Text(text = stringResource(id = R.string.my_info_night), fontSize = 12.sp)

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { checked ->
                            // Override night mode temporary
                            ThemeUtil.overrideDarkMode(darkMode = checked)
                            // Show night mode settings tip
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.message_find_tip),
                                    actionLabel = context.getString(R.string.title_settings_night_mode),
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    navigator.navigate(route = SettingsDestination.UI)
                                }
                            }
                        },
                        thumbContent = {
                            if (isDarkMode) {
                                Icon(Icons.Filled.DarkMode, contentDescription = null)
                            }
                        },
                        enabled = switchEnabled
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
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_settings_24),
                    text = stringResource(id = R.string.title_settings),
                    onClick = { navigator.navigate(SettingsDestination.Settings) },
                )
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_info_black_24),
                    text = stringResource(id = R.string.my_info_about),
                    onClick = { navigator.navigate(SettingsDestination.About) },
                )
            }
        }
    }
}
