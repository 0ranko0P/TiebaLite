package com.huanchengfly.tieba.post.core.ui.context

import androidx.compose.runtime.compositionLocalOf
import com.huanchengfly.tieba.post.core.data.model.settings.UISettings

val LocalUISettings = compositionLocalOf { UISettings() }