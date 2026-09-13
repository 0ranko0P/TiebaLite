package com.huanchengfly.tieba.post.core.data.model.settings

import com.huanchengfly.tieba.post.core.data.model.HmTime

data class SignConfig(
    val autoSign: Boolean = false,
    val autoSignSlow: Boolean = true,
    val autoSignTime: HmTime = randomSignTime(),
    val okSignOfficial: Boolean = true,
)

fun randomSignTime(): HmTime = HmTime(hourOfDay = (9..16).random(), minute = 0) // 9:00--16:00