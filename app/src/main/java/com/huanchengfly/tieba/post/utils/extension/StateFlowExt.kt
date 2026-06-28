package com.huanchengfly.tieba.post.utils.extension

import kotlinx.coroutines.flow.MutableStateFlow

inline fun <T> MutableStateFlow<T>.set(block: T.() -> T) {
    this.value = this.value.block()
}
