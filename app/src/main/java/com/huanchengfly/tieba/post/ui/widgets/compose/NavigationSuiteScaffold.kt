package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.util.fastFirst
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.ui.page.main.BlurBottomNavigation
import com.huanchengfly.tieba.post.ui.page.main.NavigationDrawerContent
import com.huanchengfly.tieba.post.ui.page.main.NavigationItem
import com.huanchengfly.tieba.post.ui.page.main.NavigationRail
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.utils.MainNavigationType
import com.huanchengfly.tieba.post.ui.utils.calculateNavigationPosition
import com.huanchengfly.tieba.post.ui.utils.calculateNavigationType

private const val NavigationSuiteLayoutIdTag = "navigationSuite"
private const val ContentLayoutIdTag = "content"

/**
 * Layout for a [NavigationSuiteScaffold]'s content. This function wraps the [content] and places
 * the [navigationSuite] component according to the given [layoutType].
 *
 * The usage of this function is recommended when you need some customization that is not viable via
 * the use of [NavigationSuiteScaffold]. Example usage:
 *
 * @param navigationSuite the navigation component to be displayed
 * @param layoutType the current [MainNavigationType]
 * @param content the content of your screen
 */
@Composable
fun NavigationSuiteScaffoldLayout(
    modifier: Modifier = Modifier,
    navigationSuite: @Composable () -> Unit,
    navigationBarAtop: Boolean = true,
    layoutType: MainNavigationType = MainNavigationType.BOTTOM_NAVIGATION,
    content: @Composable () -> Unit = {}
) {
    Layout(
        modifier = modifier,
        content = {
            // Wrap the navigation suite and content composables each in a Box to not propagate the
            // parent's (Surface) min constraints to its children (see b/312664933).
            Box(Modifier.layoutId(NavigationSuiteLayoutIdTag)) { navigationSuite() }
            Box(Modifier.layoutId(ContentLayoutIdTag)) { content() }
        }
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // Find the navigation suite composable through it's layoutId tag
        val navigationPlaceable =
            measurables
                .fastFirst { it.layoutId == NavigationSuiteLayoutIdTag }
                .measure(looseConstraints)

        val isNavigationBar = layoutType == MainNavigationType.BOTTOM_NAVIGATION
        val layoutHeight = constraints.maxHeight
        val layoutWidth = constraints.maxWidth

        // Find the content composable through it's layoutId tag
        val contentPlaceable =
            measurables
                .fastFirst { it.layoutId == ContentLayoutIdTag }
                .measure(
                    when {
                        isNavigationBar && navigationBarAtop -> looseConstraints

                        isNavigationBar -> constraints.copy(
                            minHeight = layoutHeight - navigationPlaceable.height,
                            maxHeight = layoutHeight - navigationPlaceable.height
                        )

                        else -> constraints.copy(
                            minWidth = layoutWidth - navigationPlaceable.width,
                            maxWidth = layoutWidth - navigationPlaceable.width
                        )
                    }
                )

        layout(layoutWidth, layoutHeight) {
            if (isNavigationBar) {
                // Place content above the navigation component.
                contentPlaceable.placeRelative(0, 0)
                // Place the navigation component at the bottom of the screen.
                navigationPlaceable.placeRelative(0, layoutHeight - (navigationPlaceable.height))
            } else {
                // Place the navigation component at the start of the screen.
                navigationPlaceable.placeRelative(0, 0)
                // Place content to the side of the navigation component.
                contentPlaceable.placeRelative((navigationPlaceable.width), 0)
            }
        }
    }
}

/** Default window insets to be used and consumed by navigation bar */
val NavigationBarWindowInsets: WindowInsets
    @Composable
    get() = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)

/** Default window insets for navigation rail. */
val RailWindowInsets: WindowInsets
    @Composable
    get() = WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)

val DrawerWindowInsets: WindowInsets
    @Composable
    get() = WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)

@Composable
fun NavigationSuiteScaffold(
    modifier: Modifier = Modifier,
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    navigationItems: List<NavigationItem>,
    navigationBarAtop: Boolean = true,
    layoutType: MainNavigationType = calculateNavigationType(LocalWindowAdaptiveInfo.current),
    navigationContentPosition: MainNavigationContentPosition = calculateNavigationPosition(LocalWindowAdaptiveInfo.current),
    content: @Composable () -> Unit = {},
) {
    NavigationSuiteScaffoldLayout(
        modifier = modifier,
        navigationSuite = {
            when (layoutType) {
                MainNavigationType.PERMANENT_NAVIGATION_DRAWER -> {
                    NavigationDrawerContent(
                        currentPosition = currentPosition,
                        onChangePosition = onChangePosition,
                        navigationItems = navigationItems,
                        navigationContentPosition = navigationContentPosition
                    )
                }

                MainNavigationType.NAVIGATION_RAIL -> {
                    NavigationRail(
                        currentPosition = currentPosition,
                        onChangePosition = onChangePosition,
                        navigationItems = navigationItems,
                        navigationContentPosition = navigationContentPosition
                    )
                }

                MainNavigationType.BOTTOM_NAVIGATION -> {
                    BlurBottomNavigation(
                        currentPosition = currentPosition,
                        onChangePosition = onChangePosition,
                        navigationItems = navigationItems
                    )
                }
            }
        },
        navigationBarAtop = navigationBarAtop,
        layoutType = layoutType,
        content = {
            // Skip consume NavBar WindowInsets if navigationBarAtop enabled
            if (navigationBarAtop && layoutType == MainNavigationType.BOTTOM_NAVIGATION) {
                content()
            } else {
                Box(
                    Modifier.consumeWindowInsets(
                        when (layoutType) {
                            MainNavigationType.BOTTOM_NAVIGATION ->
                                NavigationBarWindowInsets.only(WindowInsetsSides.Bottom)
                            MainNavigationType.NAVIGATION_RAIL ->
                                RailWindowInsets.only(WindowInsetsSides.Start)
                            MainNavigationType.PERMANENT_NAVIGATION_DRAWER ->
                                DrawerWindowInsets.only(WindowInsetsSides.Start)
                        }
                    )
                ) {
                    content()
                }
            }
        }
    )
}
