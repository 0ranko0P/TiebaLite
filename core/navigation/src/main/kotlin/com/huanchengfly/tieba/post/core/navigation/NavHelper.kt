package com.huanchengfly.tieba.post.core.navigation

import androidx.navigation3.runtime.NavKey
import com.huanchengfly.tieba.post.core.common.ktx.fastIndexOf
import com.huanchengfly.tieba.post.core.common.ktx.fastIndexOfLast
import kotlin.reflect.KClass

inline fun <reified T : NavKey> Navigator.popUpNavigate(key: NavKey, inclusive: Boolean = false) {
    val backStack = state.currentSubStack
    val index = backStack.indexOfLast { it is T }

    if (index != -1) {
        val retainCount = if (inclusive) index else index + 1
        if (backStack.size > retainCount) {
            backStack.subList(retainCount, backStack.size).clear()
        }
    }
    backStack.add(key)
}

inline fun <reified List : NavKey, reified Detail: NavKey> Navigator.containsScene(): Boolean {
    val backStack = state.currentSubStack
    if (backStack.size >= 2) {
        val index = backStack.fastIndexOfLast { it is List }
        return index != -1 && backStack.getOrNull(index + 1) is Detail
    } else {
        return false
    }
}

/**
 * Returns `true` if type of [navKey] is found in the list.
 */
inline fun <reified T: NavKey> List<KClass<out T>>.containsType(navKey: NavKey): Boolean {
    val navKeyClass = navKey::class
    return this.fastIndexOf { it == navKeyClass } != -1
}
