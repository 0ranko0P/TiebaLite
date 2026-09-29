package com.huanchengfly.tieba.post.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.huanchengfly.tieba.post.core.designsystem.component.FloatingIconNavigationBarOverride.ShortNavigationBar
import com.huanchengfly.tieba.post.core.designsystem.component.FloatingNavigationBarOverride.ShortNavigationBar
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.NavigationBarHeight
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TallNavigationBarHeight

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val floatingNavigationBarCompactScreenOffset: Dp
    @Composable
    get() = if (WindowInsets.navigationBars.getBottom(LocalDensity.current) > 0) {
        FloatingToolbarDefaults.ScreenOffset / 4
    } else {
        FloatingToolbarDefaults.ScreenOffset
    }

private val FloatingNavigationBarElevation: Dp = 1.dp

private val FloatingIconNavigationBarHeight = NavigationBarHeight

val ColorScheme.vibrantFloatingNavigationBarColor: Color
    get() = surfaceColorAtElevation(4.dp)

val ColorScheme.vibrantFloatingNavigationBarContentColor: Color
    get() = onSurface

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
@ExperimentalMaterial3ExpressiveApi
@Composable
fun IconNavigationItem(
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

@ExperimentalMaterial3ComponentOverrideApi
context(scope: ShortNavigationBarOverrideScope)
private fun Modifier.floatingNavBarContainer(
    height: Dp,
    screenOffset: Dp,
): Modifier = this then with(scope) {
    Modifier
        .windowInsetsPadding(windowInsets)
        .heightIn(min = height)
        .clickable(interactionSource = null, indication = null, onClick = {})
        .selectableGroup()
        .graphicsLayer {
            this.shape = CircleShape
            this.clip = true
            this.translationY = -screenOffset.toPx()
            this.shadowElevation = FloatingNavigationBarElevation.toPx()
        }
        .then(modifier)
        .background(color = containerColor, shape = CircleShape)
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
                    .floatingNavBarContainer(
                        height = NavigationBarHeight,
                        screenOffset = floatingNavigationBarCompactScreenOffset,
                    )
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
                    .floatingNavBarContainer(
                        height = FloatingIconNavigationBarHeight,
                        screenOffset = floatingNavigationBarCompactScreenOffset,
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

@ExperimentalMaterial3ComponentOverrideApi
object DefaultNavigationBarOverride : ShortNavigationBarOverride {
    @Composable
    override fun ShortNavigationBarOverrideScope.ShortNavigationBar() {
        Surface(color = containerColor, contentColor = contentColor, modifier = modifier) {
            Layout(
                modifier =
                    Modifier
                        .windowInsetsPadding(windowInsets)
                        .height(TallNavigationBarHeight)
                        .selectableGroup(),
                content = content,
                measurePolicy = EqualWeightContentMeasurePolicy,
            )
        }
    }
}

// androidx.compose.material3.EqualWeightContentMeasurePolicy
private object EqualWeightContentMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val width = constraints.maxWidth
        var itemHeight = constraints.minHeight
        val itemsCount = measurables.size
        // If there are no items, bar will be empty.
        if (itemsCount < 1) {
            return layout(width, itemHeight) {}
        }

        val itemsPlaceables: List<Placeable>
        if (!constraints.hasBoundedWidth) {
            // If width constraint is not bounded, let item containers widths be as big as they are.
            // This may lead to a different items arrangement than the expected.
            itemsPlaceables =
                measurables.fastMap {
                    it.measure(constraints.constrain(Constraints.fixedHeight(height = itemHeight)))
                }
        } else {
            val itemWidth = width / itemsCount
            measurables.fastForEach {
                val measurableHeight = it.maxIntrinsicHeight(itemWidth)
                if (itemHeight < measurableHeight) {
                    itemHeight = measurableHeight.coerceAtMost(constraints.maxHeight)
                }
            }

            // Make sure the item containers have the same width and height.
            itemsPlaceables =
                measurables.fastMap {
                    it.measure(
                        constraints.constrain(
                            Constraints.fixed(width = itemWidth, height = itemHeight)
                        )
                    )
                }
        }

        return layout(width, itemHeight) {
            var x = 0
            val y = 0
            itemsPlaceables.fastForEach { item ->
                item.placeRelative(x, y)
                x += item.width
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
