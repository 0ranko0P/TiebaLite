package com.huanchengfly.tieba.post.core.ui.modifier

import androidx.compose.ui.Modifier

inline fun Modifier.block(modifier: Modifier.() -> Modifier?): Modifier = this then (Modifier.modifier() ?: Modifier)

inline fun Modifier.onCase(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) this.modifier() else this
}

inline fun <T> Modifier.onNotNull(obj: T?, modifier: Modifier.(T) -> Modifier): Modifier {
    return if (obj != null) this.modifier(obj) else this
}

inline fun <T1, T2> Modifier.onNotNull(
    obj1: T1?,
    obj2: T2?,
    modifier: Modifier.(Pair<T1, T2>) -> Modifier
): Modifier {
    return if (obj1 != null && obj2 != null) this.modifier(obj1 to obj2) else this
}

inline fun <T> Modifier.withNonNull(obj: T?, modifier: T.() -> Modifier): Modifier {
    return if (obj != null) this then obj.modifier() else this
}
