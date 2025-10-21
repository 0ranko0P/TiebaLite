package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

private val DEFAULT_INDICATOR_WIDTH = 16.dp
private val DEFAULT_INDICATOR_HEIGHT = 3.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerTabIndicator(
    pagerState: PagerState,
    tabPositions: List<TabPosition>,
    tabWidth: Dp = DEFAULT_INDICATOR_WIDTH
) {
    if (tabPositions.isNotEmpty()) {
        val currentPage = minOf(tabPositions.lastIndex, pagerState.currentPage)
        val currentTab = tabPositions[currentPage]
        val prevTab = tabPositions.getOrNull(currentPage - 1)
        val nextTab = tabPositions.getOrNull(currentPage + 1)
        val fraction = pagerState.currentPageOffsetFraction
        val currentTabLeft = currentTab.left + (currentTab.width / 2 - tabWidth / 2)
        val indicatorOffset = if (fraction > 0 && nextTab != null) {
            val nextTabLeft = nextTab.left + (nextTab.width / 2 - tabWidth / 2)
            lerp(currentTabLeft, nextTabLeft, fraction)
        } else if (fraction < 0 && prevTab != null) {
            val prevTabLeft = prevTab.left + (prevTab.width / 2 - tabWidth / 2)
            lerp(currentTabLeft, prevTabLeft, -fraction)
        } else {
            currentTabLeft
        }
        val animatedIndicatorOffset by
            animateDpAsState(
                targetValue = indicatorOffset,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.BottomStart)
                .offset(x = animatedIndicatorOffset, y = (-8).dp)
                .width(tabWidth)
                .height(3.dp)
                .background(color = LocalContentColor.current, CircleShape)
        )
    }
}

/**
 * From androidx.compose.material3.samples.FancyAnimatedIndicatorWithModifier
 *
 * 0Ranko0P changes:
 *   1. border indicator to line indicator
 *   2. remove color animation
 *   3. add vertical padding support
 * */
@Composable
fun TabIndicatorScope.FancyAnimatedIndicatorWithModifier(
    index: Int,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    verticalPadding: Dp = Dp.Hairline
) {
    var startAnimatable by remember { mutableStateOf<Animatable<Dp, AnimationVector1D>?>(null) }
    var endAnimatable by remember { mutableStateOf<Animatable<Dp, AnimationVector1D>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val cornerRadius = CornerRadius(4.5f)

    Box(
        Modifier
            .tabIndicatorLayout { measurable: Measurable,
                                  constraints: Constraints,
                                  tabPositions: List<TabPosition> ->
                val newStart = tabPositions[index].left
                val newEnd = tabPositions[index].right

                val startAnim =
                    startAnimatable
                        ?: Animatable(newStart, Dp.VectorConverter).also { startAnimatable = it }

                val endAnim =
                    endAnimatable
                        ?: Animatable(newEnd, Dp.VectorConverter).also { endAnimatable = it }

                if (endAnim.targetValue != newEnd) {
                    coroutineScope.launch {
                        endAnim.animateTo(
                            newEnd,
                            animationSpec =
                                if (endAnim.value < newEnd) {
                                    spring(stiffness = Spring.StiffnessMedium)
                                } else {
                                    spring(stiffness = Spring.StiffnessVeryLow)
                                }
                        )
                    }
                }

                if (startAnim.targetValue != newStart) {
                    coroutineScope.launch {
                        startAnim.animateTo(
                            newStart,
                            animationSpec =
                                // Handle directionality here, if we are moving to the right, we
                                // want the right side of the indicator to move faster, if we are
                                // moving to the left, we want the left side to move faster.
                                if (startAnim.value < newStart) {
                                    spring(stiffness = Spring.StiffnessVeryLow)
                                } else {
                                    spring(stiffness = Spring.StiffnessMedium)
                                }
                        )
                    }
                }

                val indicatorEnd = endAnim.value.roundToPx()
                val indicatorStart = startAnim.value.roundToPx()

                val indicatorWidth = indicatorEnd - indicatorStart
                val indicatorHeight = DEFAULT_INDICATOR_HEIGHT.roundToPx()
                val horizontalPadding = (tabPositions[index].width - tabPositions[index].contentWidth).times(0.5f).roundToPx()

                // Apply an offset from the start to correctly position the indicator around the tab
                val placeable =
                    measurable.measure(
                        Constraints.fixed(
                            width = (indicatorWidth - horizontalPadding * 2).coerceIn(0, indicatorWidth),
                            height = indicatorHeight
                        )
                    )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        x = indicatorStart + horizontalPadding,
                        y = constraints.maxHeight - indicatorHeight - verticalPadding.roundToPx()
                    )
                }
            }
            .fillMaxSize()
            .drawWithContent {
                drawRoundRect(color = indicatorColor, cornerRadius = cornerRadius)
            }
    )
}

@Composable
fun TabClickMenu(
    selected: Boolean,
    onClick: () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = 0.7f),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Press>()
            .collect {
                menuState.offset = it.pressPosition.round()
            }
    }

    ClickMenu(
        menuContent = menuContent,
        menuState = menuState
    ) {
        Tab(
            selected = selected,
            onClick = {
                if (!selected) {
                    onClick()
                } else {
                    menuState.toggle()
                }
            },
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource,
            selectedContentColor = selectedContentColor,
            unselectedContentColor = unselectedContentColor,
            content = content
        )
    }
}

@Composable
fun TabClickMenu(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = 0.7f),
) {
    TabClickMenu(
        selected = selected,
        onClick = onClick,
        menuContent = menuContent,
        modifier = modifier,
        enabled = enabled,
        menuState = menuState,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
    ) {
        val rotate by animateFloatAsState(
            targetValue = if (menuState.expanded) 180f else 0f,
            label = "ArrowIndicatorRotate"
        )
        val alpha by animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            label = "ArrowIndicatorAlpha"
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .height(48.dp)
                .padding(start = 16.dp)
        ) {
            text()

            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier
                    .graphicsLayer {
                        this.rotationZ = rotate
                        this.alpha = alpha
                    }
            )
        }
    }
}