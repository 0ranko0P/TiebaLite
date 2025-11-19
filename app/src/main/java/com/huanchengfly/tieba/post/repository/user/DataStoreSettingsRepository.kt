package com.huanchengfly.tieba.post.repository.user

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.material.color.utilities.Variant
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
import com.huanchengfly.tieba.post.ui.models.settings.WaterType
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.JobQueue
import com.huanchengfly.tieba.post.utils.LauncherIcons
import com.huanchengfly.tieba.post.utils.ThemeUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val DATA_STORE_NAME = "app_preferences"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATA_STORE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler {
        Log.e(DATA_STORE_NAME, "onHandleCorruption", it)
        emptyPreferences()
    }
)

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

    private inner class ComplexSettings<T>(val transformer: PreferenceTransformer<T>): Settings<T>(
        flow = dataStore.data.map(transform = transformer.get).distinctUntilChanged()
    ) {

        override fun set(new: T) = queue.submit(Dispatchers.IO) {
            dataStore.edit { transformer.set(it, new) }
        }

        override fun save(transform: (old: T) -> T) = queue.submit(Dispatchers.IO) {
            dataStore.edit {
                val new = transform(transformer.get(it))
                transformer.set(it, new)
            }
        }
    }

    private inner class SimpleSettings<T>(val key: Preferences.Key<T>, val default: T): Settings<T>(
        flow = dataStore.data.map { it[key] ?: default }.distinctUntilChanged()
    ) {

        override fun set(new: T) = queue.submit(Dispatchers.IO) { dataStore.edit { it[key] = new } }

        override fun save(transform: (T) -> T) = queue.submit(Dispatchers.IO) {
            dataStore.edit { it[key] = transform(it[key] ?: default) }
        }
    }

    override val clientConfig: Settings<ClientConfig> = ComplexSettings(ClientConfigTransformer)

    override val accountUid: Settings<Long> = SimpleSettings(longPreferencesKey("account_uid"), -1)

    override val blockSettings: Settings<BlockSettings> = ComplexSettings(BlockTransformer)

    override val fontScale: Settings<Float> = SimpleSettings(floatPreferencesKey("fontScale"), 1.0f)

    override val habitSettings: Settings<HabitSettings> = ComplexSettings(HabitSettingsTransformer)

    override val themeSettings: Settings<ThemeSettings> = ComplexSettings(ThemeSettingsTransformer)

    override val uiSettings: Settings<UISettings> = ComplexSettings(UISettingsTransformer)

    override val signConfig: Settings<SignConfig> = ComplexSettings(SignConfigTransformer)

    override val UUIDSettings: Settings<String> = SimpleSettings(stringPreferencesKey("uuid"), "")

    override val myLittleTail: Settings<String> = SimpleSettings(stringPreferencesKey("little_tail"), "")
}

private object HabitSettingsTransformer : PreferenceTransformer<HabitSettings> {
    override val get: (Preferences) -> HabitSettings = {
        HabitSettings(
            collectedDesc = it[booleanPreferencesKey(KEY_COLLECTED_DESC)] == true,
            favoriteDesc = it[booleanPreferencesKey(KEY_FAVORITE_DESC)] == true,
            favoriteSeeLz = it[booleanPreferencesKey(KEY_FAVORITE_SEE_LZ)] ?: true,
            forumSortType = it[intPreferencesKey(KEY_FORUM_SORT_DEFAULT)] ?: ForumSortType.BY_REPLY,
            forumFAB = it[intPreferencesKey(KEY_FORUM_FAB_FUNCTION)] ?: ForumFAB.BACK_TO_TOP,
            hideMedia = it[booleanPreferencesKey(KEY_POST_HIDE_MEDIA)] == true,
            hideReply = it[booleanPreferencesKey(KEY_REPLY_HIDE)] == true,
            hideReplyWarning = it[booleanPreferencesKey(KEY_REPLY_HIDE_WARNING)] == true,
            imageLoadType = it[intPreferencesKey(KEY_IMAGE_LOAD_TYPE)] ?: ImageUtil.SETTINGS_SMART_ORIGIN,
            imageWatermarkType = it[intPreferencesKey(KEY_IMAGE_WATERMARK_TYPE)] ?: WaterType.FORUM_NAME,
            showBothName = it[booleanPreferencesKey(KEY_SHOW_NICKNAME)] == true,
            showHistoryInHome = it[booleanPreferencesKey(KEY_HOME_PAGE_SHOW_HISTORY)] ?: true,
        )
    }

