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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.core.designsystem.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.core.designsystem.theme.LocalExtendedColorScheme
import com.huanchengfly.tieba.post.core.designsystem.util.isTranslucent
import com.huanchengfly.tieba.post.core.ui.context.LocalUISettings
import com.huanchengfly.tieba.post.core.ui.modifier.withNonNull
import com.huanchengfly.tieba.post.core.ui.util.DisplayUtil.GESTURE_3BUTTON
import com.huanchengfly.tieba.post.core.ui.util.DisplayUtil.GESTURE_DEFAULT
import com.huanchengfly.tieba.post.core.ui.util.DisplayUtil.GESTURE_NONE
import com.huanchengfly.tieba.post.core.ui.util.DisplayUtil.gestureType
import com.huanchengfly.tieba.post.core.ui.util.ThemeUtil
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

val LocalHazeState = staticCompositionLocalOf<TbHazeState?> { null }

@Immutable
class TbHazeState(colorScheme: ExtendedColorScheme) {

    val state: HazeState = HazeState()

    val hazeStyle: HazeStyle = colorScheme.buildDefaultHazeStyle()

    // Disable input scale on dark mode to reduce banding artifact
    val inputScale = if (colorScheme.darkTheme) HazeInputScale.None else DefaultHazeInputScale

    /***
     * Default scrim used whenever background blur is disabled.
     * */
    val fallbackTint = HazeTint(color = colorScheme.colorScheme.surface)

    fun Modifier.defaultHazeEffect(
        style: HazeStyle = hazeStyle,
        fallbackTint: HazeTint = this@TbHazeState.fallbackTint,
        block: (HazeEffectScope.() -> Unit)? = null,
    ): Modifier = this.hazeEffect(state, style) {
        if (block != null) {
            block()
        }
        this.inputScale = this@TbHazeState.inputScale
        this.fallbackTint = if (blurEnabled) HazeTint.Unspecified else fallbackTint
    }

    companion object {
        private val DefaultHazeInputScale: HazeInputScale = HazeInputScale.Fixed(0.033f)

        private fun ExtendedColorScheme.buildDefaultHazeStyle() = HazeStyle(
            backgroundColor = colorScheme.surfaceContainer,
            tint = null,
            blurRadius = 28.dp,
            noiseFactor = if (darkTheme) 0.2f else 0f // Reduce banding artifact on dark mode
        )
    }
}

@Composable
fun rememberTbHazeState(): TbHazeState {
    val colorSchemeExt = LocalExtendedColorScheme.current

    return remember(colorSchemeExt.colorScheme) { TbHazeState(colorSchemeExt) }
}

@ReadOnlyComposable
@Composable
fun isHazeBlurEnabled(): Boolean {
    return !MaterialTheme.colorScheme.isTranslucent && !LocalUISettings.current.reduceEffect
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
                    .withNonNull(LocalHazeState.current) {
                        Modifier.hazeEffect(state, style = hazeStyle)
                    }
                    .background(color = trackedColorScheme.navigationContainer)
            )
        }

        // 实体按键:
        GESTURE_NONE -> { /* Empty */ }
    }
}

@Composable
private inline fun TbHazeState.HazeBox(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    style: HazeStyle = hazeStyle,
    noinline hazeBlock: (HazeEffectScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier.defaultHazeEffect(style, block = hazeBlock),
        content = { content() }
    )
}

/**
 * Material Scaffold with real-time backdrop blurring effect on [topBar] and [bottomBar].
 *
 * @param modifier the [Modifier] to be applied to this scaffold
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
    topBar: @Composable () -> Unit = {},
    topHazeBlock: (HazeEffectScope.() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = BlurNavigationBarPlaceHolder,
    bottomHazeBlock: (HazeEffectScope.() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    snackbarHost: @Composable () -> Unit = { SnackbarHost(LocalSnackbarHostState.current) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    backgroundColor: Color = MaterialTheme.colorScheme.run { if (isTranslucent) Color.Transparent else background },
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    content: @Composable (PaddingValues) -> Unit
) {
    if (isHazeBlurEnabled()) {
        val hazeState = rememberTbHazeState()

        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalHazeState provides hazeState,
        ) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    hazeState.HazeBox(content = topBar, hazeBlock = topHazeBlock)
                },
                bottomBar = if (bottomBar === BlurNavigationBarPlaceHolder) {
                    bottomBar
                } else {
                    { hazeState.HazeBox(content = bottomBar, hazeBlock = bottomHazeBlock) }
                },
                snackbarHost = snackbarHost,
                floatingActionButton = floatingActionButton,
                floatingActionButtonPosition = floatingActionButtonPosition,
                containerColor = backgroundColor,
                contentColor = contentColor
            ) { paddingValues ->
                Box(modifier = Modifier.hazeSource(hazeState.state)) {
                    content(paddingValues)
                }
            }
        }
    } else {
        MyScaffold(
            modifier = modifier,
            useMD2Layout = MaterialTheme.colorScheme.isTranslucent,
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
