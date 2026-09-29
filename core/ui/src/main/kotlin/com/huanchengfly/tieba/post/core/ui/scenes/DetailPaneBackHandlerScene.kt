package com.huanchengfly.tieba.post.core.ui.scenes

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope

internal class DetailPaneBackHandlerScene<T : Any>(
    private val scene: Scene<T>,
    private val onNavBack: () -> Unit,
) : Scene<T> by scene {
    override val key = scene::class to scene.key

    override val content: @Composable () -> Unit = {
        scene.content()
        if (scene.entries.size > 1) {
            BackHandler(onBack = onNavBack)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetailPaneBackHandlerScene<*>) return false
        return scene == other.scene
    }

    override fun hashCode(): Int {
        return scene.hashCode()
    }
}

/**
 * A [SceneDecoratorStrategy] that handles back button when ListDetailSceneStrategy is chosen.
 *
 * @property onNavBack callback to be invoked when the system back button is pressed.
 */
class DetailPaneBackHandlerSceneDecoratorStrategy<T : Any>(
    private val onNavBack: () -> Unit,
) : SceneDecoratorStrategy<T> {

    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> {
        return if (scene.metadata.contains(METADATA_KEY)) {
            DetailPaneBackHandlerScene(scene, onNavBack)
        } else {
            scene
        }
    }

    companion object {
        internal const val METADATA_KEY = "com.huanchengfly.tieba.post.core.ui.scenes.DetailPaneBackHandler"

        fun backHandler(): Map<String, Any> = mapOf(METADATA_KEY to true)
    }
}

@Composable
fun <T : Any> rememberDetailPaneBackHandlerSceneDecoratorStrategy(
    onBack: () -> Unit
): DetailPaneBackHandlerSceneDecoratorStrategy<T> {
    return remember { DetailPaneBackHandlerSceneDecoratorStrategy(onBack) }
}
