package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

open class ScrollStateConnection(
    val orientation: Orientation = Orientation.Vertical
): NestedScrollConnection {

    private val _isScrolling = mutableStateOf(false)
    val isScrolling: State<Boolean>
        get() = _isScrolling

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val availableAt = if (orientation == Orientation.Vertical) available.y else available.x
        if (availableAt != 0f) {
            _isScrolling.value = true
        } else {
            _isScrolling.value = false
        }

        return Offset.Zero
    }
}

@Composable
fun rememberScrollStateConnection(orientation: Orientation = Orientation.Vertical) =
    remember(orientation) {
        ScrollStateConnection(orientation = orientation)
    }