    override val set: (MutablePreferences, HabitSettings) -> Unit = { it, habit ->
        it[booleanPreferencesKey(KEY_COLLECTED_DESC)] = habit.collectedDesc
        it[booleanPreferencesKey(KEY_FAVORITE_DESC)] = habit.favoriteDesc
        it[booleanPreferencesKey(KEY_FAVORITE_SEE_LZ)] = habit.favoriteSeeLz
        it[intPreferencesKey(KEY_FORUM_SORT_DEFAULT)] = habit.forumSortType
        it[intPreferencesKey(KEY_FORUM_FAB_FUNCTION)] = habit.forumFAB
        it[booleanPreferencesKey(KEY_POST_HIDE_MEDIA)] = habit.hideMedia
        it[booleanPreferencesKey(KEY_REPLY_HIDE)] = habit.hideReply
        it[booleanPreferencesKey(KEY_REPLY_HIDE_WARNING)] = habit.hideReplyWarning
        it[intPreferencesKey(KEY_IMAGE_LOAD_TYPE)] = habit.imageLoadType
        it[intPreferencesKey(KEY_IMAGE_WATERMARK_TYPE)] = habit.imageWatermarkType
        it[booleanPreferencesKey(KEY_SHOW_NICKNAME)] = habit.showBothName
        it[booleanPreferencesKey(KEY_HOME_PAGE_SHOW_HISTORY)] = habit.showHistoryInHome
    }

    private const val KEY_COLLECTED_DESC = "ui_fav_desc"
    private const val KEY_FAVORITE_DESC = "ui_fav_desc_sort"
    private const val KEY_FAVORITE_SEE_LZ = "ui_fav_see_lz"
    private const val KEY_FORUM_FAB_FUNCTION = "forum_fab"
    private const val KEY_FORUM_SORT_DEFAULT = "forum_sort_type"
    private const val KEY_IMAGE_LOAD_TYPE = "img_load_type"
    private const val KEY_IMAGE_WATERMARK_TYPE = "img_watermark"
    private const val KEY_POST_HIDE_MEDIA = "ui_post_hide_media"
    private const val KEY_REPLY_HIDE = "ui_reply_hide"
    private const val KEY_REPLY_HIDE_WARNING = "ui_reply_hide_warn"
    private const val KEY_SHOW_NICKNAME = "ui_show_both_name"
    private const val KEY_HOME_PAGE_SHOW_HISTORY = "ui_history_in_home"
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
        val darkPrefOrdinal = it[intPreferencesKey(KEY_DARK_THEME_MODE)] ?: DarkPreference.FOLLOW_SYSTEM.ordinal
        val appIconOrdinal = it[intPreferencesKey(KEY_APP_ICON)] ?: LauncherIcons.NEW_ICON.ordinal
        UISettings(
            appIcon = LauncherIcons.entries[appIconOrdinal],
            appIconThemed = it[booleanPreferencesKey(KEY_APP_THEMED_ICON)] == true,
            darkPreference = DarkPreference.entries[darkPrefOrdinal],
            darkenImage = it[booleanPreferencesKey(KEY_DARKEN_IMAGE_ON_NIGHT)] ?: true,
            liftBottomBar = it[booleanPreferencesKey(KEY_LIFT_BOTTOM_BAR)] ?: false,
            reduceEffect = it[booleanPreferencesKey(KEY_REDUCE_EFFECT)] ?: (Build.VERSION.SDK_INT < Build.VERSION_CODES.S),
            setupFinished = it[booleanPreferencesKey(KEY_SETUP_FINISHED)] == true,
            homeForumList = it[booleanPreferencesKey(KEY_HOME_SINGLE_FORUM_LIST)] == true
        )
    }

    override val set: (MutablePreferences, UISettings) -> Unit = { it, ui ->
        it[intPreferencesKey(KEY_APP_ICON)] = ui.appIcon.ordinal
        it[booleanPreferencesKey(KEY_APP_THEMED_ICON)] = ui.appIconThemed
        it[intPreferencesKey(KEY_DARK_THEME_MODE)] = ui.darkPreference.ordinal
        it[booleanPreferencesKey(KEY_DARKEN_IMAGE_ON_NIGHT)] = ui.darkenImage
        it[booleanPreferencesKey(KEY_LIFT_BOTTOM_BAR)] = ui.liftBottomBar
        it[booleanPreferencesKey(KEY_REDUCE_EFFECT)] = ui.reduceEffect
        it[booleanPreferencesKey(KEY_SETUP_FINISHED)] = ui.setupFinished
        it[booleanPreferencesKey(KEY_HOME_SINGLE_FORUM_LIST)] = ui.homeForumList
    }

    const val KEY_APP_ICON = "app_icon"
    const val KEY_APP_THEMED_ICON = "app_themed_icon"

    /**
     * Dark mode preferences, Default mode is [DarkPreference.FOLLOW_SYSTEM]
     *
     * @see ThemeUtil.shouldUseNightMode
     * */
    private const val KEY_DARK_THEME_MODE = "dark_mode"
    private const val KEY_DARKEN_IMAGE_ON_NIGHT = "ui_dark_img"
    private const val KEY_LIFT_BOTTOM_BAR = "ui_lift_bottom"
    private const val KEY_SETUP_FINISHED = "ui_setup"
    private const val KEY_REDUCE_EFFECT = "ui_reduce_effect"
    private const val KEY_HOME_SINGLE_FORUM_LIST = "ui_forum_list_in_home"
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
        )
    }

    override val set: (MutablePreferences, SignConfig) -> Unit = { it, config ->
        it.putBoolean(KEY_OKSIGN_AUTO, config.autoSign)
        it.putBoolean(KEY_OKSIGN_SLOW, config.autoSignSlow)
        it.putString(KEY_OKSIGN_AUTO_TIME, config.autoSignTime)
        it.putBoolean(KEY_OKSIGN_OFFICIAL, config.okSignOfficial)
    }

    private const val KEY_OKSIGN_AUTO = "auto_sign"
    private const val KEY_OKSIGN_AUTO_TIME = "auto_sign_time"
    private const val KEY_OKSIGN_OFFICIAL = "sign_using_official"
    private const val KEY_OKSIGN_SLOW = "sign_slow_mode"
}

