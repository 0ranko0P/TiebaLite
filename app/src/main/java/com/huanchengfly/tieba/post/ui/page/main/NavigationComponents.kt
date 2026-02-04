package com.huanchengfly.tieba.post.ui.page.main

import androidx.annotation.StringRes
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.LocalAccount

private val ActiveIndicatorHeight = 56.dp
private val ActiveIndicatorWidth = 240.dp

// androidx.compose.material3.tokens.NavigationBarTokens.ContainerHeight
val BottomNavigationHeight = 80.dp

@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = Color.Transparent,
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.primary.copy(0.25f),
    itemColor: Color = MaterialTheme.colorScheme.onSurface,
    selectedItemColor: Color = MaterialTheme.colorScheme.primary,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .height(ActiveIndicatorHeight)
            .fillMaxWidth(),
        shape = shape,
        color = if (selected) selectedBackgroundColor else backgroundColor,
        interactionSource = interactionSource,
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                val iconColor = if (selected) selectedItemColor else itemColor
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f)) {
                val labelColor = if (selected) selectedItemColor else itemColor
                CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
            }
            if (badge != null) {
                Spacer(Modifier.width(12.dp))
                val badgeColor = if (selected) selectedItemColor else itemColor
                CompositionLocalProvider(LocalContentColor provides badgeColor, content = badge)
            }
        }
    }
}

@Composable
fun NavigationDrawerContent(
    modifier: Modifier = Modifier,
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    navigationItems: List<NavigationItem>,
    navigationContentPosition: MainNavigationContentPosition,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
) {
    PermanentDrawerSheet(
        modifier = modifier.width(ActiveIndicatorWidth),
        drawerShape = drawerShape,
        drawerContainerColor = drawerContainerColor,
        drawerContentColor = drawerContentColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            val account = LocalAccount.current
            if (account != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    AccountNavIcon(size = Sizes.Large)
                    Text(
                        text = account.nickname ?: account.name,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                val context = LocalContext.current
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(data = R.drawable.ic_launcher_new_round, size = Sizes.Small)
                    Text(
                        text = remember { context.getString(R.string.app_name).uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (navigationContentPosition == MainNavigationContentPosition.CENTER) {
                Spacer(Modifier.weight(1.0f))
            }

            navigationItems.fastForEachIndexed { index, navigationItem ->
                NavigationDrawerItem(
                    selected = index == currentPosition,
                    onClick = {
                        if (index != currentPosition) {
                            onChangePosition(index)
                        }
                    },
                    label = { Text(text = stringResource(navigationItem.title)) },
                    icon = {
                        NavIcon(
                            modifier = Modifier.size(Sizes.Tiny),
                            item = navigationItem,
                            atEnd = index == currentPosition
                        )
                    }
                )
            }

            if (navigationContentPosition == MainNavigationContentPosition.CENTER) {
                Spacer(Modifier.weight(1.0f))
            }
        }
    }
}

@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    navigationItems: List<NavigationItem>,
    navigationContentPosition: MainNavigationContentPosition
) {
    NavigationRail(
        modifier = modifier,
        containerColor = TiebaLiteTheme.extendedColorScheme.navigationContainer,
        contentColor = TiebaLiteTheme.colorScheme.onSurface,
        header = {
            AccountNavIcon(modifier = Modifier.padding(top = 10.dp))
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = if (navigationContentPosition == MainNavigationContentPosition.TOP) Arrangement.Top else Arrangement.Center
        ) {
            navigationItems.fastForEachIndexed { index, navigationItem ->
                NavigationRailItem(
                    selected = index == currentPosition,
                    onClick = {
                        if (index != currentPosition) {
                            onChangePosition(index)
                        }
                    },
                    icon = {
                        NavIcon(
                            modifier = Modifier.size(16.dp),
                            item = navigationItem,
                            atEnd = index == currentPosition
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigation(
    modifier: Modifier = Modifier,
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    navigationItems: List<NavigationItem>,
    containerColor: Color = TiebaLiteTheme.extendedColorScheme.navigationContainer,
    contentColor: Color = TiebaLiteTheme.colorScheme.contentColorFor(containerColor),
) {
    NavigationBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        navigationItems.fastForEachIndexed { index, navigationItem ->
            NavigationBarItem(
                selected = index == currentPosition,
                onClick = {
                    if (index != currentPosition) {
                        onChangePosition(index)
                    }
                },
                icon = {
                    NavIcon(
                        modifier = Modifier.size(Sizes.Tiny),
                        item = navigationItem,
                        atEnd = index == currentPosition
                    )
                },
                alwaysShowLabel = false
            )
        }
    }
}

@Composable
private fun NavIcon(modifier: Modifier = Modifier, item: NavigationItem, atEnd: Boolean) {
    BadgedBox(
        modifier = modifier,
        badge = {
            val badgeText = item.badgeText() ?: return@BadgedBox
            Badge {
                Text(
                    text = badgeText,
                    // 6.sp ~ BadgeTokens.LargeLabelTextFont.fontSize
                    autoSize = TextAutoSize.StepBased(6.sp, LocalTextStyle.current.fontSize),
                    maxLines = 1
                )
            }
        },
    ) {
        Icon(
            painter = rememberAnimatedVectorPainter(animatedImageVector = item.icon(), atEnd = atEnd),
            contentDescription = null,
        )
    }
}

@Immutable
data class NavigationItem(
    @StringRes val title: Int,
    val icon: @Composable () -> AnimatedImageVector,
    val badgeText: () -> String? = { null },
)

@Preview("NavigationDrawerContent", device = Devices.PIXEL_TABLET)
@Composable
private fun NavigationDrawerContentPreview() = TiebaLiteTheme {
    val navItems = rememberNavigationItems()
    CompositionLocalProvider(LocalAccount provides null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavigationDrawerContent(
                currentPosition = 0,
                onChangePosition = {},
                navigationItems = navItems,
                navigationContentPosition = MainNavigationContentPosition.TOP
            )
            NavigationDrawerContent(
                currentPosition = 0,
                onChangePosition = {},
                navigationItems = navItems,
                navigationContentPosition = MainNavigationContentPosition.CENTER
            )
        }
    }
}