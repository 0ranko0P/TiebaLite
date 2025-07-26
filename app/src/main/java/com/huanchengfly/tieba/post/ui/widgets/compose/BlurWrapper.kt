package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
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
import com.huanchengfly.tieba.post.arch.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.navigationBar
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_3BUTTON
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_DEFAULT
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_NONE
import com.huanchengfly.tieba.post.utils.DisplayUtil.gestureType
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

// Override TopBar & BottomBar background for blurring effect
private fun ExtendedColors.overrideSysBarColor(): ExtendedColors =
    this.copy(topBar = topBar.copy(0.6f), bottomBar = bottomBar.copy(0.78f))

@OptIn(ExperimentalHazeApi::class)
val DefaultInputScale = HazeInputScale.Fixed(0.66f)

val defaultHazeStyle: HazeStyle
    @Composable
    get() {
        val colors = LocalExtendedColors.current
        return remember(colors.name) {
            HazeStyle(colors.windowBackground, null, blurRadius = 48.dp, noiseFactor = 0.15f)
        }
    }

/**
 * Placeholder to make [Scaffold] consume [WindowInsets.Companion.navigationBars]
 * */
val BlurNavigationBarPlaceHolder: @Composable () -> Unit = {
    val navBarInsets = WindowInsets.navigationBars
    when(navBarInsets.gestureType(LocalDensity.current)) {

        // 全面屏手势: 透明背景
        GESTURE_DEFAULT -> Spacer(modifier = Modifier.windowInsetsBottomHeight(navBarInsets))

        // 三大金刚: 背景和模糊滤镜
        GESTURE_3BUTTON ->  {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(insets = navBarInsets)
                    .block {
                        LocalHazeState.current?.let { hazeEffect(it, defaultHazeStyle) }
                    }
                    .background(LocalExtendedColors.current.navigationBar)
            )
        }

        // 实体按键:
        GESTURE_NONE -> { /* Empty */ }
    }
}

@Composable
inline fun BlurWrapper(
    modifier: Modifier = Modifier,
    hazeStyle: HazeStyle = defaultHazeStyle,
    noinline hazeBlock: (HazeEffectScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) = Box(
    modifier = modifier
        .hazeEffect(LocalHazeState.current, hazeStyle, hazeBlock),
    content = { content() }
)

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
    topHazeBlock: (HazeEffectScope.() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = BlurNavigationBarPlaceHolder,
    bottomHazeBlock: (HazeEffectScope.() -> Unit)? = null,
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

    if (!ThemeUtil.isTranslucentTheme(colors) && appPreferences.useRenderEffect) {
        val hazeState = remember { HazeState() }

        CompositionLocalProvider(
            LocalSnackbarHostState provides scaffoldState.snackbarHostState,
            LocalExtendedColors provides colors.overrideSysBarColor(),
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
                contentColor
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .block {
                            if (attachHazeContentState) hazeSource(hazeState) else null
                        }
                ) {
                    content(paddingValues)
                }
            }
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

private fun List<LazyListState?>.canScrollBackwardAt(index: Int): Boolean {
    return getOrNull(index)?.canScrollBackward == true
}

fun PagerState.enableBlur(children: List<LazyListState?>): Boolean {
    return when {
        currentPageOffsetFraction == 0f -> children.canScrollBackwardAt(currentPage)

        // Pager is scrolling forward, check current child and next child
        currentPageOffsetFraction > 0f -> {
            children.canScrollBackwardAt(currentPage) || children.canScrollBackwardAt(currentPage + 1)
        }

        // Pager is scrolling backward, check current child and previous child
        currentPageOffsetFraction < 0f -> {
            children.canScrollBackwardAt(currentPage) || children.canScrollBackwardAt(currentPage - 1)
        }

        else -> false
    }
}
