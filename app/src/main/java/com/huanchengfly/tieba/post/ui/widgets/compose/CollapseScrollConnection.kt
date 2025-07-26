package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Collapsible [NestedScrollConnection] with an animated collapse ratio.
 *
 * @author 0Ranko0P
 * @see ratio
 * */
open class CollapseScrollConnection(
    private val scope: CoroutineScope,
    private val spec: AnimationSpec<Float> = TweenSpec(),
) : NestedScrollConnection {

    companion object {

        fun saver(
            scope: CoroutineScope,
        ): Saver<CollapseScrollConnection, *> = Saver(
            save = {
                packInts(val1 = it.state.ordinal, val2 = it.ratio.roundToInt())
            },
            restore = { saved: Long ->
                val state = CollapseState.entries[unpackInt1(value = saved)]
                val ratio = unpackInt2(value = saved).toFloat()

                CollapseScrollConnection(scope).apply {
                    this.state = state
                    this.ratio = ratio
                }
            }
        )

        enum class CollapseState {
            Expanded, Expanding, Collapsing, Collapsed
        }
    }

    var state = CollapseState.Expanded
        private set

    private val animator = Animatable(0f)

    /**
     * Animated collapse ratio, from 0f to -1f when [state] changes.
     * */
    var ratio by mutableFloatStateOf(0f)
        private set

    private fun isCollapseState(): Boolean = state.ordinal >= 2

    private fun isExpandState(): Boolean = state.ordinal <= 1

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return when {
            state == CollapseState.Expanding || state == CollapseState.Collapsing -> available

            source == NestedScrollSource.UserInput && available.y < 0f && isExpandState() -> {
                updateAnimation(CollapseState.Collapsed)
                available
            }

            else -> Offset.Zero
        }
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return when {
            state == CollapseState.Expanding || state == CollapseState.Collapsing -> available

            available.y > 0 && isCollapseState() -> {
                updateAnimation(CollapseState.Expanded)
                available
            }

            else -> Offset.Zero

        }
    }

    private fun updateAnimation(newState: CollapseState): Job = scope.launch (Dispatchers.Main) {
        val target: Float = if (newState == CollapseState.Expanded) 0f else -1.0f
        state = if (newState == CollapseState.Expanded) CollapseState.Expanding else CollapseState.Collapsing

        if (animator.isRunning) animator.stop()
        animator.animateTo(target, spec) {
            ratio = value
        }
        state = newState
    }
}

@Composable
fun rememberCollapseConnection(
    scope: CoroutineScope = rememberCoroutineScope(),
): CollapseScrollConnection {
    return rememberSaveable(
        saver = CollapseScrollConnection.saver(scope),
        init = {
            CollapseScrollConnection(scope)
        }
    )
}

@Composable
fun CollapseScrollable(
    modifier: Modifier = Modifier,
    connection: CollapseScrollConnection = rememberCollapseConnection(),
    content: @Composable BoxScope.(Float) -> Unit
) = Box(modifier.nestedScroll(connection)) {
    content(connection.ratio)
}
