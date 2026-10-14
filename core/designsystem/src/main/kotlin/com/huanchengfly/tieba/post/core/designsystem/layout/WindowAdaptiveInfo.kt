package com.huanchengfly.tieba.post.core.designsystem.layout

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.staticCompositionLocalOf

val LocalWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo> {
    error("No WindowAdaptiveInfo provided!")
}