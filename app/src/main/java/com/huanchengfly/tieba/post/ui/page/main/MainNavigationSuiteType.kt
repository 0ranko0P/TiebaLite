package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import com.huanchengfly.tieba.post.ui.widgets.compose.isNavigationBar

/**
 * Extended [NavigationSuiteType] for main navigation
 * */
enum class MainNavigationSuiteType {

    /**
     * A navigation suite type that instructs the [NavigationSuite] to expect a
     * [FloatingIconNavigationBarOverride] with vertical [ShortNavigationBarItem]s that will be
     * displayed at the bottom of the screen in floating style.
     *
     * @see [ShortNavigationBar]
     */
    FloatingNavigationBar,

    /**
     * A navigation suite type that instructs the [NavigationSuite] to expect a
     * [FloatingNavigationBarOverride] with icon only [IconNavigationItem]s that will be displayed
     * at the bottom of the screen in floating style.
     *
     * @see [ShortNavigationBar]
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
        fun fromNavigationSuiteType(type: NavigationSuiteType, floating: Boolean, noLabel: Boolean): MainNavigationSuiteType {
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

        val MainNavigationSuiteType.isFloatingNavigationBar
            get() = this == FloatingNavigationBar || this == FloatingNavigationBarCompact

    }
}