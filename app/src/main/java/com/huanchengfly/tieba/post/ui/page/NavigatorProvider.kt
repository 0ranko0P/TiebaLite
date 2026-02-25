@file:Suppress("NOTHING_TO_INLINE")

package com.huanchengfly.tieba.post.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import kotlin.reflect.KClass

val LocalNavController = staticCompositionLocalOf<NavController> { error("No navigator is available") }

@NonSkippableComposable
@Composable
fun ProvideNavigator(
    navigator: NavController,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNavController provides navigator, content = content)
}

inline fun <T> NavController.setResult(key: String, value: T) {
    previousBackStackEntry?.savedStateHandle?.set(key, value)
}

inline fun <R : Any, T> NavController.consumeResult(route: KClass<R>, key: String): T? {
    val savedStateHandle = getBackStackEntry(route = route).savedStateHandle
    return savedStateHandle.remove(key)
}
