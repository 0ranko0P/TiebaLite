package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Collapsible [NestedScrollConnection] with an animated collapse ratio.
 *
 * @author 0Ranko0P
 * @see ratio
 * */
open class CollapseScrollConnection(
    density: Density,
    private val scope: CoroutineScope,
    private val orientation: Orientation,
    private val spec: AnimationSpec<Float> = TweenSpec(),
    val collapseThreshold: Dp = DEFAULT_COLLAPSE_THRESHOLD,
) : NestedScrollConnection {

    companion object {
        val DEFAULT_COLLAPSE_THRESHOLD = 18.dp

        fun saver(
            density: Density,
            scope: CoroutineScope,
            orientation: Orientation,
        ): Saver<CollapseScrollConnection, *> = Saver(
            save = {
                packInts(val1 = it.state.ordinal, val2 = it.ratio.roundToInt())
            },
            restore = { saved: Long ->
                val state = CollapseState.entries[unpackInt1(value = saved)]
                val ratio = unpackInt2(value = saved).toFloat()

                CollapseScrollConnection(density, scope, orientation).apply {
                    this.state = state
                    this.ratio = ratio
                }
            }
        )
    }

    private val threshold = with(density) { collapseThreshold.toPx() }

    private enum class CollapseState {
        Expanded, Expanding, Collapsing, Collapsed
    }

    private var state = CollapseState.Expanded

    val animator = Animatable(0f)

    /**
     * Animated collapse ratio, from 0f to -1f when [state] changes.
     * */
    var ratio by mutableFloatStateOf(0f)
        private set

    private fun isCollapseState(): Boolean = state.ordinal >= 2

    private fun isExpandState(): Boolean = state.ordinal <= 1

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta: Float = if (orientation == Orientation.Vertical) available.y else available.x

        if (delta > 0f && isCollapseState()) {
            updateAnimation(CollapseState.Expanded)
            return available
        } else if (delta < 0f && isExpandState()) {
            // Collapse only if hits threshold
            if (abs(delta) < threshold) return Offset.Zero
            updateAnimation(CollapseState.Collapsed)
            return available
        } else {
            return Offset.Zero
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
    orientation: Orientation = Orientation.Vertical
): CollapseScrollConnection {
    val density = LocalDensity.current
    return rememberSaveable(
        orientation,
        saver = CollapseScrollConnection.saver(density, scope, orientation)
    ) {
        CollapseScrollConnection(density, scope, orientation)
    }
}

@Composable
fun CollapseScrollable(
    modifier: Modifier = Modifier,
    connection: CollapseScrollConnection = rememberCollapseConnection(),
    content: @Composable BoxScope.(Float) -> Unit
) = Box(modifier.nestedScroll(connection)) {
    content(connection.ratio)
}
