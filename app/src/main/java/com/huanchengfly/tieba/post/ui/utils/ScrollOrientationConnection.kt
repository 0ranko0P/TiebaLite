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

    var isScrollingForward by mutableStateOf(false)
        private set

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available != OffsetZero && available != Offset.Zero) {
            isScrollingForward = when (orientation) {
                Orientation.Vertical -> available.y > 0f

                Orientation.Horizontal -> available.x < 0f
            }
        }

        return Offset.Zero
    }
}

// no comment
private val OffsetZero = Offset(-0.0f, -0.0f)

@Composable
fun rememberScrollOrientationConnection(orientation: Orientation = Orientation.Vertical) =
    remember(orientation) {
        ScrollOrientationConnection(orientation = orientation)
    }
