package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo

@Composable
fun Container(
    modifier: Modifier = Modifier,
    fluid: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val windowSize = LocalWindowAdaptiveInfo.current.windowSizeClass
    val widthFraction = remember(windowSize) {
        when {
            fluid -> 1f
            windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND) -> 0.75f
            windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) -> 0.87f
            else -> 1f
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier.fillMaxWidth(widthFraction),
        ) {
            content()
        }
    }
}

@Composable
fun OneTimeMeasurer(modifier: Modifier = Modifier, content: @Composable BoxScope.(IntSize?) -> Unit) {
    var size: IntSize? by remember { mutableStateOf(null) }

    Box(
        modifier = if (size == null) modifier.onGloballyPositioned { size = it.size } else modifier
    ) {
        content(size)
    }
}