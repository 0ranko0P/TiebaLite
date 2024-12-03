package com.huanchengfly.tieba.post.arch

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
class ImmutableHolder<T>(val item: T) {
    operator fun component1(): T = item

    @Stable
    fun get(): T = item

    @Stable
    inline fun <R> get(getter: T.() -> R): R {
        return getter(item)
    }

    inline fun <R> getImmutable(getter: T.() -> R): ImmutableHolder<R> {
        return wrapImmutable(getter(item))
    }

    inline fun <R> getNullableImmutable(getter: T.() -> R?): ImmutableHolder<R>? {
        return getter(item)?.wrapImmutable()
    }

    inline fun <R> getImmutableList(getter: T.() -> List<R>): ImmutableList<ImmutableHolder<R>> {
        return getter(item).map { wrapImmutable(it) }.toImmutableList()
    }

    @Stable
    fun isNotNull(): Boolean = item != null

    @Stable
    fun <R> isNotNull(getter: T.() -> R): Boolean = getter(item) != null

    override fun toString(): String {
        return "ImmutableHolder(item=$item)"
    }
}

fun <T> ImmutableHolder<T>?.getOrNull(): T? = this?.get()

fun <T> wrapImmutable(item: T): ImmutableHolder<T> = ImmutableHolder(item)

@JvmName("wrapImmutableExt")
fun <T> T.wrapImmutable(): ImmutableHolder<T> = ImmutableHolder(this)

fun <T> List<T>.wrapImmutable(): ImmutableList<ImmutableHolder<T>> =
    map { wrapImmutable(it) }.toImmutableList()