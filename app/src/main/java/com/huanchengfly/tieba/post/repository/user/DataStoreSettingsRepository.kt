package com.huanchengfly.tieba.post.repository.user

import android.content.Context
import android.os.Build
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.material.color.utilities.Variant
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getColor
import com.huanchengfly.tieba.post.putBoolean
import com.huanchengfly.tieba.post.putColor
import com.huanchengfly.tieba.post.putInt
import com.huanchengfly.tieba.post.putLong
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.theme.TiebaBlue
import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.models.settings.ClientConfig
import com.huanchengfly.tieba.post.ui.models.settings.DarkPreference
import com.huanchengfly.tieba.post.ui.models.settings.ForumFAB
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.SignConfig
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.utils.JobQueue
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil.DARK_MODE_FOLLOW_SYSTEM
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private interface PreferenceTransformer<T> {
    val get: (preference: Preferences) -> T
    val set: (MutablePreferences, T) -> Unit
}

/**
 * DataStore implementation of [SettingsRepository].
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
): SettingsRepository {

    private val queue = JobQueue()

    private val dataStore = context.dataStore

    private inner class ComplexSettings<T>(val transformer: PreferenceTransformer<T>): Settings<T> {

        override val flow: Flow<T> = dataStore.data.map(transform = transformer.get).distinctUntilChanged()

        override fun set(new: T) {
            queue.submit(Dispatchers.IO) {
                dataStore.edit { transformer.set(it, new) }
            }
        }

        override fun save(transform: (old: T) -> T) {
            queue.submit(Dispatchers.IO) {
                dataStore.edit {
                    val new = transform(transformer.get(it))
                    transformer.set(it, new)
                }
            }
        }
    }

    override val clientConfig: Settings<ClientConfig> = ComplexSettings(ClientConfigTransformer)

    override val blockSettings: Settings<BlockSettings> = ComplexSettings(BlockTransformer)

    override val habitSettings: Settings<HabitSettings> = ComplexSettings(HabitSettingsTransformer)

    override val themeSettings: Settings<ThemeSettings> = ComplexSettings(ThemeSettingsTransformer)

    override val uiSettings: Settings<UISettings> = ComplexSettings(UISettingsTransformer)

    override val signConfig: Settings<SignConfig> = ComplexSettings(SignConfigTransformer)
}


private object HabitSettingsTransformer : PreferenceTransformer<HabitSettings> {
    override val get: (Preferences) -> HabitSettings = {
        HabitSettings(
            forumSortType = it[intPreferencesKey(KEY_FORUM_SORT_DEFAULT)] ?: ForumSortType.BY_REPLY,
            forumFAB = it[intPreferencesKey(KEY_FORUM_FAB_FUNCTION)] ?: ForumFAB.BACK_TO_TOP,
            showBothName = it[booleanPreferencesKey(KEY_SHOW_NICKNAME)] == true
        )
    }

    override val set: (MutablePreferences, HabitSettings) -> Unit = { it, habit ->
        it[intPreferencesKey(KEY_FORUM_SORT_DEFAULT)] = habit.forumSortType
        it[intPreferencesKey(KEY_FORUM_FAB_FUNCTION)] = habit.forumFAB
        it[booleanPreferencesKey(KEY_SHOW_NICKNAME)] = habit.showBothName
    }

    private const val KEY_FORUM_FAB_FUNCTION = "forum_fab"
    private const val KEY_FORUM_SORT_DEFAULT = "forum_sort_type"
    private const val KEY_SHOW_NICKNAME = "ui_show_both_name"
}

private object ThemeSettingsTransformer : PreferenceTransformer<ThemeSettings> {
    override val get: (Preferences) -> ThemeSettings = {
        val transFilters = it[longPreferencesKey(KEY_TRANSLUCENT_FILTERS)]

        ThemeSettings(
            theme = it[intPreferencesKey(KEY_THEME)]?.let { i -> Theme.entries[i] } ?: Theme.BLUE,
            customColor = it.getColor(KEY_CUSTOM_COLOR),
            customVariant = it[intPreferencesKey(KEY_CUSTOM_VARIANT)]?.let { i -> Variant.entries[i] },
            transColor = it.getColor(KEY_TRANSLUCENT_COLOR) ?: TiebaBlue,
            transAlpha = transFilters?.let { value -> unpackFloat1(value) } ?: 1.0f,
            transBlur = transFilters?.let { value -> unpackFloat2(value) } ?: 0f,
            transDarkColorMode = it[booleanPreferencesKey(KEY_TRANSLUCENT_DARK_COLOR_MODE)] == true,
            transBackground = it[stringPreferencesKey(KEY_TRANSLUCENT_BACKGROUND)]
        )
    }

    override val set: (MutablePreferences, ThemeSettings) -> Unit = { it, theme ->
        it.putInt(KEY_THEME, theme.theme.ordinal)
        it.putColor(KEY_CUSTOM_COLOR, theme.customColor)
        it.putInt(KEY_CUSTOM_VARIANT, theme.customVariant?.ordinal)
        it.putColor(KEY_TRANSLUCENT_COLOR, theme.transColor)
        it.putLong(KEY_TRANSLUCENT_FILTERS, packFloats(theme.transAlpha, theme.transBlur))
        it[booleanPreferencesKey(KEY_TRANSLUCENT_DARK_COLOR_MODE)] = theme.transDarkColorMode
        it.putString(KEY_TRANSLUCENT_BACKGROUND, theme.transBackground)
    }

    private const val KEY_THEME = "theme" // Theme.ordinal
    private const val KEY_CUSTOM_COLOR = "custom_primary_color"
    private const val KEY_CUSTOM_VARIANT = "custom_variant" // Int: Variant.ordinal

    private const val KEY_TRANSLUCENT_FILTERS = "trans_filters" // packaged two float, alpha and blur
    private const val KEY_TRANSLUCENT_COLOR = "trans_primary_color"

    private const val KEY_TRANSLUCENT_BACKGROUND = "translucent_background_path"
    private const val KEY_TRANSLUCENT_DARK_COLOR_MODE = "trans_dark_color"
}

private object UISettingsTransformer: PreferenceTransformer<UISettings> {
    override val get: (Preferences) -> UISettings = {
        val darkPrefOrdinal = it[intPreferencesKey(KEY_DARK_THEME_MODE)]
        UISettings(
            darkPreference = darkPrefOrdinal?.let { i -> DarkPreference.entries[i] } ?: DarkPreference.FOLLOW_SYSTEM,
            reduceEffect = it[booleanPreferencesKey(KEY_REDUCE_EFFECT)] ?: (Build.VERSION.SDK_INT < Build.VERSION_CODES.S),
            setupFinished = it[booleanPreferencesKey(KEY_SETUP_FINISHED)] == true
        )
    }

    override val set: (MutablePreferences, UISettings) -> Unit = { it, ui ->
        it[intPreferencesKey(KEY_DARK_THEME_MODE)] = ui.darkPreference.ordinal
        it[booleanPreferencesKey(KEY_REDUCE_EFFECT)] = ui.reduceEffect
        it[booleanPreferencesKey(KEY_SETUP_FINISHED)] = ui.setupFinished
    }

    /**
     * Dark mode preferences, Default mode is [DARK_MODE_FOLLOW_SYSTEM]
     *
     * @see ThemeUtil.shouldUseNightMode
     * */
    private const val KEY_DARK_THEME_MODE = "dark_mode"
    private const val KEY_SETUP_FINISHED = "ui_setup"
    private const val KEY_REDUCE_EFFECT = "ui_reduce_effect"
}

