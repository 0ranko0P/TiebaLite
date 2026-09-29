package com.huanchengfly.tieba.post.ui.page.settings

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface SettingsDestination: NavKey {

    @Serializable
    data object Settings: SettingsDestination

    @Serializable
    data object About: SettingsDestination

    @Serializable
    data object AccountManage: SettingsDestination

    @Serializable
    data object AppFont: SettingsDestination

    @Serializable
    data object BlockSettings: SettingsDestination

    @Serializable
    data object ForumBlockList: SettingsDestination

    @Serializable
    data object KeywordBlockList: SettingsDestination

    @Serializable
    data object UserBlockList: SettingsDestination

    @Serializable
    data object UI: SettingsDestination

    @Serializable
    data object Habit: SettingsDestination

    @Serializable
    data object Privacy: SettingsDestination

    @Serializable
    data object StickyHeader: SettingsDestination

    @Serializable
    data object More: SettingsDestination

    @Serializable
    data object OKSign: SettingsDestination

    @Serializable
    data object WorkInfo: SettingsDestination
}
