package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

private val DEFAULT_INDICATOR_WIDTH = 16.dp

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
        val animatedIndicatorOffset by animateDpAsState(targetValue = indicatorOffset)
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

@Composable
fun TabIndicator(
    selectedTabIndex: Int,
    tabPositions: List<TabPosition>,
    tabIndicatorWidth: Dp = DEFAULT_INDICATOR_WIDTH
) {
    if (tabPositions.isNotEmpty()) {
        val currentTab = tabPositions[selectedTabIndex]
        val currentTabLeft = currentTab.left + (currentTab.width / 2 - tabIndicatorWidth / 2)
        val animatedIndicatorOffset by animateDpAsState(targetValue = currentTabLeft)
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.BottomStart)
                .offset(x = animatedIndicatorOffset, y = (-8).dp)
                .width(tabIndicatorWidth)
                .height(3.dp)
                .background(color = LocalContentColor.current, CircleShape)
        )
    }
}

@Composable
fun TabClickMenu(
    selected: Boolean,
    onClick: () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium),
    content: @Composable ColumnScope.() -> Unit,
) {
    LaunchedEffect(Unit) {
        launch {
            interactionSource.interactions
                .filterIsInstance<PressInteraction.Press>()
                .collect {
                    menuState.offset = it.pressPosition
                }
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
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium),
) {
    TabClickMenu(
        selected = selected,
        onClick = onClick,
        menuContent = menuContent,
        modifier = modifier,
        enabled = enabled,
        menuState = menuState,
        interactionSource = interactionSource,
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

        val tabTextStyle =
            MaterialTheme.typography.button.copy(fontSize = 13.sp, letterSpacing = 0.sp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .height(48.dp)
                .padding(start = 16.dp)
        ) {
            ProvideTextStyle(value = tabTextStyle) {
                text()
            }
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotate)
                    .alpha(alpha)
            )
        }
    }
}