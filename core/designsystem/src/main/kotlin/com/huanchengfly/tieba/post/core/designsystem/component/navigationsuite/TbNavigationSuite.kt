package com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.WideNavigationRailState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.core.designsystem.component.FloatingIconNavigationBar
import com.huanchengfly.tieba.post.core.designsystem.component.FloatingNavigationBar
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteType.Companion.toNavigationSuiteType

/**
 * TbLite navigation component according to the current [TbNavigationSuiteType] to be used with
 * the [NavigationSuiteScaffold].
 *
 * Added more custom navigation component, see [FloatingNavigationBar], [FloatingIconNavigationBar].
 *
 * @param tbNavigationSuiteType the [TbNavigationSuiteType] of the associated [NavigationSuiteScaffold].
 * @param modifier the [Modifier] to be applied to the navigation component
 * @param colors [NavigationSuiteColors] that will be used to determine the container (background)
 *   color of the navigation component and the preferred color for content inside the navigation
 *   component
 * @param verticalArrangement the vertical arrangement of the items inside vertical navigation
 *   components, such as the wide navigation rail. It's recommended to use [Arrangement.Top],
 *   [Arrangement.Center], or [Arrangement.Bottom].
 * @param primaryActionContent The optional primary action content of the navigation suite scaffold,
 *   if any. Typically a [androidx.compose.material3.FloatingActionButton]. It'll be displayed
 *   inside vertical navigation components as their header, and above horizontal navigation
 *   components.
 * @param content the content inside the current navigation component, typically
 *   [TbNavigationSuiteItem]s
 */
@NonRestartableComposable
@Composable
fun TbNavigationSuite(
    tbNavigationSuiteType: TbNavigationSuiteType,
    modifier: Modifier = Modifier,
    railState: WideNavigationRailState = rememberWideNavigationRailState(),
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    verticalArrangement: Arrangement.Vertical = NavigationSuiteDefaults.verticalArrangement,
    primaryActionContent: @Composable (() -> Unit) = {},
    content: @Composable () -> Unit,
) = when (tbNavigationSuiteType) {
    TbNavigationSuiteType.FloatingNavigationBar -> {
        FloatingNavigationBar(
            modifier = modifier,
            containerColor = colors.shortNavigationBarContainerColor,
            contentColor = colors.shortNavigationBarContentColor,
            content = remember(content) { movableContentOf(content) },
        )
    }
    TbNavigationSuiteType.FloatingNavigationBarCompact -> {
        FloatingIconNavigationBar(
            modifier = modifier,
            containerColor = colors.shortNavigationBarContainerColor,
            contentColor = colors.shortNavigationBarContentColor,
            content = remember(content) { movableContentOf(content) },
        )
    }
    TbNavigationSuiteType.WideNavigationRailExpanded -> {
        androidx.compose.material3.WideNavigationRail(
            modifier = modifier,
            state = railState,
            header = primaryActionContent,
            arrangement = verticalArrangement,
            colors = colors.wideNavigationRailColors,
            contentPadding = PaddingValues(top = 8.dp, bottom = 44.dp),
            content = remember(content) { movableContentOf(content) },
        )
    }
    else -> {
        androidx.compose.material3.adaptive.navigationsuite.NavigationSuite(
            navigationSuiteType = tbNavigationSuiteType.toNavigationSuiteType(),
            modifier = modifier,
            colors = colors,
            verticalArrangement = verticalArrangement,
            primaryActionContent = primaryActionContent,
            content = content,
        )
    }
}
