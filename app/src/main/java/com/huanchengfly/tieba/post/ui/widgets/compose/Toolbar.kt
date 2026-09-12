package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.utils.calculateNavigationType
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil

val AppBarHeight: Dp = 56.dp

@Composable
fun AccountNavIconIfCompact(onLoginClicked: () -> Unit) {
    if (calculateNavigationType(LocalWindowAdaptiveInfo.current).isNavigationBar) {
        AccountNavIcon(
            onLoginClicked = onLoginClicked,
            modifier = Modifier.padding(start = 12.dp),
            size = 32.dp
        )
    }
}

val MoreMenuItem: @Composable () -> Unit = {
    val contentDescription = stringResource(id = R.string.btn_more)
    PlainTooltipBox(
        contentDescription = contentDescription,
        hasAction = true,
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = contentDescription,
            modifier = Modifier.minimumInteractiveComponentSize(),
        )
    }
}

@Composable
fun AccountNavIcon(
    onLoginClicked: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.Small
) = Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
) {
    val currentAccount = LocalAccount.current

    if (currentAccount == null) {
        Avatar(data = R.drawable.ic_launcher_new_round, size = size)
    } else {
        Avatar(
            data = StringUtil.getAvatarUrl(currentAccount.portrait),
            size = size,
            contentDescription = stringResource(id = R.string.title_switch_account_long_press)
        )
    }
}

@Composable
fun ActionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    positionProvider: PopupPositionProvider = rememberTooltipPositionProvider(
        positioning = TooltipAnchorPosition.Above,
        spacingBetweenTooltipAndAnchor = SpacingBetweenTooltipAndAnchor
    ),
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    onClick: () -> Unit
) {
    PlainTooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        contentDescription = contentDescription,
        hasAction = true,
    ) {
        IconButton(onClick = onClick, enabled = enabled, colors = colors) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@NonRestartableComposable
@Composable
fun ActionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    @StringRes contentDescription: Int,
    positionProvider: PopupPositionProvider = rememberTooltipPositionProvider(
        positioning = TooltipAnchorPosition.Above,
        spacingBetweenTooltipAndAnchor = SpacingBetweenTooltipAndAnchor
    ),
    enabled: Boolean = true,
    onClick: () -> Unit
) = ActionItem(
    modifier = modifier,
    icon = icon,
    contentDescription = LocalContext.current.getString(contentDescription),
    positionProvider = positionProvider,
    enabled = enabled,
    onClick = onClick
)

@Composable
fun BackNavigationIcon(modifier: Modifier = Modifier, onBackPressed: () -> Unit) {
    IconButton(onClick = onBackPressed, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(id = R.string.button_back)
        )
    }
}

@NonRestartableComposable
@Composable
fun TitleCentredToolbar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) =
    TitleCentredToolbar(
        title = { Text(text = title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )

@Composable
fun TitleCentredToolbar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = TiebaLiteTheme.extendedColorScheme.appBarColors,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
        modifier = modifier,
        topBar = {
            Box(contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navigationIcon?.invoke()

                    Spacer(modifier = Modifier.weight(1f))

                    actions()
                }

                ProvideTextStyle(MaterialTheme.typography.titleLarge, content = title)
            }
        },
        colors = colors,
        content = content
    )
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TiebaLiteTheme.extendedColorScheme.appBarColors,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    navigationIcon?.invoke()
                }

                Box(modifier = Modifier.weight(1.0f)) {
                    ProvideTextStyle(MaterialTheme.typography.titleLarge, content = title)
                }

                actions()
            }
        },
        colors = colors,
        content = content
    )
}

@Composable
fun TopAppBarContainer(
    modifier: Modifier = Modifier,
    topBar: @Composable BoxScope.() -> Unit,
    colors: TopAppBarColors = TiebaLiteTheme.extendedColorScheme.appBarColors,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.containerColor,
        contentColor = colors.titleContentColor,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = TopAppBarHorizontalPadding)
                    .height(AppBarHeight),
                content = topBar
            )

            content?.invoke(this)
        }
    }
}