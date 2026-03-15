package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.ShortNavigationBarOverride
import androidx.compose.material3.ShortNavigationBarOverrideScope
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.HorizontalRuler
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.main.FloatingIconNavigationBarOverride.ShortNavigationBar
import com.huanchengfly.tieba.post.ui.page.main.FloatingNavigationBarOverride.ShortNavigationBar
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.NavigationBarHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.LocalAccount

val FloatingNavigationBarScreenOffset = FloatingToolbarDefaults.ScreenOffset / 2

private val FloatingNavigationBarElevation: Dp = 1.dp

private val FloatingIconNavigationBarHeight = 56.dp

val ColorScheme.vibrantFloatingNavigationBarColor: Color
    get() = surfaceColorAtElevation(4.dp)

val ColorScheme.vibrantFloatingNavigationBarContentColor: Color
    get() = onSurface

/**
 * Drawer primary action used in [NavigationSuite]
 * */
@Composable
fun TbDrawerNavigationAction(
    onLoginClicked: () -> Unit,
    modifier: Modifier = Modifier,
    account: Account? = LocalAccount.current,
) {
    if (account != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.padding(all = 16.dp)
        ) {
            AccountNavIcon(onLoginClicked = onLoginClicked, size = Sizes.Large)
            Text(
                text = account.nickname ?: account.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
        }
    } else {
        val context = LocalContext.current
        Row(
            modifier = modifier.padding(16.dp),
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
}

@NonRestartableComposable
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    androidx.compose.material3.NavigationDrawerItem(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        badge = badge,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
    )
}

/**
 * Icon only short navigation bar item.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services
 * @param modifier the [Modifier] to be applied to this item
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this item. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this item in different states
 */
@Composable
internal fun IconNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    colors: NavigationItemColors,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val iconColor = colors.iconColor(selected = selected, enabled = enabled)
    Box(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .size(LocalMinimumInteractiveComponentSize.current),
    ) {
        val indicatorAnimationProgress = animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .badgeBounds(),
            contentAlignment = Alignment.Center,
            content = {
                // Create the indicator ripple.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .indication(interactionSource, ripple())
                )
                // Create the indicator. The indicator has a expansion animation which interferes
                // with the timing of the ripple, which is why they are separate composables.
                Indicator(colors.selectedIndicatorColor) {
                    indicatorAnimationProgress.value.coerceAtLeast(0f)
                }

                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
            },
        )
    }
}

@Composable
private fun BoxScope.Indicator(
    indicatorColor: Color,
    indicatorAnimationProgress: () -> Float,
) {
    Box(
        Modifier
            .matchParentSize()
            .drawBehind {
                drawCircle(indicatorColor, size.minDimension / 2 * indicatorAnimationProgress())
            }
    )
}

context(scope: ShortNavigationBarOverrideScope)
private fun Modifier.floatingNavBarContainer(height: Dp): Modifier = this then with(scope) {
    Modifier
        .windowInsetsPadding(windowInsets)
        .heightIn(min = height)
        .clickableNoIndication(onClick = {})
        .selectableGroup()
        .graphicsLayer {
            this.shape = CircleShape
            this.clip = true
            this.translationY = -FloatingNavigationBarScreenOffset.toPx()
            this.shadowElevation = FloatingNavigationBarElevation.toPx()
        }
        .then(modifier)
        .background(containerColor)
}

/**
 * This override provides the default behavior of the [ShortNavigationBar] component.
 */
@ExperimentalMaterial3ComponentOverrideApi
object FloatingNavigationBarOverride : ShortNavigationBarOverride {
    @Composable
    override fun ShortNavigationBarOverrideScope.ShortNavigationBar() {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .floatingNavBarContainer(NavigationBarHeight)
                    .padding(start = 24.dp, top = 6.dp, end = 24.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                content()
            }
        }
    }
}

/**
 * This override provides the default behavior of the [ShortNavigationBar] component.
 */
@ExperimentalMaterial3ComponentOverrideApi
object FloatingIconNavigationBarOverride : ShortNavigationBarOverride {
    @Composable
    override fun ShortNavigationBarOverrideScope.ShortNavigationBar() {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier
                    .floatingNavBarContainer(FloatingIconNavigationBarHeight)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

private val BadgeTopRuler = HorizontalRuler()
private val BadgeEndRuler = VerticalRuler()

internal fun Modifier.badgeBounds() =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            width = placeable.width,
            height = placeable.height,
            rulers = {
                // use provides instead of provideRelative cause we will place relative
                // in the badge code
                BadgeEndRuler provides coordinates.size.width.toFloat()
                BadgeTopRuler provides 0f
            },
        ) {
            placeable.place(0, 0)
        }
    }
