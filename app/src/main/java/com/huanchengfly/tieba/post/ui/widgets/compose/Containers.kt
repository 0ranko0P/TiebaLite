package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass

@Composable
fun Container(
    modifier: Modifier = Modifier,
    fluid: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val windowWidthSizeClass = LocalWindowSizeClass.current.widthSizeClass
    val widthFraction = remember(windowWidthSizeClass) {
        if (fluid) {
            1f
        } else {
            when (windowWidthSizeClass) {
                WindowWidthSizeClass.Medium -> 0.87f
                WindowWidthSizeClass.Expanded -> 0.75f
                else -> 1f
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier.fillMaxWidth(widthFraction),
        ) {
            content()
        }
    }
}

@Composable
fun OneTimeMeasurer(modifier: Modifier = Modifier, content: @Composable BoxScope.(IntSize?) -> Unit) {
    var size: IntSize? by remember { mutableStateOf(null) }

    Box(
        modifier = if (size == null) modifier.onGloballyPositioned { size = it.size } else modifier
    ) {
        content(size)
    }
}