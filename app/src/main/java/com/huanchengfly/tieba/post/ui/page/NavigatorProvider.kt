package com.huanchengfly.tieba.post.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

val LocalNavController = staticCompositionLocalOf<NavController> { error("No navigator is available") }

@NonSkippableComposable
@Composable
fun ProvideNavigator(
    navigator: NavController,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNavController provides navigator, content = content)
}

fun <T> NavController.navigateBackWithResult(key: String, value: T) {
    previousBackStackEntry?.savedStateHandle?.set(key, value)
    navigateUp()
}

fun <T> NavController.consumeResult(key: String): T? {
    val savedStateHandle = currentBackStackEntry?.savedStateHandle ?: return null
    return savedStateHandle.remove(key)
}
