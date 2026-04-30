package com.huanchengfly.tieba.post.ui.page.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.theme.BlueGrey700
import com.huanchengfly.tieba.post.theme.Cyan700
import com.huanchengfly.tieba.post.theme.Green700
import com.huanchengfly.tieba.post.theme.Purple700
import com.huanchengfly.tieba.post.theme.Red700
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
import com.huanchengfly.tieba.post.ui.page.Destination.Login
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.About
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.AccountManage
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CollapsingAvatarTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PreferenceItemPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedTextPrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SettingsSegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil

private fun SegmentedPrefsScope.accountPreference(
    account: Account?,
    onManageAccountClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    iconTint: Color,
) {
    if (account != null) {
        preference(
            onClick = onManageAccountClicked,
            title = { Text(text = stringResource(R.string.title_account_manage)) },
            summary = {
                val name = account.nickname ?: account.name
                Text(text = stringResource(R.string.summary_now_account, name))
            },
            icon = {
                Avatar(
                    data = remember { StringUtil.getAvatarUrl(account.portrait) },
                    modifier = Modifier.size(SettingsLeadingIconSize)
                )
            }
        )
    } else {
        mainPreference(
            title = R.string.title_account_manage,
            summary = R.string.summary_not_logged_in,
            icon = Icons.Rounded.AccountCircle,
            iconContainer = iconTint,
            onClick = onLoginClicked,
        )
    }
}

@Composable
fun SettingsPage(navigator: NavController) {
    val account = LocalAccount.current

    SettingsScaffold(
        titleRes = R.string.title_settings,
        titleHorizontalAlignment = Alignment.CenterHorizontally,
        onBack = navigator::navigateUp,
    ) {
        group(verticalPadding = SettingsGroupVerticalPadding) {
            accountPreference(
                account = account,
                onManageAccountClicked = {
                    navigator.navigateDebounced(route = AccountManage)
                },
                onLoginClicked = { navigator.navigateDebounced(route = Login) },
                iconTint = Purple700,
            )

            mainPreference(
                title = R.string.title_oksign,
                summary = R.string.summary_settings_oksign,
                icon = Icons.Rounded.Checklist,
                iconContainer = Purple700,
                enabled = account != null
            ) {
                navigator.navigateDebounced(SettingsDestination.OKSign)
            }
        }

        group(verticalPadding = SettingsGroupVerticalPadding) {
            mainPreference(
                title = R.string.title_block_settings,
                summary = R.string.summary_block_settings,
                icon = Icons.Rounded.DoNotDisturbOff,
                iconContainer = Red700,
            ) {
                navigator.navigateDebounced(SettingsDestination.BlockSettings)
            }
        }

        group(verticalPadding = SettingsGroupVerticalPadding) {
            mainPreference(
                title = R.string.title_settings_custom,
                summary = R.string.summary_settings_custom,
                icon = Icons.Outlined.FormatPaint,
                iconContainer = Green700,
            ) {
                navigator.navigateDebounced(SettingsDestination.UI)
            }

            mainPreference(
                title = R.string.title_settings_read_habit,
                summary = R.string.summary_settings_habit,
                icon = Icons.Outlined.DashboardCustomize,
                iconContainer = Green700,
            ) {
                navigator.navigateDebounced(SettingsDestination.Habit)
            }
        }

        group(verticalPadding = SettingsGroupVerticalPadding) {
            mainPreference(
                title = R.string.title_settings_privacy,
                summary = R.string.summary_settings_privacy,
                icon = Icons.Outlined.Shield,
                iconContainer = Cyan700,
            ) {
                navigator.navigateDebounced(SettingsDestination.Privacy)
            }
        }

        group(verticalPadding = SettingsGroupVerticalPadding) {
            mainPreference(
                title = R.string.title_settings_more,
                summary = R.string.summary_settings_more,
                icon =  Icons.Rounded.MoreHoriz,
                iconContainer = BlueGrey700
            ) {
                navigator.navigateDebounced(SettingsDestination.More)
            }

            mainPreference(
                title = R.string.title_about,
                summary = R.string.summary_settings_about,
                icon = Icons.Outlined.Info,
                iconContainer = BlueGrey700
            ) {
                navigator.navigate(About)
            }
        }
    }
}

