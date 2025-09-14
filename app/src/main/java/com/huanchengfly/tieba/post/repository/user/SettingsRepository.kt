package com.huanchengfly.tieba.post.repository.user

import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.models.settings.ClientConfig
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.SignConfig
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import kotlinx.coroutines.flow.Flow

interface Settings<T> {

    val flow: Flow<T>

    fun set(new: T)

    fun save(transform: (old: T) -> T)
}

/**
 * App Settings
 * */
interface SettingsRepository {

    val blockSettings: Settings<BlockSettings>

    val habitSettings: Settings<HabitSettings>

    val themeSettings: Settings<ThemeSettings>
    val uiSettings: Settings<UISettings>

    val signConfig: Settings<SignConfig>

    val clientConfig: Settings<ClientConfig>
}
