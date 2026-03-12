package com.huanchengfly.tieba.post.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass

/**
 * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
 * ergonomics and reachability depending upon the height of the device.
 */
fun calculateNavigationPosition(adaptiveInfo: WindowAdaptiveInfo): Arrangement.Vertical = with(adaptiveInfo) {
    when {
        windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> {
            Arrangement.Center
        }

        else -> NavigationSuiteDefaults.verticalArrangement
    }
}

fun calculateNavigationType(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType = with(adaptiveInfo) {
    when {
        windowPosture.isTabletop -> NavigationSuiteType.ShortNavigationBarMedium

        !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> {
            NavigationSuiteType.NavigationRail
        }

        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> {
            NavigationSuiteType.NavigationDrawer
        }

        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            NavigationSuiteType.NavigationRail
        }

        else -> NavigationSuiteType.NavigationBar
    }
}
