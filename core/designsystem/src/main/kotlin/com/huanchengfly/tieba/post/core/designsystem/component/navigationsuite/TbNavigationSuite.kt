package com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.LocalShortNavigationBarOverride
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import com.huanchengfly.tieba.post.core.designsystem.component.DefaultNavigationBarOverride
import com.huanchengfly.tieba.post.core.designsystem.component.FloatingIconNavigationBarOverride
import com.huanchengfly.tieba.post.core.designsystem.component.FloatingNavigationBarOverride
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteType.Companion.toNavigationSuiteType

@ExperimentalMaterial3ComponentOverrideApi
@NonRestartableComposable
@Composable
fun TbNavigationSuite(
    tbNavigationSuiteType: TbNavigationSuiteType,
    modifier: Modifier = Modifier,
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    verticalArrangement: Arrangement.Vertical = NavigationSuiteDefaults.verticalArrangement,
    primaryActionContent: @Composable (() -> Unit) = {},
    content: @Composable () -> Unit,
) {
    val shortNavBarOverride = when (tbNavigationSuiteType) {
        TbNavigationSuiteType.FloatingNavigationBar -> FloatingNavigationBarOverride
        TbNavigationSuiteType.FloatingNavigationBarCompact -> FloatingIconNavigationBarOverride
        TbNavigationSuiteType.NavigationBar -> DefaultNavigationBarOverride
        else -> androidx.compose.material3.DefaultShortNavigationBarOverride
    }
    CompositionLocalProvider(LocalShortNavigationBarOverride provides shortNavBarOverride) {
        NavigationSuite(
            navigationSuiteType = tbNavigationSuiteType.toNavigationSuiteType(),
            modifier = modifier,
            colors = colors,
            verticalArrangement = verticalArrangement,
            primaryActionContent = primaryActionContent,
            content = content,
        )
    }
}