private object BlockTransformer: PreferenceTransformer<BlockSettings> {
    override val get: (Preferences) -> BlockSettings = {
        BlockSettings(
            blockVideo = it[booleanPreferencesKey(KEY_BLOCK_VIDEO)] == true,
            hideBlocked = it[booleanPreferencesKey(KEY_HIDE_BLOCKED)] ?: true
        )
    }

    override val set: (MutablePreferences, BlockSettings) -> Unit = { it, block ->
        it[booleanPreferencesKey(KEY_BLOCK_VIDEO)] = block.blockVideo
        it[booleanPreferencesKey(KEY_HIDE_BLOCKED)] = block.hideBlocked
    }

    private const val KEY_HIDE_BLOCKED = "ui_post_hide_blocked"
    private const val KEY_BLOCK_VIDEO = "ui_block_video"
}

private object SignConfigTransformer: PreferenceTransformer<SignConfig> {
    override val get: (Preferences) -> SignConfig = {
        SignConfig(
            autoSign = it[booleanPreferencesKey(KEY_OKSIGN_AUTO)] == true,
            autoSignSlow = it[booleanPreferencesKey(KEY_OKSIGN_SLOW)] ?: true,
            autoSignTime = it[stringPreferencesKey(KEY_OKSIGN_AUTO_TIME)] ?: "09:00",
            okSignOfficial = it[booleanPreferencesKey(KEY_OKSIGN_OFFICIAL)] ?: true,
            ignoreBatteryOp = it[booleanPreferencesKey(KEY_IGNORE_BATTERY_OPTIMIZATION)] == true,
        )
    }

    override val set: (MutablePreferences, SignConfig) -> Unit = { it, config ->
        it.putBoolean(KEY_OKSIGN_AUTO, config.autoSign)
        it.putBoolean(KEY_OKSIGN_SLOW, config.autoSignSlow)
        it.putString(KEY_OKSIGN_AUTO_TIME, config.autoSignTime)
        it.putBoolean(KEY_OKSIGN_OFFICIAL, config.okSignOfficial)
        it.putBoolean(KEY_IGNORE_BATTERY_OPTIMIZATION, config.ignoreBatteryOp)
    }

    private const val KEY_OKSIGN_AUTO = "auto_sign"
    private const val KEY_OKSIGN_AUTO_TIME = "auto_sign_time"
    private const val KEY_IGNORE_BATTERY_OPTIMIZATION = "ui_ignore_battery"
    private const val KEY_OKSIGN_OFFICIAL = "sign_using_official"
    private const val KEY_OKSIGN_SLOW = "sign_slow_mode"
}

private object ClientConfigTransformer: PreferenceTransformer<ClientConfig> {
    override val get: (Preferences) -> ClientConfig = {
        ClientConfig(
            clientId = it[stringPreferencesKey(KEY_CLIENT_ID)],
            sampleId = it [stringPreferencesKey(KEY_SAMPLE_ID)],
            baiduId = it [stringPreferencesKey(KEY_BAIDU_ID)],
            activeTimestamp = it[longPreferencesKey(KEY_ACTIVE_TIMESTAMP)] ?: System.currentTimeMillis()
        )
    }

    override val set: (MutablePreferences, ClientConfig) -> Unit = { it, config ->
        it.putString(KEY_CLIENT_ID, config.clientId)
        it.putString(KEY_SAMPLE_ID, config.sampleId)
        it.putString(KEY_BAIDU_ID, config.baiduId)
        it.putLong(KEY_ACTIVE_TIMESTAMP, config.activeTimestamp)
    }

    private const val KEY_CLIENT_ID = "client_id"
    private const val KEY_SAMPLE_ID = "sample_id"
    private const val KEY_BAIDU_ID = "baidu_id"
    private const val KEY_ACTIVE_TIMESTAMP = "active_timestamp"
}
