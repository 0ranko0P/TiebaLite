package com.huanchengfly.tieba.post.repository.user

import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.models.settings.ClientConfig
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.SignConfig
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.utils.UIDUtil
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface Settings<T> {

    val flow: Flow<T>

    fun set(new: T)

    fun save(transform: (old: T) -> T)
}

/**
 * App Settings
 * */
interface SettingsRepository {

    /**
     * Settings of current user account ID, ``-1`` if no user logged-in
     * */
    val accountUid: Settings<Long>

    val blockSettings: Settings<BlockSettings>

    /**
     * Settings of the scaling factor for fonts
     * */
    val fontScale: Settings<Float>

    val habitSettings: Settings<HabitSettings>

    val themeSettings: Settings<ThemeSettings>

    val uiSettings: Settings<UISettings>

    val signConfig: Settings<SignConfig>

    /**
     * Settings of client [UUID].
     *
     * @see UIDUtil.uUID
     * */
    val UUIDSettings: Settings<String>

    val clientConfig: Settings<ClientConfig>
}
