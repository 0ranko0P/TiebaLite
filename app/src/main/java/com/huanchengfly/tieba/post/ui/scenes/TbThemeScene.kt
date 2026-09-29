package com.huanchengfly.tieba.post.ui.scenes

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.core.navigation.LocalBackButtonState
import com.huanchengfly.tieba.post.theme.LocalExtendedColorScheme
import com.huanchengfly.tieba.post.theme.isTranslucent

// TODO: Move to core:ui
data class TbThemeScene<T : Any>(
    private val directive: PaneScaffoldDirective,
    private val scene: Scene<T>,
    private val sharedTransitionScope: SharedTransitionScope?,
) : Scene<T> by scene {
    override val key = scene::class to scene.key

    override val content: @Composable () -> Unit = {
        val isListDetail = directive.maxHorizontalPartitions > 1
        var colorSchemeExt = LocalExtendedColorScheme.current
        val colorScheme = colorSchemeExt.colorScheme
        var uiSettings = LocalUISettings.current
        // Reduce effect on list detail mode
        if (isListDetail && !uiSettings.reduceEffect) {
            uiSettings = uiSettings.copy(reduceEffect = true)
        }
        // Disable AppBar container color animation on list detail mode
        if (isListDetail && !colorScheme.isTranslucent) {
            colorSchemeExt = colorSchemeExt.copy(
                appBarColors = colorSchemeExt.appBarColors.copy(scrolledContainerColor = colorScheme.surface),
                navigationContainer = colorScheme.surfaceContainer,
            )
        }

        CompositionLocalProvider(
            LocalBackButtonState provides (scene.entries.size <= 1),
            LocalUISettings provides uiSettings,
            LocalExtendedColorScheme provides colorSchemeExt,
            content = scene.content
        )
    }
}

@Composable
fun <T : Any> rememberTbThemeSceneDecoratorStrategy(
    directive: PaneScaffoldDirective,
    sharedTransitionScope: SharedTransitionScope,
): TbThemeSceneDecoratorStrategy<T> {
    return remember(directive, sharedTransitionScope) {
        TbThemeSceneDecoratorStrategy(directive, sharedTransitionScope)
    }
}

class TbThemeSceneDecoratorStrategy<T : Any>(
    private val directive: PaneScaffoldDirective,
    private val sharedTransitionScope: SharedTransitionScope,
) : SceneDecoratorStrategy<T> {

    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> {
        return TbThemeScene(directive, scene, sharedTransitionScope)
    }
}