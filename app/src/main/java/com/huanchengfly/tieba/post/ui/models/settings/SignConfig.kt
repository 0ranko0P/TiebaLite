package com.huanchengfly.tieba.post.ui.models.settings

data class SignConfig(
    val autoSign: Boolean = false,
    val autoSignSlow: Boolean = true,
    val autoSignTime: String,
    val okSignOfficial: Boolean = true,
    val ignoreBatteryOp: Boolean = false,
)