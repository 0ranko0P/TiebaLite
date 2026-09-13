package com.huanchengfly.tieba.post.core.data.repository.user

import com.huanchengfly.tieba.post.core.data.model.settings.BlockSettings
import com.huanchengfly.tieba.post.core.data.model.settings.ClientConfig
import com.huanchengfly.tieba.post.core.data.model.settings.HabitSettings
import com.huanchengfly.tieba.post.core.data.model.settings.PrivacySettings
import com.huanchengfly.tieba.post.core.data.model.settings.Settings
import com.huanchengfly.tieba.post.core.data.model.settings.SignConfig
import com.huanchengfly.tieba.post.core.data.model.settings.ThemeSettings
import com.huanchengfly.tieba.post.core.data.model.settings.UISettings

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

    val privacySettings: Settings<PrivacySettings>

    val themeSettings: Settings<ThemeSettings>

    val uiSettings: Settings<UISettings>

    val signConfig: Settings<SignConfig>

    /**
     * Settings of client [java.util.UUID].
     *
     * @see UIDUtil.uUID
     * */
    val UUIDSettings: Settings<String>

    val clientConfig: Settings<ClientConfig>

    val myLittleTail: Settings<String>
}