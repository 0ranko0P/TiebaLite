package com.huanchengfly.tieba.post.core.common.ktx

/** @see [kotlin.UnsafeLazyImpl] */
fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)