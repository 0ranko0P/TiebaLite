package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.WidthSizeClasses

@Composable
fun containerWidthFraction(): State<Float> = derivedMediaQuery {
    when {
        windowWidth >= WindowSizeClass.WidthSizeClasses.Expanded -> 0.75f
        windowWidth >= WindowSizeClass.WidthSizeClasses.Medium -> 0.87f
        else -> 1f
    }
}

@Composable
@NonRestartableComposable
fun Container(
    modifier: Modifier = Modifier,
    fluid: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    if (fluid) {
        Box(
            modifier = modifier.fillMaxWidth(),
            content = content
        )
    } else {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val widthFraction by containerWidthFraction()
            Box(modifier = modifier.fillMaxWidth(widthFraction), content = content)
        }
    }
}

@Composable
fun OneTimeMeasurer(modifier: Modifier = Modifier, content: @Composable BoxScope.(IntSize?) -> Unit) {
    var size: IntSize? by remember { mutableStateOf(null) }

    Box(
        modifier = if (size == null) modifier.onSizeChanged { size = it } else modifier
    ) {
        content(size)
    }
}

// un-inlined box
@Composable
fun StrongBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier, content = content)
}
