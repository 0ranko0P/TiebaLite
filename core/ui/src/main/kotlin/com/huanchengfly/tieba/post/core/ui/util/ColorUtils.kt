package com.huanchengfly.tieba.post.core.ui.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Stable
fun Color.toHexString(): String {
    if (this == Color.Transparent) {
        return "#00FFFFFF"
    }
    return "#${Integer.toHexString(toArgb()).substring(2)}"
}