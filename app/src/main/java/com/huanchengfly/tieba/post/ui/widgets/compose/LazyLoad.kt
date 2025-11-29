package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun LazyLoad(
    loaded: Boolean,
    onLoad: () -> Unit,
) {
    if (loaded) return

    val curOnLoad by rememberUpdatedState(newValue = onLoad)
    LaunchedEffect(Unit) {
        curOnLoad()
    }
}

