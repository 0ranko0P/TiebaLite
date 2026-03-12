/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import com.huanchengfly.tieba.post.NoWindowInsets

/**
 * compose/material3/material3-adaptive-navigation-suite/src/commonMain/kotlin/androidx/compose/material3/adaptive/navigationsuite/NavigationSuiteScaffold.kt
 *
 * Commit 37c3263 'Extract material3-adaptive-navigation-suite host tests to common'.
 * of branch androidx-main
 *
 * 0Ranko0p changes:
 *   1. Add navigationBarAtop option to NavigationSuiteScaffoldLayout
 *   2. Add navigationBarAtop option to Modifier.navigationSuiteScaffoldConsumeWindowInsets
 */

/**
 * Layout for a [NavigationSuiteScaffold]'s content. This function wraps the [content] and places
 * the [navigationSuite], and the [primaryActionContent], if any, according to the current
 * [NavigationSuiteType].
 *
 * The usage of this function is recommended when you need some customization that is not viable via
 * the use of [NavigationSuiteScaffold]. An usage example of using a custom modal wide rail can be
 * found at androidx.compose.material3.demos.NavigationSuiteScaffoldCustomConfigDemo.
 *
 * @param navigationSuite the navigation component to be displayed, typically [NavigationSuite]
 * @param navigationSuiteType the current [NavigationSuiteType]. Usually
 *   [NavigationSuiteScaffoldDefaults.navigationSuiteType]
 * @param navigationBarAtop place the content behind the navigation bar just like MD3 Scaffold
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold layout
 * @param primaryActionContent The optional primary action content of the navigation suite scaffold,
 *   if any. Typically a [androidx.compose.material3.FloatingActionButton]. It'll be displayed
 *   inside vertical navigation components as part of their header, and above horizontal navigation
 *   components.
 * @param primaryActionContentHorizontalAlignment The horizontal alignment of the primary action
 *   content, if present, when it's displayed along with a horizontal navigation component.
 * @param content the content of your screen
 */
@Composable
fun NavigationSuiteScaffoldLayout(
    modifier: Modifier = Modifier,
    navigationSuite: @Composable () -> Unit,
    navigationSuiteType: NavigationSuiteType,
    navigationBarAtop: Boolean = true,
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    primaryActionContent: @Composable (() -> Unit) = {},
    primaryActionContentHorizontalAlignment: Alignment.Horizontal =
        NavigationSuiteScaffoldDefaults.primaryActionContentAlignment,
    content: @Composable () -> Unit,
) {
    val animationProgress by
        animateFloatAsState(
            targetValue = if (state.currentValue == NavigationSuiteScaffoldValue.Hidden) 0f else 1f,
            animationSpec = AnimationSpec,
        )

    Layout({
        // Wrap the navigation suite and content composables each in a Box to not propagate the
        // parent's (Surface) min constraints to its children (see b/312664933).
        Box(Modifier.layoutId(NavigationSuiteLayoutIdTag)) { navigationSuite() }
        Box(Modifier.layoutId(PrimaryActionContentLayoutIdTag)) { primaryActionContent() }
        Box(Modifier.layoutId(ContentLayoutIdTag)) { content() }
    },
        modifier = modifier,
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // Find the navigation suite composable through it's layoutId tag
        val navigationPlaceable =
            measurables
                .fastFirst { it.layoutId == NavigationSuiteLayoutIdTag }
                .measure(looseConstraints)
        val primaryActionContentPlaceable =
            measurables
                .fastFirst { it.layoutId == PrimaryActionContentLayoutIdTag }
                .measure(looseConstraints)
        val isNavigationBar = navigationSuiteType.isNavigationBar
        val layoutHeight = constraints.maxHeight
        val layoutWidth = constraints.maxWidth
        // Find the content composable through it's layoutId tag.
        val contentPlaceable =
            measurables
                .fastFirst { it.layoutId == ContentLayoutIdTag }
                .measure(
                    // 0Ranko0p changes: Lays out the content behind the navbar just like MD3
                    // Scaffold, this is required for background blurring
                    if (isNavigationBar && navigationBarAtop) {
                        looseConstraints
                    // End of 0Ranko0p changes
                    } else if (isNavigationBar) {
                        constraints.copy(
                            minHeight =
                                layoutHeight -
                                        (navigationPlaceable.height * animationProgress).toInt(),
                            maxHeight =
                                layoutHeight -
                                        (navigationPlaceable.height * animationProgress).toInt(),
                        )
                    } else {
                        constraints.copy(
                            minWidth =
                                layoutWidth -
                                    (navigationPlaceable.width * animationProgress).toInt(),
                            maxWidth =
                                layoutWidth -
                                    (navigationPlaceable.width * animationProgress).toInt(),
                        )
                    }
                )

        layout(layoutWidth, layoutHeight) {
            if (isNavigationBar) {
                // Place content above the navigation component.
                contentPlaceable.placeRelative(0, 0)
                // Place the navigation component at the bottom of the screen.
                navigationPlaceable.placeRelative(
                    0,
                    layoutHeight - (navigationPlaceable.height * animationProgress).toInt(),
                )
                // Place the primary action content above the navigation component.
                val positionX =
                    if (primaryActionContentHorizontalAlignment == Alignment.Start) {
                        PrimaryActionContentPadding.roundToPx()
                    } else if (
                        primaryActionContentHorizontalAlignment == Alignment.CenterHorizontally
                    ) {
                        (layoutWidth - primaryActionContentPlaceable.width) / 2
                    } else {
                        layoutWidth -
                            primaryActionContentPlaceable.width -
                            PrimaryActionContentPadding.roundToPx()
                    }
                primaryActionContentPlaceable.placeRelative(
                    positionX,
                    layoutHeight -
                        primaryActionContentPlaceable.height -
                        PrimaryActionContentPadding.roundToPx() -
                        (navigationPlaceable.height * animationProgress).toInt(),
                )
            } else {
                // Place the navigation component at the start of the screen.
                navigationPlaceable.placeRelative(
                    (0 - (navigationPlaceable.width * (1f - animationProgress))).toInt(),
                    0,
                )
                // Place content to the side of the navigation component.
                contentPlaceable.placeRelative(
                    (navigationPlaceable.width * animationProgress).toInt(),
                    0,
                )
            }
        }
    }
}

