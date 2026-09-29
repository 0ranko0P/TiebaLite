package com.huanchengfly.tieba.post.core.common.ktx

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Iterates through a [List] in reverse order using the index and calls [action] for each item. This
 * does not allocate an iterator like [Iterable.forEachIndexed].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 *
 * @see androidx.compose.ui.util.fastForEachIndexed
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastForEachReverseIndexed(action: (Int, T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices.reversed()) {
        val item = get(index)
        action(index, item)
    }
}

/**
 * Returns the index of first element matching the given [predicate], or -1 if no such element was
 * found. This does not allocate an iterator like [Iterable.forEach].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastIndexOf(predicate: (T) -> Boolean): Int {
    contract { callsInPlace(predicate) }
    for (index in indices) {
        if (predicate(this[index])) return index
    }
    return -1
}

/**
 * Returns the index of last element matching the given [predicate], or -1 if no such element was
 * found. This does not allocate an iterator like [Iterable.forEach].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 *
 * @see androidx.compose.ui.util.fastLastOrNull
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastIndexOfLast(predicate: (T) -> Boolean): Int {
    contract { callsInPlace(predicate) }
    for (index in indices.reversed()) {
        if (predicate(this[index])) return index
    }
    return -1
}
