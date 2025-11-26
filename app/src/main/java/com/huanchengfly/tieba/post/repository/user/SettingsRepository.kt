package com.huanchengfly.tieba.post.repository.user

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.models.settings.ClientConfig
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettings
import com.huanchengfly.tieba.post.ui.models.settings.SignConfig
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.utils.UIDUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

@Immutable
abstract class Settings<T>(protected val flow: Flow<T>): Flow<T> by flow {

    suspend fun snapshot(): T = this.first()

    abstract fun set(new: T)

    abstract fun save(transform: (old: T) -> T)
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

    val privacySettings: Settings<PrivacySettings>

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

    val myLittleTail: Settings<String>
}
