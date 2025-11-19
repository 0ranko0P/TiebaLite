package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.utils.HmTime

@Immutable
data class SignConfig(
    val autoSign: Boolean = false,
    val autoSignSlow: Boolean = true,
    val autoSignTime: HmTime = randomSignTime(),
    val okSignOfficial: Boolean = true,
)

fun randomSignTime(): HmTime = HmTime(hourOfDay = (9..16).random(), minute = 0) // 9:00--16:00