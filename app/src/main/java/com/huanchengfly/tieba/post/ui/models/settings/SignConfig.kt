package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable

@Immutable
data class SignConfig(
    val autoSign: Boolean = false,
    val autoSignSlow: Boolean = true,
    val autoSignTime: String = "09:00",
    val okSignOfficial: Boolean = true,
)