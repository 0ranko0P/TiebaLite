package com.huanchengfly.tieba.post.ui.page.settings

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.scene.SceneStrategy
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon

data class SettingsSceneDecorator<T : Any>(
    private val scene: Scene<T>,
    private val title: Int,
) : Scene<T> by scene {
    override val key = scene::class to scene.key

    override val content: @Composable (() -> Unit) = {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(title),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher?.let { dispatcher ->
                        BackNavigationIcon(onBackPressed = dispatcher::onBackPressed)
                    }
                },
            )
            Box(Modifier.weight(1f).consumeWindowInsets(TopAppBarDefaults.windowInsets)) {
                scene.content()
            }
        }
    }
}

/**
 * A [SceneStrategy] that returns a [SettingsSceneDecorator] if there is a scene that wants to
 * display settings title.
 */
class SettingsSceneDecoratorStrategy<T : Any> : SceneDecoratorStrategy<T> {

    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> {
        val (title, alwaysVisible) = scene.metadata[SettingsDecoratorKey] ?: return scene
        return if (alwaysVisible || scene.entries.size > 1) {
            SettingsSceneDecorator(scene, title)
        } else {
            scene
        }
    }

    companion object {
        internal object SettingsDecoratorKey: NavMetadataKey<Pair<Int, Boolean>>

        fun decorate(@StringRes title: Int, alwaysVisible: Boolean = false) = metadata {
            put(SettingsDecoratorKey, title to alwaysVisible)
        }
    }
}

@Composable
fun <T : Any> rememberSettingsSceneStrategy(): SettingsSceneDecoratorStrategy<T> {
    return remember {
        SettingsSceneDecoratorStrategy()
    }
}
