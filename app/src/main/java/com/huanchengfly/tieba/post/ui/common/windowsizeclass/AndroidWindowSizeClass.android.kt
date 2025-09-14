package com.huanchengfly.tieba.post.ui.common.windowsizeclass

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.adapter.computeWindowSizeClass
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo

@Composable
fun calculateWindowSizeClass(activity: Activity): WindowSizeClass {
    // Observe view configuration changes and recalculate the size class on each change. We can't
    // use Activity#onConfigurationChanged as this will sometimes fail to be called on different
    // API levels, hence why this function needs to be @Composable so we can observe the
    // ComposeView's configuration changes.
    LocalConfiguration.current
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    return WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)
}

@ReadOnlyComposable
@Composable
fun isWindowWidthCompact(): Boolean {
    val windowSize = LocalWindowAdaptiveInfo.current.windowSizeClass
    return !windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
}

@ReadOnlyComposable
@Composable
fun isWindowHeightCompact(): Boolean {
    val windowSize = LocalWindowAdaptiveInfo.current.windowSizeClass
    return !windowSize.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)
}

@ReadOnlyComposable
@Composable
fun isLooseWindowWidth(): Boolean = LocalWindowAdaptiveInfo.current.windowSizeClass.isLooseWindowWidth()

fun WindowSizeClass.isLooseWindowWidth(): Boolean {
    return !isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND) ||
            isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
}
