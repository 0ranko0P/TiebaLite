package com.huanchengfly.tieba.post.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.NavigationBarHeight
import com.huanchengfly.tieba.post.core.designsystem.layout.layoutId
import kotlin.math.roundToInt


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
        val indicatorAnimationProgress = animateIndicatorProgressAsState(selected)
        Box(
            modifier = Modifier
                .matchParentSize()
                .badgeBounds(),
            contentAlignment = Alignment.Center,
            content = {
                // Create the indicator ripple.
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .indication(interactionSource, ripple())
                )
                // Create the indicator. The indicator has a width-expansion animation which interferes
                // with the timing of the ripple, which is why they are separate composables.
                Box(
                    Modifier
                        .matchParentSize()
                        .drawBehind {
                            val progress = indicatorAnimationProgress.value.coerceAtLeast(0f)
                            drawCircle(colors.selectedIndicatorColor, size.minDimension / 2 * progress)
                        }
                )

                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
            },
        )
    }
}

/**
 * Custom short navigation bar item.
 *
 * Short navigation bars offer a persistent and convenient way to switch between primary
 * destinations in an app.
 *
 * It's recommend for navigation items to always have a text label. An [ShortNavigationBarItem]
 * always displays labels (if they exist) when selected and unselected.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param label text label for this item
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states. See [ShortNavigationBarItemDefaults.colors]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun RoundedIconNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: NavigationItemColors = ShortNavigationBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val styledLabel: @Composable (() -> Unit)? =
        if (label == null) {
            null
        } else {
            {
                StyledLabel(
                    labelTextStyle = MaterialTheme.typography.labelMedium,
                    color = colors.textColor(selected, enabled),
                    animateColor = false,
                    content = label,
                )
            }
        }

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
            .sizeIn(
                minWidth = LocalMinimumInteractiveComponentSize.current,
                minHeight = NavigationItemHeight,
                maxHeight = NavigationItemHeight,
            ),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        val indicatorAnimationProgress = animateIndicatorProgressAsState(selected)

        NavigationItemLayout(
            interactionSource = interactionSource,
            indicatorColor = colors.selectedIndicatorColor,
            indicatorShape = CircleShape,
            icon = {
                val iconColor = colors.iconColor(selected, enabled)
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
            },
            label = styledLabel,
            indicatorAnimationProgress = { indicatorAnimationProgress.value.coerceAtLeast(0f) },
            indicatorPadding = PaddingValues(horizontal = 28.dp, vertical = 4.dp),
            topIconItemPadding = if (label != null) PaddingValues(vertical = 6.dp) else PaddingValues.Zero,
        )
    }
}

@Composable
private fun NavigationItemLayout(
    interactionSource: InteractionSource,
    indicatorColor: Color,
    indicatorShape: Shape,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    indicatorAnimationProgress: () -> Float,
    indicatorPadding: PaddingValues,
    topIconItemPadding: PaddingValues,
) {
    Layout(
        modifier = Modifier.badgeBounds(),
        content = {
            // Create the indicator ripple.
            IndicatorRipple(interactionSource, indicatorShape)
            // Create the indicator. The indicator has a width-expansion animation which interferes
            // with the timing of the ripple, which is why they are separate composables.
            Indicator(indicatorColor, indicatorShape, indicatorAnimationProgress)

            Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }

            if (label != null) {
                Box(Modifier.layoutId(LabelLayoutIdTag)) { label() }
            }
        },
        measurePolicy =
            TopIconOrIconOnlyMeasurePolicy(
                label != null,
                indicatorAnimationProgress,
                indicatorPadding,
                topIconItemPadding,
            )
    )
}

private class TopIconOrIconOnlyMeasurePolicy(
    val hasLabel: Boolean,
    val indicatorAnimationProgress: () -> Float,
    val indicatorPadding: PaddingValues,
    val topIconItemPadding: PaddingValues,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        @Suppress("NAME_SHADOWING") val indicatorAnimationProgress = indicatorAnimationProgress()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val indicatorHorizontalPadding =
            indicatorPadding.calculateStartPadding(layoutDirection) +
                    indicatorPadding.calculateEndPadding(layoutDirection)
        val indicatorVerticalPadding =
            indicatorPadding.calculateTopPadding() + indicatorPadding.calculateBottomPadding()
        // When measuring icon, account for the indicator in its constraints.
        val iconPlaceable =
            measurables
                .fastFirst { it.layoutId == IconLayoutIdTag }
                .measure(
                    looseConstraints.offset(
                        horizontal = -indicatorHorizontalPadding.roundToPx(),
                        vertical = -indicatorVerticalPadding.roundToPx(),
                    )
                )
        // Next, when measuring the indicator and ripple, still need to obey looseConstraints.
        val totalIndicatorWidth = iconPlaceable.width + indicatorHorizontalPadding.roundToPx()
        val indicatorHeight = constraints.maxHeight
        val animatedIndicatorWidth = (totalIndicatorWidth * indicatorAnimationProgress).roundToInt()
        val indicatorRipplePlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(width = totalIndicatorWidth, height = indicatorHeight)
                    )
                )
        val indicatorPlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(width = animatedIndicatorWidth, height = indicatorHeight)
                    )
                )

        return if (hasLabel) {
            val labelPlaceable =
                measurables
                    .fastFirst { it.layoutId == LabelLayoutIdTag }
                    .measure(
                        looseConstraints.offset(
                            vertical = -iconPlaceable.height
                        )
                    )

            placeLabelAndTopIcon(
                labelPlaceable,
                iconPlaceable,
                indicatorRipplePlaceable,
                indicatorPlaceable,
                constraints,
                topIconItemPadding.calculateTopPadding(),
                topIconItemPadding.calculateBottomPadding(),
                topIconItemPadding.calculateStartPadding(layoutDirection),
                topIconItemPadding.calculateEndPadding(layoutDirection),
            )
        } else {
            placeIcon(
                iconPlaceable,
                indicatorRipplePlaceable,
                indicatorPlaceable,
                topIconItemPadding,
                constraints,
            )
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        val iconHeight =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.maxIntrinsicHeight(width)
        val labelHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LabelLayoutIdTag }
                ?.maxIntrinsicHeight(width) ?: 0
        val paddings =
            (topIconItemPadding.calculateTopPadding() +
                    topIconItemPadding.calculateBottomPadding() +
                    indicatorPadding.calculateTopPadding() +
                    indicatorPadding.calculateBottomPadding())
                .roundToPx()

        return iconHeight + labelHeight + paddings
    }
}

/** Places the provided [Placeable]s in the correct position. */
private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable,
    contentPadding: PaddingValues,
    constraints: Constraints,
): MeasureResult {
    val startPadding = contentPadding.calculateStartPadding(layoutDirection).roundToPx()
    val endPadding = contentPadding.calculateEndPadding(layoutDirection).roundToPx()
    val topPadding = contentPadding.calculateTopPadding().roundToPx()
    val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
    val noPaddingsWidth = indicatorRipplePlaceable.width
    val width = constraints.constrainWidth(noPaddingsWidth + startPadding + endPadding)
    val noPaddingsHeight = indicatorRipplePlaceable.height
    val height = constraints.constrainHeight(noPaddingsHeight + topPadding + bottomPadding)

    val centeringHorizontalOffset = (width - indicatorRipplePlaceable.width) / 2
    val centeringVerticalOffset = (height - indicatorRipplePlaceable.height) / 2
    val horizontalOffset = centeringHorizontalOffset + startPadding
    val verticalOffset = centeringVerticalOffset + topPadding

    val indicatorX = (noPaddingsWidth - indicatorPlaceable.width) / 2
    val indicatorY = (noPaddingsHeight - indicatorPlaceable.height) / 2
    val iconX = (noPaddingsWidth - iconPlaceable.width) / 2
    val iconY = (noPaddingsHeight - iconPlaceable.height) / 2
    val rippleX = (noPaddingsWidth - indicatorRipplePlaceable.width) / 2
    val rippleY = (noPaddingsHeight - indicatorRipplePlaceable.height) / 2

    return layout(width, height) {
        indicatorPlaceable.placeRelative(horizontalOffset + indicatorX, verticalOffset + indicatorY)
        iconPlaceable.placeRelative(horizontalOffset + iconX, verticalOffset + iconY)
        indicatorRipplePlaceable.placeRelative(horizontalOffset + rippleX, verticalOffset + rippleY)
    }
}

