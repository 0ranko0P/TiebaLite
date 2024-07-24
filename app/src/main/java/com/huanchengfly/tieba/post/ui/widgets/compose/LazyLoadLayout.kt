package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.Drag
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * LazyColumn with swipe-up-to-refresh behaviour
 *
 * @param   onLoad Called when user performed a swipe up refresh, set Null to disable this behaviour
 * @param   onLazyLoad Called when the colum scrolls on end
 *
 * @see SwipeUpRefreshScrollConnection
 * @see [LazyListState.canScrollForward]
 * */
@Composable
fun SwipeUpLazyLoadColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    isLoading: Boolean,
    onLoad: (() -> Unit)?,
    onLazyLoad: (() -> Unit)? = null,
    bottomIndicator: @Composable BoxScope.(onThreshold: Boolean) -> Unit,
    items: LazyListScope.() -> Unit
) {
    val refreshState = rememberSwipeUpRefreshConnection(isLoading, onLoad)

    if (onLazyLoad != null) {
        val isAtBottom: Boolean by remember {
            derivedStateOf { !state.canScrollForward && state.firstVisibleItemIndex != 0 }
        }

        LaunchedEffect(isAtBottom) {
            if (isAtBottom && !isLoading) onLazyLoad()
        }
    }

    Box(modifier = modifier.nestedScroll(refreshState)) {
        OneTimeMeasurer(modifier = Modifier.align(Alignment.BottomCenter)) { size ->
            Box(
                modifier = Modifier
                    .offset { // offset with LazyColumn together
                        val height = size?.height ?: return@offset IntOffset.Zero
                        IntOffset(x = 0, y = height + refreshState.position.roundToInt())
                    }
                    // hide when measure indicator's height
                    .then(if (size == null) Modifier.alpha(0f) else Modifier)
            ) {
                val onThreshold by remember { derivedStateOf { refreshState.progress >= 1.0f } }
                bottomIndicator(onThreshold)
            }

            LaunchedEffect(size) { // set indicator's height as threshold
                if (size == null) return@LaunchedEffect
                refreshState.setThreshold(size.height.toFloat())
            }
        }

        LazyColumn(
            modifier = Modifier.offset { IntOffset(x = 0, y = refreshState.position.roundToInt()) },
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = items
        )
    }
}

private class SwipeUpRefreshScrollConnection(
    private val scope: CoroutineScope,
    threshold: Float,
    val onRefresh: (() -> Unit)?
) : NestedScrollConnection {

    private var _refreshing = false
    private var _offset by mutableFloatStateOf(0f)
    private var _threshold by mutableFloatStateOf(threshold)

    private var _position by mutableFloatStateOf(0f)
    private val adjustedDistancePulled by derivedStateOf { _offset * 0.5f }

    val threshold: Float get() = _threshold
    val position: Float get() = _position

    /**
     * A float representing how far the user has pulled as a percentage of the [threshold].
     *
     * If the component has not been pulled at all, progress is zero. If the pull has reached
     * halfway to the threshold, progress is 0.5f. A value greater than 1 indicates that pull has
     * gone beyond the refreshThreshold - e.g. a value of 2f indicates that the user has pulled to
     * two times the refreshThreshold.
     */
    val progress: Float get() = abs(adjustedDistancePulled / threshold)

    private val mutatorMutex = MutatorMutex()

    fun setThreshold(threshold: Float) {
        _threshold = -threshold
    }

    fun setRefreshing(refreshing: Boolean) {
        _refreshing = refreshing
        if (!refreshing && position < 0f) {
            animateIndicatorTo(0f)
        }
    }

    private fun animateIndicatorTo(offset: Float) = scope.launch {
        mutatorMutex.mutate {
            animate(initialValue = _position, targetValue = offset) { value, _ ->
                _position = value
            }
        }
    }

    private fun onSwipe(delta: Float): Float {
        val newOffset = (_offset + delta).coerceAtMost(0f)
        val dragConsumed = newOffset - _offset
        _offset = newOffset
        _position = calculateIndicatorPosition()
        return dragConsumed
    }

    private fun onRelease(velocity: Float): Float {
        if (onRefresh == null) {
            animateIndicatorTo(0f)
        } else if (!_refreshing) {
            if (adjustedDistancePulled < threshold) {
                onRefresh.invoke()
                animateIndicatorTo(threshold)
            } else {
                animateIndicatorTo(0f)
            }
        }

        val consumed = when {
            // We are flinging without having dragged the pull refresh (for example a fling inside
            // a list) - don't consume
            _offset == 0f -> 0f
            // If the velocity is negative, the fling is upwards, and we don't want to prevent the
            // the list from scrolling
            velocity < 0f -> 0f
            // We are showing the indicator, and the fling is downwards - consume everything
            else -> velocity
        }
        _offset = 0f
        return consumed
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = when {
        source == Drag && available.y > 0 -> Offset(0f, onSwipe(available.y)) // Swiping up
        else -> Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = when {
        source == Drag && available.y < 0 -> Offset(0f, onSwipe(available.y)) // Pulling down
        else -> Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return Velocity(0f, onRelease(available.y))
    }

    private fun calculateIndicatorPosition(): Float = when {
        // If drag hasn't gone past the threshold, the position is the adjustedDistancePulled.
        adjustedDistancePulled >= threshold -> adjustedDistancePulled
        else -> {
            // How far beyond the threshold pull has gone, as a percentage of the threshold.
            val overshootPercent = progress - 1.0f
            // Limit the overshoot to 200%. Linear between 0 and 200.
            val linearTension = overshootPercent.coerceIn(0f, 2f)
            // Non-linear tension. Increases with linearTension, but at a decreasing rate.
            val tensionPercent = linearTension - linearTension.pow(2) / 4
            // The additional offset beyond the threshold.
            val extraOffset = threshold * tensionPercent
            threshold + extraOffset
        }
    }
}

@Composable
private fun rememberSwipeUpRefreshConnection(
    refreshing: Boolean,
    onRefresh: (() -> Unit)?,
    refreshThreshold: Dp = 80.dp
): SwipeUpRefreshScrollConnection {
    require(refreshThreshold > 0.dp) { "The refresh trigger must be greater than zero!" }

    val scope = rememberCoroutineScope()
    val thresholdPx: Float

    with(LocalDensity.current) {
        thresholdPx = refreshThreshold.toPx()
    }
    val state = remember(scope) {
        SwipeUpRefreshScrollConnection(scope, thresholdPx, onRefresh)
    }

    SideEffect {
        state.setThreshold(thresholdPx)
        state.setRefreshing(refreshing)
    }

    return state
}