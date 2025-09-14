package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.PaddingNone

/**
 * compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/pulltorefresh/PullToRefresh.kt
 *
 * commit 576eeec 'Bump the version number for Material3 to 1.4.0-rc01'.
 * on branch androidx-compose-material3-release
 *
 * 0Ranko0p changes:
 *   1. Add ability to enable/disable pullToRefresh
 *   2. Apply content paddings to indicator
 *   3. Change default indicator to custom [TopPullToRefreshIndicator]
 */

/**
 * Indicator for [PullToRefreshBox] with primary color background.
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopPullToRefreshIndicator(
    modifier: Modifier = Modifier,
    state: PullToRefreshState,
    isRefreshing: Boolean
) {
    Indicator(
        modifier = modifier,
        state = state,
        isRefreshing = isRefreshing,
        containerColor = MaterialTheme.colorScheme.primary,
        color = MaterialTheme.colorScheme.onPrimary
    )
}

/**
 * Indicator without [PullToRefreshState].
 * */
@Composable
fun RefreshIndicator(
    modifier: Modifier = Modifier,
    isRefreshing: Boolean,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Crossfade(
        modifier = modifier.padding(top = 12.dp),
        targetState = isRefreshing,
        animationSpec = tween(durationMillis = 100)
    ) { refreshing ->
        if (refreshing) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(containerColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    color = color,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * [PullToRefreshBox] is a container that expects a scrollable layout as content and adds gesture
 * support for manually refreshing when the user swipes downward at the beginning of the content. By
 * default, it uses [PullToRefreshDefaults.Indicator] as the refresh indicator, but you may also
 * choose to set your own indicator or use [TopPullToRefreshIndicator].
 *
 * @param isRefreshing whether a refresh is occurring
 * @param onRefresh callback invoked when the user gesture crosses the threshold, thereby requesting
 *   a refresh.
 * @param modifier the [Modifier] to be applied to this container
 * @param state the state that keeps track of distance pulled
 * @param contentAlignment The default alignment inside the Box.
 * @param indicator the indicator that will be drawn on top of the content when the user begins a
 *   pull or a refresh is occurring
 * @param content the content of the pull refresh container, typically a scrollable layout such as
 *   [LazyColumn] or a layout using [Modifier.verticalScroll]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopCenter,
    contentPadding: PaddingValues = PaddingNone,
    indicator: @Composable BoxScope.() -> Unit = {
        TopPullToRefreshIndicator(
            isRefreshing = isRefreshing,
            state = state,
        )
    },
    content: @Composable BoxScope.() -> Unit,
) {
    if (enabled) {
        Box(
            modifier.pullToRefresh(state = state, isRefreshing = isRefreshing, onRefresh = onRefresh),
            contentAlignment = contentAlignment,
        ) {
            content()
            Box(
                modifier = Modifier.padding(contentPadding),
                content = indicator
            )
        }
    } else {
        Box(modifier = modifier, content = content)
    }
}
