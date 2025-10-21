package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.isTranslucent

// Workaround to enable background blurring on StickyHeader
@Composable @ReadOnlyComposable
fun useStickyHeaderWorkaround(): Boolean = !MaterialTheme.colorScheme.isTranslucent

/**
 * Workaround to enable background blurring on StickyHeader.
 *
 * This composes [header] as overlay inside the TopAppBar when first item becomes invisible.
 * */
@Composable
fun StickyHeaderOverlay(state: LazyListState, header: @Composable () -> Unit) {
    val shouldPinOnTopBar by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
    if (shouldPinOnTopBar) {
        header()
    }
}

/**
 * Workaround to apply TopAppBar container color changes on StickyHeader
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun containerColorNoAni(appBarState: TopAppBarState, listState: LazyListState): State<Color>  {
    val topAppBarColors = TiebaLiteTheme.topAppBarColors
    return remember {
        derivedStateOf {
            // double check, LazyListState#scrollToItem might cause buggy TopAppbarState
            if (appBarState.overlappedFraction > 0.01f && listState.firstVisibleItemIndex > 0) {
                topAppBarColors.scrolledContainerColor
            } else {
                topAppBarColors.containerColor
            }
        }
    }
}
