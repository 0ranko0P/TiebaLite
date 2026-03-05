package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.material3.FloatingToolbarState
import androidx.compose.ui.unit.dp

internal val ExtendedFabHeight = 56.dp
internal val ToolbarToFabGap = 8.dp

internal val FloatingToolbarState.collapsedFraction: Float
    get() = if (offsetLimit != 0f) {
        offset / offsetLimit
    } else {
        0f
    }
