package com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite

import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Stable
import androidx.window.core.layout.WindowSizeClass

/**
 * Extended [NavigationSuiteType] for main navigation
 * */
enum class TbNavigationSuiteType {

    /**
     * A navigation suite type that instructs the [NavigationSuite] to expect a
     * [FloatingIconNavigationBarOverride] with vertical [ShortNavigationBarItem]s that will be
     * displayed at the bottom of the screen in floating style.
     *
     * @see [androidx.compose.material3.ShortNavigationBar]
     */
    FloatingNavigationBar,

    /**
     * A navigation suite type that instructs the [NavigationSuite] to expect a
     * [FloatingNavigationBarOverride] with icon only [IconNavigationItem]s that will be displayed
     * at the bottom of the screen in floating style.
     *
     * @see [androidx.compose.material3.ShortNavigationBar]
     */
    FloatingNavigationBarCompact,

    /** @see [NavigationSuiteType.ShortNavigationBarCompact] */
    ShortNavigationBarCompact,

    /** @see [NavigationSuiteType.ShortNavigationBarMedium] */
    ShortNavigationBarMedium,

    /** @see [NavigationSuiteType.WideNavigationRailCollapsed] */
    WideNavigationRailCollapsed,

    /** @see [NavigationSuiteType.WideNavigationRailExpanded] */
    WideNavigationRailExpanded,

    /** @see [NavigationSuiteType.NavigationBar] */
    NavigationBar,

    /** @see [NavigationSuiteType.NavigationRail] */
    NavigationRail,

    /** @see [NavigationSuiteType.NavigationDrawer] */
    NavigationDrawer,

    /** @see [NavigationSuiteType.None] */
    None;

    companion object {
        @Stable
        fun fromNavigationSuiteType(type: NavigationSuiteType, floating: Boolean, noLabel: Boolean): TbNavigationSuiteType {
            if (type.isNavigationBar) {
                if (floating && noLabel) {
                    return FloatingNavigationBarCompact
                } else if (floating) {
                    return FloatingNavigationBar
                } else if (noLabel) {
                    return ShortNavigationBarCompact
                }
            }
            return when (type) {
                NavigationSuiteType.ShortNavigationBarCompact -> ShortNavigationBarCompact
                NavigationSuiteType.ShortNavigationBarMedium -> ShortNavigationBarMedium
                NavigationSuiteType.WideNavigationRailCollapsed -> WideNavigationRailCollapsed
                NavigationSuiteType.WideNavigationRailExpanded -> WideNavigationRailExpanded
                NavigationSuiteType.NavigationBar -> NavigationBar
                NavigationSuiteType.NavigationRail -> NavigationRail
                NavigationSuiteType.NavigationDrawer -> NavigationDrawer
                NavigationSuiteType.None -> None
                else -> None
            }
        }

        @Stable
        fun TbNavigationSuiteType.toNavigationSuiteType(): NavigationSuiteType {
            return when (this) {
                FloatingNavigationBar,
                FloatingNavigationBarCompact,
                ShortNavigationBarCompact -> NavigationSuiteType.ShortNavigationBarCompact
                ShortNavigationBarMedium -> NavigationSuiteType.ShortNavigationBarMedium
                WideNavigationRailCollapsed -> NavigationSuiteType.WideNavigationRailCollapsed
                WideNavigationRailExpanded -> NavigationSuiteType.WideNavigationRailExpanded
                NavigationBar -> NavigationSuiteType.NavigationBar
                NavigationRail -> NavigationSuiteType.NavigationRail
                NavigationDrawer -> NavigationSuiteType.NavigationDrawer
                None -> NavigationSuiteType.None
            }
        }

        val TbNavigationSuiteType.isFloatingNavigationBar
            get() = this == FloatingNavigationBar || this == FloatingNavigationBarCompact

    }
}

fun calculateNavigationType(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType = with(adaptiveInfo) {
    when {
        windowPosture.isTabletop -> NavigationSuiteType.ShortNavigationBarMedium

        windowSizeClass.minHeight == WindowSizeClass.HeightSizeClasses.Compact -> {
            NavigationSuiteType.NavigationRail
        }

        windowSizeClass.minWidth == WindowSizeClass.WidthSizeClasses.Compact -> {
            NavigationSuiteType.ShortNavigationBarCompact
        }

        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> {
            NavigationSuiteType.NavigationDrawer
        }

        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            NavigationSuiteType.NavigationRail
        }

        else -> NavigationSuiteType.NavigationBar
    }
}