@file:Suppress("NOTHING_TO_INLINE")

package com.huanchengfly.tieba.post.core.common.ktx

inline fun Boolean.booleanToString(): String = if (this) "1" else "0"

inline fun Boolean.booleanToInt(): Int = if (this) 1 else 0