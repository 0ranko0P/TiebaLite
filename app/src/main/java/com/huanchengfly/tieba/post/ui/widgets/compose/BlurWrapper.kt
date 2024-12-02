package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.DrawerDefaults
import androidx.compose.material.FabPosition
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.ui.Scaffold
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_3BUTTON
import com.huanchengfly.tieba.post.utils.DisplayUtil.gestureType
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import dev.chrisbanes.haze.HazeChildScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

// Override TopBar & BottomBar background for blurring effect
private fun ExtendedColors.overrideSysBarColor(blur: Boolean): ExtendedColors = if (blur) {
    this.copy(topBar = topBar.copy(0.6f), bottomBar = bottomBar.copy(0.78f))
} else {
    this.copy(topBar = topBar.copy(0.98f), bottomBar = bottomBar.copy(0.98f))
}

val defaultHazeStyle: HazeStyle
    @Composable
    get() {
        val colors = LocalExtendedColors.current
        return remember(colors.name) {
            HazeStyle(colors.windowBackground, null, blurRadius = 48.dp, noiseFactor = 0.15f)
        }
    }

/**
 * [NavigationBarPlaceHolder] with background blur
 * */
val BlurNavigationBarPlaceHolder: @Composable () -> Unit = {
    val hazeState = LocalHazeState.current
    val navBarInsets = WindowInsets.navigationBars

    // Check navBar gesture type, enable blurring conditionally
    if (hazeState != null && navBarInsets.gestureType(LocalDensity.current) == GESTURE_3BUTTON) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(navBarInsets)
                .hazeChild(state = LocalHazeState.current!!, style = defaultHazeStyle)
                .background(color = LocalExtendedColors.current.bottomBar)
        )
    } else {
        NavigationBarPlaceHolder()
    }
}

@Composable
inline fun BlurWrapper(
    modifier: Modifier = Modifier,
    hazeStyle: HazeStyle = defaultHazeStyle,
    noinline hazeBlock: (HazeChildScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val hazeState = LocalHazeState.current

    if (hazeState != null) {
        Box(modifier = modifier.hazeChild(hazeState, hazeStyle, hazeBlock)) { content() }
    } else {
        Box(modifier = modifier) { content() }
    }
}

/**
 * Scaffold which lays out [content] behind both the top bar and the bottom bar content with Haze
 * background blurring.
 *
 * @see [com.google.accompanist.insets.ui.Scaffold]
 * @see [LocalHazeState]
 * */
@Composable
fun BlurScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    attachHazeContentState: Boolean = true,
    hazeStyle: HazeStyle = defaultHazeStyle,
    topBar: @Composable () -> Unit = {},
    topHazeBlock: (HazeChildScope.() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = BlurNavigationBarPlaceHolder,
    bottomHazeBlock: (HazeChildScope.() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SwipeToDismissSnackbarHost(it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    drawerScrimColor: Color = DrawerDefaults.scrimColor,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (PaddingValues) -> Unit
) {
    val appPreferences = LocalContext.current.appPreferences
    val colors = LocalExtendedColors.current

    // Disable blurring on translucent theme
    if (!ThemeUtil.isTranslucentTheme(colors) && !appPreferences.reduceEffect) {
        val hazeState = remember {
            if (appPreferences.useRenderEffect) HazeState() else null
        }

        CompositionLocalProvider(
            LocalSnackbarHostState provides scaffoldState.snackbarHostState,
            LocalExtendedColors provides colors.overrideSysBarColor(hazeState != null),
            LocalHazeState provides hazeState,
        ) {
            Scaffold(
                modifier,
                scaffoldState,
                topBar = {
                    BlurWrapper(hazeStyle = hazeStyle, hazeBlock = topHazeBlock, content = topBar)
                },
                bottomBar = if (bottomBar === BlurNavigationBarPlaceHolder) {
                    bottomBar
                } else { {
                    BlurWrapper(hazeStyle = hazeStyle, hazeBlock = bottomHazeBlock, content = bottomBar)
                } },
                snackbarHost,
                floatingActionButton,
                floatingActionButtonPosition,
                isFloatingActionButtonDocked,
                drawerContent,
                drawerGesturesEnabled,
                drawerShape,
                drawerElevation,
                drawerBackgroundColor,
                drawerContentColor,
                drawerScrimColor,
                backgroundColor,
                contentColor,
                content = if (attachHazeContentState && hazeState != null) {
                    { padding -> Box(modifier = Modifier.haze(hazeState)) { content(padding) } }
                } else {
                    content
                }
            )
        }
    } else {
        MyScaffold(
            modifier,
            scaffoldState,
            topBar,
            bottomBar,
            snackbarHost,
            floatingActionButton,
            floatingActionButtonPosition,
            isFloatingActionButtonDocked,
            drawerContent,
            drawerGesturesEnabled,
            drawerShape,
            drawerElevation,
            drawerBackgroundColor,
            drawerContentColor,
            drawerScrimColor,
            backgroundColor,
            contentColor,
            content = content
        )
    }
}
