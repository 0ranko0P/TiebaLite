package com.huanchengfly.tieba.post.ui.utils

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * [NestedScrollConnection] can determine attached scroll hierarchy is scrolling forward on
 * given [orientation].
 * */
class ScrollOrientationConnection(
    val orientation: Orientation
): NestedScrollConnection {

    var isScrollingForward by mutableStateOf(true)
        private set

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        when {
            orientation === Orientation.Vertical && available.y != 0.0f -> {
                isScrollingForward = available.y > 0f
            }

            orientation === Orientation.Horizontal && available.x != 0.0f -> {
                isScrollingForward = available.x < 0f
            }
        }

        return Offset.Zero
    }
}

@Composable
fun rememberScrollOrientationConnection(orientation: Orientation = Orientation.Vertical) =
    remember(orientation) {
        ScrollOrientationConnection(orientation = orientation)
    }
