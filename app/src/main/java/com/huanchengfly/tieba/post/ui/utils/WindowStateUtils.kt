package com.huanchengfly.tieba.post.ui.utils

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass

/**
 * Different type of navigation supported by app depending on device size and state.
 */
enum class MainNavigationType {
    BOTTOM_NAVIGATION, NAVIGATION_RAIL, PERMANENT_NAVIGATION_DRAWER
}

/**
 * Different position of navigation content inside Navigation Rail, Navigation Drawer depending on device size and state.
 */
enum class MainNavigationContentPosition {
    TOP, CENTER
}

/**
 * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
 * ergonomics and reachability depending upon the height of the device.
 */
fun calculateNavigationPosition(windowInfo: WindowAdaptiveInfo): MainNavigationContentPosition {
    return when {
        windowInfo.windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> {
            MainNavigationContentPosition.CENTER
        }

        else -> MainNavigationContentPosition.TOP
    }
}

fun calculateNavigationType(windowInfo: WindowAdaptiveInfo): MainNavigationType = with(windowInfo) {
    when {
        windowPosture.isTabletop -> MainNavigationType.BOTTOM_NAVIGATION

        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            MainNavigationType.PERMANENT_NAVIGATION_DRAWER
        }

        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            MainNavigationType.NAVIGATION_RAIL
        }

        else -> MainNavigationType.BOTTOM_NAVIGATION
    }
}
