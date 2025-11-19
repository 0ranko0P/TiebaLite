package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_3BUTTON
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_DEFAULT
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_NONE
import com.huanchengfly.tieba.post.utils.DisplayUtil.gestureType
import com.huanchengfly.tieba.post.utils.ThemeUtil
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

@OptIn(ExperimentalHazeApi::class)
val DefaultInputScale = HazeInputScale.Fixed(0.66f)

val defaultHazeStyle: HazeStyle
    @Composable get() {
        val colors = MaterialTheme.colorScheme
        return remember(colors) {
            HazeStyle(colors.surfaceContainer, null, blurRadius = 48.dp, noiseFactor = 0.15f)
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
            val trackedColorScheme by ThemeUtil.colorState
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(insets = navBarInsets)
                    .onNotNull(LocalHazeState.current) { haze ->
                        hazeEffect(state = haze, style = defaultHazeStyle)
                    }
                    .background(color = trackedColorScheme.navigationContainer)
            )
        }

        // 实体按键:
        GESTURE_NONE -> { /* Empty */ }
    }
}

@Composable
@NonRestartableComposable
fun BlurWrapper(
    modifier: Modifier = Modifier,
    hazeStyle: HazeStyle = defaultHazeStyle,
    hazeBlock: (HazeEffectScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.hazeEffect(LocalHazeState.current, hazeStyle, hazeBlock)) {
        content()
    }
}

/**
 * Scaffold which lays out [content] behind both the top bar and the bottom bar content with Haze
 * background blurring.
 *
 * @see [LocalHazeState]
 * */
@Composable
fun BlurScaffold(
    modifier: Modifier = Modifier,
    attachHazeContentState: Boolean = true,
    hazeStyle: HazeStyle = defaultHazeStyle,
    topBar: @Composable () -> Unit = {},
    topHazeBlock: (HazeEffectScope.() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = BlurNavigationBarPlaceHolder,
    bottomHazeBlock: (HazeEffectScope.() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    snackbarHost: @Composable () -> Unit = { SnackbarHost(LocalSnackbarHostState.current) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    content: @Composable (PaddingValues) -> Unit
) {
    val isTranslucent = MaterialTheme.colorScheme.isTranslucent
    if (!isTranslucent && !LocalUISettings.current.reduceEffect) {
        val hazeState = remember { HazeState() }
        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalHazeState provides hazeState,
        ) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    BlurWrapper(hazeStyle = hazeStyle, hazeBlock = topHazeBlock, content = topBar)
                },
                bottomBar = if (bottomBar === BlurNavigationBarPlaceHolder) {
                    bottomBar
                } else {
                    { BlurWrapper(hazeStyle = hazeStyle, hazeBlock = bottomHazeBlock, content = bottomBar) }
                },
                snackbarHost = snackbarHost,
                floatingActionButton = floatingActionButton,
                floatingActionButtonPosition = floatingActionButtonPosition,
                containerColor = backgroundColor,
                contentColor = contentColor
            ) { paddingValues ->
                if (attachHazeContentState) {
                    Box(Modifier.hazeSource(hazeState), content = { content(paddingValues) })
                } else {
                    content(paddingValues)
                }
            }
        }
    } else {
        MyScaffold(
            modifier = modifier,
            useMD2Layout = isTranslucent,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHostState = snackbarHostState,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            content = content
        )
    }
}

@Stable
fun Modifier.hazeSource(
    state: HazeState?,
    zIndex: Float = 0f,
    key: Any? = null,
): Modifier =
    if (state == null) Modifier else (this.hazeSource(state, zIndex, key))

private fun List<LazyListState?>.canScrollBackwardAt(index: Int): Boolean {
    return getOrNull(index)?.canScrollBackward == true
}
