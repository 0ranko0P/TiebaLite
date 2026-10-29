package com.huanchengfly.tieba.post.core.ui.component

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.WidthSizeClasses

@Composable
fun defaultGridCells(): StaggeredGridCells = mediaQuery {
    when {
        windowWidth <= 460.dp -> StaggeredGridCells.Fixed(1)
        windowWidth <= WindowSizeClass.WidthSizeClasses.Medium -> StaggeredGridCells.Fixed(2)
        else -> StaggeredGridCells.Adaptive(320.dp)
    }
}

private const val LoadingIndicatorContentType = "Loading"

val FeedGridItemSpacing = 16.dp

@Composable
fun FeedLazyVerticalStaggeredGrid(
    modifier: Modifier = Modifier,
    columns: StaggeredGridCells = defaultGridCells(),
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
    verticalItemSpacing: Dp = FeedGridItemSpacing,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(FeedGridItemSpacing),
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    onLazyLoad: (() -> Unit)? = null,
    isLoading: () -> Boolean = { false },
    content: LazyStaggeredGridScope.() -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = columns,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalItemSpacing = verticalItemSpacing,
        horizontalArrangement = horizontalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = {
            content()

            onLazyLoad?.let {
                item(key = LoadingIndicatorContentType, contentType = LoadingIndicatorContentType) {
                    LoadingIndicator(
                        modifier = Modifier.onVisibilityChanged(minDurationMs = 300) { visible ->
                            if (visible && !isLoading() && state.layoutInfo.visibleItemsInfo.size > 1) {
                                onLazyLoad()
                            }
                        },
                    )
                }
            }
        }
    )
}

fun LazyStaggeredGridScope.singleLineItem(
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyStaggeredGridItemScope.() -> Unit,
) = item(key, contentType, StaggeredGridItemSpan.FullLine, content)