package com.huanchengfly.tieba.post.repository.user

import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.models.settings.ClientConfig
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettings
import com.huanchengfly.tieba.post.ui.models.settings.SignConfig
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.utils.HmTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private class FakeSettings<T>(default: T): Settings<T>(MutableStateFlow(default)) {

    override fun set(new: T) = (flow as MutableStateFlow).update { new }

    override fun save(transform: (T) -> T) = (flow as MutableStateFlow).update { transform(it) }
}

class FakeSettingsRepository @Inject constructor(): SettingsRepository {

    override val accountUid: Settings<Long> = FakeSettings(-1)

    override val blockSettings: Settings<BlockSettings> = FakeSettings(
        BlockSettings(blockVideo = false, hideBlocked = false)
    )

    override val fontScale: Settings<Float>
        get() = throw RuntimeException("Not yet implemented")

    override val habitSettings: Settings<HabitSettings>
        get() = throw RuntimeException("Not yet implemented")

    override val privacySettings: Settings<PrivacySettings>
        get() = throw RuntimeException("Not yet implemented")

    override val themeSettings: Settings<ThemeSettings>
        get() = throw RuntimeException("Not yet implemented")

    override val uiSettings: Settings<UISettings>
        get() = throw RuntimeException("Not yet implemented")

    override val signConfig: Settings<SignConfig> =
        FakeSettings(SignConfig(autoSignTime = HmTime(12, 0)))

    override val UUIDSettings: Settings<String>
        get() = throw RuntimeException("Not yet implemented")

    override val clientConfig: Settings<ClientConfig>
        get() = throw RuntimeException("Not yet implemented")

    override val myLittleTail: Settings<String>
        get() = throw RuntimeException("Not yet implemented")
}