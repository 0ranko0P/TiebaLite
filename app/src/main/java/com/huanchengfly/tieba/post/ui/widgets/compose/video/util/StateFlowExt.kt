package com.huanchengfly.tieba.post.ui.widgets.compose.video.util

import kotlinx.coroutines.flow.MutableStateFlow

inline fun <T> MutableStateFlow<T>.set(block: T.() -> T) {
    this.value = this.value.block()
}