/**
 * Places the provided [Placeable]s in the correct position.
 *
 * @param labelPlaceable text label placeable inside this item
 * @param iconPlaceable icon placeable inside this item
 * @param indicatorRipplePlaceable indicator ripple placeable inside this item
 * @param indicatorPlaceable indicator placeable inside this item, if it exists
 * @param constraints constraints of the item
 * @param topIconItemTopPadding top padding of the item
 * @param topIconItemBottomPadding bottom padding of the item
 * @param topIconItemStartPadding start padding of the item
 * @param topIconItemEndPadding end padding of the item
 */
private fun MeasureScope.placeLabelAndTopIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable,
    constraints: Constraints,
    topIconItemTopPadding: Dp,
    topIconItemBottomPadding: Dp,
    topIconItemStartPadding: Dp,
    topIconItemEndPadding: Dp,
): MeasureResult {
    val width =
        constraints.constrainWidth(maxOf(labelPlaceable.width, indicatorRipplePlaceable.width))
    val contentHeight =
        indicatorRipplePlaceable.height + labelPlaceable.height
    val height =
        constraints.constrainHeight(
            (contentHeight + topIconItemTopPadding.toPx() + topIconItemBottomPadding.toPx())
                .roundToInt()
        )

    val iconY = topIconItemTopPadding.roundToPx()
    val iconX = (width - iconPlaceable.width) / 2
    val indicatorX = (width - indicatorPlaceable.width) / 2
    val labelX = (width - labelPlaceable.width) / 2
    // Label should be fixed padding below icon.
    val labelY = iconY + iconPlaceable.height
    val rippleX = (width - indicatorRipplePlaceable.width) / 2

    val startPaddingX = topIconItemStartPadding.roundToPx()
    val contentWidth = width + startPaddingX + topIconItemEndPadding.roundToPx()
    return layout(contentWidth, height) {
        indicatorPlaceable.placeRelative(startPaddingX + indicatorX, 0)
        labelPlaceable.placeRelative(startPaddingX + labelX, labelY)
        iconPlaceable.placeRelative(startPaddingX + iconX, iconY)
        indicatorRipplePlaceable.placeRelative(startPaddingX + rippleX, 0)
    }
}

