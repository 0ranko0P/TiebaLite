package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.theme.LocalExtendedColorScheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_3BUTTON
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_DEFAULT
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_NONE
import com.huanchengfly.tieba.post.utils.DisplayUtil.gestureType
import com.huanchengfly.tieba.post.utils.ThemeUtil
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

@ReadOnlyComposable
@Composable
fun defaultInputScale(): HazeInputScale {
    // Disable input scale on dark ColorScheme to avoid banding artifact
    return if (LocalExtendedColorScheme.current.darkTheme) HazeInputScale.None else HazeInputScale.Fixed(0.33f)
}

@ReadOnlyComposable
@Composable
fun defaultHazeStyle(): HazeStyle {
    return with(LocalExtendedColorScheme.current) {
        HazeStyle(
            backgroundColor = colorScheme.surfaceContainer,
            tint = null,
            blurRadius = 28.dp,
            noiseFactor = if (darkTheme) 0.2f else 0f // Reduce banding artifact on dark mode
        )
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
                        hazeEffect(state = haze, style = defaultHazeStyle())
                    }
                    .background(color = trackedColorScheme.navigationContainer)
            )
        }

        // 实体按键:
        GESTURE_NONE -> { /* Empty */ }
    }
}

@Composable
inline fun BlurWrapper(
    modifier: Modifier = Modifier,
    state: HazeState? = LocalHazeState.current,
    style: HazeStyle = defaultHazeStyle(),
    content: @Composable () -> Unit,
    noinline hazeBlock: (HazeEffectScope.() -> Unit)? = null,
) {
    Box(modifier = modifier.hazeEffect(state, style, hazeBlock)) {
        content()
    }
}

/**
 * Material Scaffold with real-time backdrop blurring effect on [topBar] and [bottomBar].
 *
 * @param modifier the [Modifier] to be applied to this scaffold
 * @param attachHazeContentState whether or not [content] is captured as background
 *   content for [hazeEffect] child nodes.
 * @param hazeStyle the [HazeStyle] to be applied on [topBar] and [bottomBar]
 * @param topBar top app bar of the screen, typically a [TopAppBar]
 * @param topHazeBlock define the styling and visual properties for [TopAppBar]
 * @param bottomBar bottom bar of the screen, typically a [NavigationBar]
 * @param bottomHazeBlock define the styling and visual properties for [bottomBar]
 * @param snackbarHostState state of the [SnackbarHost]
 * @param snackbarHost component to host [Snackbar]s that are pushed to be shown via
 *   [SnackbarHostState.showSnackbar], typically a [SnackbarHost].
 * @param floatingActionButton Main action button of the screen, typically a [FloatingActionButton]
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition].
 * @param backgroundColor the color used for the background of this scaffold. Default
 *   is [Color.Transparent].
 * @param contentColor the preferred color for content inside this scaffold.
 * @param content content of the screen.
 *
 * @see LocalHazeState
 */
@Composable
fun BlurScaffold(
    modifier: Modifier = Modifier,
    attachHazeContentState: Boolean = true,
    hazeStyle: HazeStyle = defaultHazeStyle(),
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
        val hazeInputScale = defaultInputScale()

        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalHazeState provides hazeState,
        ) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    BlurWrapper(state = hazeState, style = hazeStyle, content = topBar) {
                        inputScale = hazeInputScale
                        topHazeBlock?.invoke(this)
                    }
                },
                bottomBar = if (bottomBar === BlurNavigationBarPlaceHolder) {
                    bottomBar
                } else {
                    {
                        BlurWrapper(state = hazeState, style = hazeStyle, content = bottomBar) {
                            inputScale = hazeInputScale
                            bottomHazeBlock?.invoke(this)
                        }
                    }
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
): Modifier = if (state == null) this else (this.hazeSource(state, zIndex, key))
