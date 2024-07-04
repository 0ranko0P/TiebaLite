package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    private val spec: AnimationSpec<Float> = TweenSpec()
) : NestedScrollConnection {

    companion object {
        private val COLLAPSE_THRESHOLD = 12.dp
    }

    private val threshold = with(density) { COLLAPSE_THRESHOLD.toPx() }

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
    return remember { CollapseScrollConnection(density, scope, orientation) }
}

@Composable
fun CollapseScrollable(
    modifier: Modifier = Modifier,
    connection: CollapseScrollConnection = rememberCollapseConnection(),
    content: @Composable BoxScope.(Float) -> Unit
) = Box(modifier.nestedScroll(connection)) {
    content(connection.ratio)
}