private object ClientConfigTransformer: PreferenceTransformer<ClientConfig> {
    override val get: (Preferences) -> ClientConfig = {
        ClientConfig(
            clientId = it[stringPreferencesKey(KEY_CLIENT_ID)],
            sampleId = it[stringPreferencesKey(KEY_SAMPLE_ID)],
            baiduId = it[stringPreferencesKey(KEY_BAIDU_ID)],
            activeTimestamp = it[longPreferencesKey(KEY_ACTIVE_TIMESTAMP)] ?: System.currentTimeMillis(),
            firstInstallTime = it[longPreferencesKey(KEY_INSTALL_TIME)],
            lastUpdateTime = it[longPreferencesKey(KEY_UPDATE_TIME)]
        )
    }

    override val set: (MutablePreferences, ClientConfig) -> Unit = { it, config ->
        it.putString(KEY_CLIENT_ID, config.clientId)
        it.putString(KEY_SAMPLE_ID, config.sampleId)
        it.putString(KEY_BAIDU_ID, config.baiduId)
        it[longPreferencesKey(KEY_ACTIVE_TIMESTAMP)] = config.activeTimestamp
        it[longPreferencesKey(KEY_INSTALL_TIME)] = config.firstInstallTime!!
        it[longPreferencesKey(KEY_UPDATE_TIME)] = config.lastUpdateTime!!
    }

    private const val KEY_CLIENT_ID = "client_id"
    private const val KEY_SAMPLE_ID = "sample_id"
    private const val KEY_BAIDU_ID = "baidu_id"
    private const val KEY_ACTIVE_TIMESTAMP = "active_timestamp"
    private const val KEY_INSTALL_TIME = "se_install_time"
    private const val KEY_UPDATE_TIME = "se_update_time"
}
