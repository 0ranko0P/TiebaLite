package com.huanchengfly.tieba.post.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

val LocalNavController = staticCompositionLocalOf<NavController> { error("No navigator is available") }

@Composable
fun ProvideNavigator(
    navigator: NavController,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNavController provides navigator, content = content)
}