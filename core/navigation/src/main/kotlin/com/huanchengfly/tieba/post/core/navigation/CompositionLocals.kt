package com.huanchengfly.tieba.post.core.navigation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalBackButtonState: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf {
    true
}

val LocalNavigator: ProvidableCompositionLocal<Navigator> = staticCompositionLocalOf {
    error("No navigator is available")
}