/** The default expanded height of a [SettingsTopAppBar] */
private val SettingsAppbarExpandHeight: Dp
    @Composable @ReadOnlyComposable get() = with(LocalWindowAdaptiveInfo.current.windowSizeClass) {
        when {
            isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> TopAppBarDefaults.LargeAppBarExpandedHeight

            // isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) ->

            else -> TopAppBarDefaults.MediumAppBarExpandedHeight
        }
    }

/** Extra padding to be applied to the [SegmentedPrefsScreen] */
private val SettingsContentPadding: PaddingValues = PaddingValues(16.dp)

private val SettingsGroupVerticalPadding: Dp = 6.dp

private val SettingsLeadingIconSize: Dp = 40.dp

private fun SegmentedPrefsScope.mainPreference(
    @StringRes title: Int,
    @StringRes summary: Int,
    icon: ImageVector,
    iconContainer: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    customPreference(key = title) { shapes ->
        val iconColor = if (enabled) iconContainer else iconContainer.copy(0.38f) // ListTokens.ItemDisabledLeadingIconOpacity
        SegmentedPreference(
            title = { Text(text = stringResource(id = title)) },
            summary = {
                Text(text = stringResource(id = summary))
            },
            contentPadding = PreferenceItemPadding,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(SettingsLeadingIconSize)
                        .background(color = iconColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val iconTint = if (enabled) Color.White else LocalContentColor.current
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                }
            },
            shapes = shapes,
            enabled = enabled,
            onClick = onClick,
        )
    }
}

@Composable
fun SettingsTopAppBar(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(0.01f) // Nearly transparent
    )
    if (isWindowHeightCompact()) {
        TopAppBar(
            modifier = modifier,
            title = { Text(text = stringResource(id = titleRes)) },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
        )
    } else {
        CollapsingAvatarTopAppBar(
            modifier = modifier,
            avatar = null,
            title = {
                Text(
                    text = stringResource(id = titleRes),
                    modifier = Modifier.padding(start = 4.dp),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            titleHorizontalAlignment = titleHorizontalAlignment,
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = SettingsAppbarExpandHeight,
            scrollBehavior = scrollBehavior,
            colors = colors,
        )
    }
}

@Composable
fun <T> SettingsScaffold(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onBack: () -> Unit,
    settings: Settings<T>,
    initialValue: T,
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    snackbarHost: @Composable () -> Unit = { SwipeToDismissSnackbarHost(LocalSnackbarHostState.current) },
    content: SettingsSegmentedPrefsScope<T>.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MyScaffold(
        modifier = modifier,
        topBar = {
            SettingsTopAppBar(
                titleRes = titleRes,
                titleHorizontalAlignment = titleHorizontalAlignment,
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHostState = snackbarHostState,
        snackbarHost = snackbarHost,
    ) { contentPadding ->
        SegmentedPrefsScreen(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            settings = settings,
            initialValue = initialValue,
            contentPadding = contentPadding + SettingsContentPadding,
            content = content
        )
    }
}

@Composable
fun SettingsScaffold(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    snackbarHost: @Composable () -> Unit = { SwipeToDismissSnackbarHost(LocalSnackbarHostState.current) },
    content: SegmentedPrefsScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MyScaffold(
        modifier = modifier,
        topBar = {
            SettingsTopAppBar(
                titleRes = titleRes,
                titleHorizontalAlignment = titleHorizontalAlignment,
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHostState = snackbarHostState,
        snackbarHost = snackbarHost,
    ) { contentPadding ->
        SegmentedTextPrefsScreen(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = contentPadding + SettingsContentPadding,
            content = content
        )
    }
}