@Composable
private fun StyledLabel(
    labelTextStyle: TextStyle,
    color: Color,
    animateColor: Boolean,
    content: @Composable () -> Unit,
) {
    val textColor =
        if (animateColor) {
            val colorAnimationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
            animateColorAsState(targetValue = color, animationSpec = colorAnimationSpec).value
        } else {
            color
        }
    CompositionLocalProvider(
        LocalContentColor provides textColor,
        LocalTextStyle provides labelTextStyle,
        content = content,
    )
}

@Composable
internal fun animateIndicatorProgressAsState(selected: Boolean) =
    animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
    )

@Composable
private fun IndicatorRipple(interactionSource: InteractionSource, indicatorShape: Shape) {
    Box(
        Modifier.layoutId(IndicatorRippleLayoutIdTag)
            .clip(indicatorShape)
            .indication(interactionSource, ripple())
    )
}

@Composable
private fun Indicator(
    indicatorColor: Color,
    indicatorShape: Shape,
    indicatorAnimationProgress: () -> Float,
) {
    Box(
        Modifier.layoutId(IndicatorLayoutIdTag)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.ModulateAlpha
                alpha = indicatorAnimationProgress()
            }
            .background(color = indicatorColor, shape = indicatorShape)
    )
}

private const val IndicatorRippleLayoutIdTag: String = "indicatorRipple"
private const val IndicatorLayoutIdTag: String = "indicator"
private const val IconLayoutIdTag: String = "icon"
private const val LabelLayoutIdTag: String = "label"

internal val NavigationItemHeight = NavigationBarHeight - 8.dp
