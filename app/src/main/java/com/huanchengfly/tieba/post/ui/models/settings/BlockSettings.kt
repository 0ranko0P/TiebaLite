package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable

@Immutable
data class BlockSettings(
    val blockVideo: Boolean = false,
    val hideBlocked: Boolean = true
)