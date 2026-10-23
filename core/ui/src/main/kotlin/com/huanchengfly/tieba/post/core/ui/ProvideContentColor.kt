package com.huanchengfly.tieba.post.core.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.ui.graphics.Color

@Composable
@NonSkippableComposable
fun ProvideContentColor(color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides color, content)
}