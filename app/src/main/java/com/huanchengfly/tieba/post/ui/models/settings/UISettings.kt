package com.huanchengfly.tieba.post.ui.models.settings

enum class DarkPreference {
    FOLLOW_SYSTEM, ALWAYS, DISABLED
}

data class UISettings(
    val darkPreference: DarkPreference = DarkPreference.FOLLOW_SYSTEM,
    val reduceEffect: Boolean = false,
    val setupFinished: Boolean = false,
)