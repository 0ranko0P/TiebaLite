package com.huanchengfly.tieba.post.core.ui.util

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

suspend fun ScrollableState.scrollToTop(maxAniScrollItems: Int = 8) {
    when(this) {
        is LazyListState -> {
            if (firstVisibleItemIndex > maxAniScrollItems) scrollToItem(0) else animateScrollToItem(0)
        }

        is LazyGridState -> {
            if (firstVisibleItemIndex > maxAniScrollItems) scrollToItem(0) else animateScrollToItem(0)
        }

        is LazyStaggeredGridState -> {
            if (firstVisibleItemIndex > maxAniScrollItems) scrollToItem(0) else animateScrollToItem(0)
        }

        else -> withTimeout(2.seconds) {
            scrollBy(Float.MAX_VALUE)
        }
    }
}