@Composable
fun Modifier.navigationSuiteScaffoldConsumeWindowInsets(
    navigationSuiteType: NavigationSuiteType,
    navigationBarAtop: Boolean,
    state: NavigationSuiteScaffoldState,
): Modifier =
    consumeWindowInsets(
        if (state.currentValue == NavigationSuiteScaffoldValue.Hidden && !state.isAnimating) {
            NoWindowInsets
        // 0Ranko0p changes: NoWindowInsets when navigationBarAtop enabled
        } else if (navigationBarAtop && navigationSuiteType.isNavigationBar) {
            NoWindowInsets
        // End of 0Ranko0p changes
        } else {
            when (navigationSuiteType) {
                NavigationSuiteType.ShortNavigationBarCompact,
                NavigationSuiteType.ShortNavigationBarMedium ->
                    ShortNavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
                NavigationSuiteType.WideNavigationRailCollapsed,
                NavigationSuiteType.WideNavigationRailExpanded ->
                    WideNavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                NavigationSuiteType.NavigationBar ->
                    NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
                NavigationSuiteType.NavigationRail ->
                    NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                NavigationSuiteType.NavigationDrawer ->
                    DrawerDefaults.windowInsets.only(WindowInsetsSides.Start)
                else -> NoWindowInsets
            }
        }
    )

private const val NavigationSuiteLayoutIdTag = "navigationSuite"
private const val PrimaryActionContentLayoutIdTag = "primaryActionContent"
private const val ContentLayoutIdTag = "content"

// androidx.compose.material3.tokens.NavigationBarTokens
internal val TallNavigationBarHeight = 80.dp
internal val NavigationBarHeight = 64.dp
private val PrimaryActionContentPadding = 16.dp
private val AnimationSpec: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 700f)

internal val NavigationSuiteType.isNavigationBar
    get() =
        this == NavigationSuiteType.ShortNavigationBarCompact ||
                this == NavigationSuiteType.ShortNavigationBarMedium ||
                this == NavigationSuiteType.NavigationBar
