package com.huanchengfly.tieba.post.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.HorizontalRuler
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.NavigationBarHeight

val floatingNavigationBarCompactScreenOffset: Dp
    @Composable
    get() = if (WindowInsets.navigationBars.getBottom(LocalDensity.current) > 0) {
        FloatingToolbarDefaults.ScreenOffset / 4
    } else {
        FloatingToolbarDefaults.ScreenOffset
    }

private val FloatingNavigationBarElevation: Dp = 1.dp

private val FloatingIconNavigationBarHeight = NavigationBarHeight

val ColorScheme.vibrantFloatingNavigationBarColor: Color
    get() = surfaceColorAtElevation(4.dp)

val ColorScheme.vibrantFloatingNavigationBarContentColor: Color
    get() = onSurface

private fun Modifier.floatingNavBarContainer(
    height: Dp,
    screenOffset: Dp,
): Modifier {
    return this then Modifier
        .heightIn(min = height)
        .clickable(interactionSource = null, indication = null, onClick = {})
        .selectableGroup()
        .graphicsLayer {
            this.shape = CircleShape
            this.clip = true
            this.translationY = -screenOffset.toPx()
            this.shadowElevation = FloatingNavigationBarElevation.toPx()
        }
}

@Composable
fun FloatingNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = ShortNavigationBarDefaults.containerColor,
    contentColor: Color = ShortNavigationBarDefaults.contentColor,
    windowInsets: WindowInsets = ShortNavigationBarDefaults.windowInsets,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(windowInsets)
                .padding(horizontal = 16.dp)
                .floatingNavBarContainer(
                    height = NavigationBarHeight,
                    screenOffset = floatingNavigationBarCompactScreenOffset,
                )
                .then(modifier)
                .background(color = containerColor, shape = CircleShape)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
fun FloatingIconNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = ShortNavigationBarDefaults.containerColor,
    contentColor: Color = ShortNavigationBarDefaults.contentColor,
    windowInsets: WindowInsets = ShortNavigationBarDefaults.windowInsets,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(windowInsets)
                .floatingNavBarContainer(
                    height = FloatingIconNavigationBarHeight,
                    screenOffset = floatingNavigationBarCompactScreenOffset,
                )
                .then(modifier)
                .background(color = containerColor, shape = CircleShape)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}

private val BadgeTopRuler = HorizontalRuler()
private val BadgeEndRuler = VerticalRuler()

internal fun Modifier.badgeBounds() =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            width = placeable.width,
            height = placeable.height,
            rulers = {
                // use provides instead of provideRelative cause we will place relative
                // in the badge code
                BadgeEndRuler provides coordinates.size.width.toFloat()
                BadgeTopRuler provides 0f
            },
        ) {
            placeable.place(0, 0)
        }
    }
