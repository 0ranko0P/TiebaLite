package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastFirstOrNull
import com.huanchengfly.tieba.post.copy
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import kotlinx.coroutines.delay

// Workaround to enable background blurring on StickyHeader
@Composable @ReadOnlyComposable
fun useStickyHeaderWorkaround(): Boolean = !MaterialTheme.colorScheme.isTranslucent

/**
 * Workaround to enable background blurring on StickyHeader.
 *
 * This composes [header] as overlay inside the TopAppBar when first item becomes invisible.
 * */
@Composable
fun StickyHeaderOverlay(state: LazyListState, header: @Composable () -> Unit) {
    val shouldPinOnTopBar by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
    if (shouldPinOnTopBar) {
        header()
    }
}

/**
 * Workaround to ignore padding top changes when using [StickyHeaderOverlay].
 * */
@Composable
fun PaddingValues.fixedTopBarPadding(topBarHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight): PaddingValues {
    if (!useStickyHeaderWorkaround()) return this

    val statusBarWindowInsets = WindowInsets.statusBars
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    return remember(
        calculateStartPadding(direction),
        topBarHeight,
        calculateEndPadding(direction),
        calculateBottomPadding()
    ) {
        with(density) {
            copy(direction, top = statusBarWindowInsets.getTop(density).toDp() + topBarHeight)
        }
    }
}

/**
 * Workaround to apply TopAppBar background changes on StickyHeader.
 * */
fun Modifier.stickyHeaderBackground(appBarState: TopAppBarState, listState: LazyListState) = composed {
    val topAppBarColors = TiebaLiteTheme.topAppBarColors
    drawWithCache {
        onDrawBehind {
            // double check, LazyListState#scrollToItem might cause buggy TopAppbarState
            val color = if (appBarState.overlappedFraction > 0.01f && listState.firstVisibleItemIndex > 0) {
                topAppBarColors.scrolledContainerColor
            } else {
                topAppBarColors.containerColor
            }
            drawRect(color)
        }
    }
}

/**
 * Workaround for overlapped [StickyHeaderOverlay] when [fixedTopBarPadding] is enabled.
 * */
suspend fun LazyListState.scrollToItemWithHeader(
    index: Int,
    scrollOffset: Int = 0,
    animate: Boolean = true,
    isHeader: (LazyListItemInfo) -> Boolean
) {
    if (!canScrollBackward && index == 0 && scrollOffset == 0) {
        return  // Skip unnecessary scrolling
    }
    // Laggy list, delay 100ms
    if (layoutInfo.visibleItemsInfo.isEmpty()) delay(100)

    val totalOffset = if (index <= 1 || layoutInfo.visibleItemsInfo.isEmpty()) {
        scrollOffset
    } else {
        // First item occupied all the space, snap to header
        if (layoutInfo.visibleItemsInfo.size == 1) {
            scrollToItem(1, scrollOffset = 0)
        }
        val headerSize = layoutInfo.visibleItemsInfo.fastFirstOrNull(isHeader)?.size ?: 0
        scrollOffset - headerSize
    }

    if (animate) {
        animateScrollToItem(index, scrollOffset = totalOffset)
    } else {
        scrollToItem(index, scrollOffset = totalOffset)
    }
}