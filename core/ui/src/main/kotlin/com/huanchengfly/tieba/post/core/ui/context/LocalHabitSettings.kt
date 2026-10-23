package com.huanchengfly.tieba.post.core.ui.context

import androidx.compose.runtime.compositionLocalOf
import com.huanchengfly.tieba.post.core.data.model.settings.HabitSettings

val LocalHabitSettings = compositionLocalOf<HabitSettings> { error("No HabitSettings provided!") }