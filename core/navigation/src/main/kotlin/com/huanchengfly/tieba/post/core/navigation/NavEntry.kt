package com.huanchengfly.tieba.post.core.navigation

import androidx.navigation3.runtime.NavKey

/** Keep sync with [androidx.navigation3.runtime.defaultContentKey] */
val NavKey.defaultContentKey: Any
    get() = Pair("$this", "${this::class}")
