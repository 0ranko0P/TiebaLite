package com.huanchengfly.tieba.post.core.data.repository.user

import android.content.Context
import android.os.Build
import android.util.Log
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
import com.huanchengfly.tieba.post.core.common.ktx.packFloats
import com.huanchengfly.tieba.post.core.common.ktx.unpackFloat1
import com.huanchengfly.tieba.post.core.common.ktx.unpackFloat2
import com.huanchengfly.tieba.post.core.data.model.HmTime
import com.huanchengfly.tieba.post.core.data.model.settings.BlockSettings
import com.huanchengfly.tieba.post.core.data.model.settings.ClientConfig
import com.huanchengfly.tieba.post.core.data.model.settings.DarkPreference
import com.huanchengfly.tieba.post.core.data.model.settings.ForumSortType
import com.huanchengfly.tieba.post.core.data.model.settings.HabitSettings
import com.huanchengfly.tieba.post.core.data.model.settings.ImageLoadType
import com.huanchengfly.tieba.post.core.data.model.settings.LauncherIcons
import com.huanchengfly.tieba.post.core.data.model.settings.NavigationLabel
import com.huanchengfly.tieba.post.core.data.model.settings.PrivacySettings
import com.huanchengfly.tieba.post.core.data.model.settings.Settings
import com.huanchengfly.tieba.post.core.data.model.settings.SignConfig
import com.huanchengfly.tieba.post.core.data.model.settings.Theme
import com.huanchengfly.tieba.post.core.data.model.settings.ThemeSettings
import com.huanchengfly.tieba.post.core.data.model.settings.UISettings
import com.huanchengfly.tieba.post.core.data.model.settings.WaterType
import com.huanchengfly.tieba.post.core.data.model.settings.randomSignTime
import com.huanchengfly.tieba.post.core.data.util.SystemPropertyUtil
import com.huanchengfly.tieba.post.utils.JobQueue
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
        Log.wtf(DATA_STORE_NAME, "onHandleCorruption", it)
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
internal class DataStoreSettingsRepository @Inject constructor(
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

    override val privacySettings: Settings<PrivacySettings> = ComplexSettings(PrivacySettingsTransformer)

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
            hideMedia = it[booleanPreferencesKey(KEY_POST_HIDE_MEDIA)] == true,
            hideReply = it[booleanPreferencesKey(KEY_REPLY_HIDE)] == true,
            hideReplyWarning = it[booleanPreferencesKey(KEY_REPLY_HIDE_WARNING)] == true,
            imageLoadType = it[intPreferencesKey(KEY_IMAGE_LOAD_TYPE)] ?: ImageLoadType.SMART_ORIGIN,
            imageWatermarkType = it[intPreferencesKey(KEY_IMAGE_WATERMARK_TYPE)] ?: WaterType.FORUM_NAME,
            showBothName = it[booleanPreferencesKey(KEY_SHOW_NICKNAME)] == true,
            stickyHeader = it[booleanPreferencesKey(KEY_STICKY_HEADER)] ?: true,
            videoAutoplay = it[booleanPreferencesKey(KEY_VIDEO_AUTOPLAY)] ?: true
        )
    }

    override val set: (MutablePreferences, HabitSettings) -> Unit = { it, habit ->
        it[booleanPreferencesKey(KEY_COLLECTED_DESC)] = habit.collectedDesc
        it[booleanPreferencesKey(KEY_FAVORITE_DESC)] = habit.favoriteDesc
        it[booleanPreferencesKey(KEY_FAVORITE_SEE_LZ)] = habit.favoriteSeeLz
        it[intPreferencesKey(KEY_FORUM_SORT_DEFAULT)] = habit.forumSortType
        it[booleanPreferencesKey(KEY_POST_HIDE_MEDIA)] = habit.hideMedia
        it[booleanPreferencesKey(KEY_REPLY_HIDE)] = habit.hideReply
        it[booleanPreferencesKey(KEY_REPLY_HIDE_WARNING)] = habit.hideReplyWarning
        it[intPreferencesKey(KEY_IMAGE_LOAD_TYPE)] = habit.imageLoadType
        it[intPreferencesKey(KEY_IMAGE_WATERMARK_TYPE)] = habit.imageWatermarkType
        it[booleanPreferencesKey(KEY_SHOW_NICKNAME)] = habit.showBothName
        it[booleanPreferencesKey(KEY_STICKY_HEADER)] = habit.stickyHeader
        it[booleanPreferencesKey(KEY_VIDEO_AUTOPLAY)] = habit.videoAutoplay
        it -= intPreferencesKey(KEY_FORUM_FAB_FUNCTION)
    }

    private const val KEY_COLLECTED_DESC = "ui_fav_desc"
    private const val KEY_FAVORITE_DESC = "ui_fav_desc_sort"
    private const val KEY_FAVORITE_SEE_LZ = "ui_fav_see_lz"

    /**
     * 吧页面悬浮按钮功能, 4.0.0 Beta 4.3 中移除
     *
     * @since 4.0.0 dev 5
     * */
    private const val KEY_FORUM_FAB_FUNCTION = "forum_fab"

    private const val KEY_FORUM_SORT_DEFAULT = "forum_sort_type"
    private const val KEY_IMAGE_LOAD_TYPE = "img_load_type"
    private const val KEY_IMAGE_WATERMARK_TYPE = "img_watermark"
    private const val KEY_POST_HIDE_MEDIA = "ui_post_hide_media"
    private const val KEY_REPLY_HIDE = "ui_reply_hide"
    private const val KEY_REPLY_HIDE_WARNING = "ui_reply_hide_warn"
    private const val KEY_SHOW_NICKNAME = "ui_show_both_name"
    private const val KEY_STICKY_HEADER = "ui_sticky_header"
    private const val KEY_VIDEO_AUTOPLAY = "ui_video_autoplay"
}

private object PrivacySettingsTransformer : PreferenceTransformer<PrivacySettings> {

    override val get: (Preferences) -> PrivacySettings = {
        PrivacySettings(
            readClipBoardLink = it[booleanPreferencesKey(KEY_PRIVACY_CLIPBOARD)] ?: true
        )
    }

    override val set: (MutablePreferences, PrivacySettings) -> Unit = { it, settings ->
        it[booleanPreferencesKey(KEY_PRIVACY_CLIPBOARD)] = settings.readClipBoardLink
    }

    private const val KEY_PRIVACY_CLIPBOARD = "clipboard_link"
}

private object ThemeSettingsTransformer : PreferenceTransformer<ThemeSettings> {
    override val get: (Preferences) -> ThemeSettings = {
        val transFilters = it[longPreferencesKey(KEY_TRANSLUCENT_FILTERS)]

        ThemeSettings(
            theme = it[intPreferencesKey(KEY_THEME)]?.let { i -> Theme.entries[i] } ?: Theme.BLUE,
            customColor = it[intPreferencesKey(KEY_CUSTOM_COLOR)],
            customVariant = it[intPreferencesKey(KEY_CUSTOM_VARIANT)],
            transColor = it[intPreferencesKey(KEY_TRANSLUCENT_COLOR)],
            transAlpha = transFilters?.let { value -> unpackFloat1(value) } ?: 1.0f,
            transBlur = transFilters?.let { value -> unpackFloat2(value) } ?: 0f,
            transDarkColorMode = it[booleanPreferencesKey(KEY_TRANSLUCENT_DARK_COLOR_MODE)] == true,
            transBackground = it[stringPreferencesKey(KEY_TRANSLUCENT_BACKGROUND)]
        )
    }

    override val set: (MutablePreferences, ThemeSettings) -> Unit = { it, theme ->
        it[intPreferencesKey(KEY_THEME)] = theme.theme.ordinal
        theme.customColor?.let { color -> it[intPreferencesKey(KEY_CUSTOM_COLOR)] = color }
        theme.customVariant?.let { ordinal -> it[intPreferencesKey(KEY_CUSTOM_VARIANT)] = ordinal }
        theme.transColor?.let { color -> it[intPreferencesKey(KEY_TRANSLUCENT_COLOR)] = color }
        it[longPreferencesKey(KEY_TRANSLUCENT_FILTERS)] = packFloats(theme.transAlpha, theme.transBlur)
        it[booleanPreferencesKey(KEY_TRANSLUCENT_DARK_COLOR_MODE)] = theme.transDarkColorMode
        theme.transBackground?.let { bg -> it[stringPreferencesKey(KEY_TRANSLUCENT_BACKGROUND)] = bg }
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

    val PRODUCT_FIRST_API_LEVEL: Int
        get() = SystemPropertyUtil.getInt("ro.product.first_api_level", default = Build.VERSION.SDK_INT)

    override val get: (Preferences) -> UISettings = {
        val darkPrefOrdinal = it[intPreferencesKey(KEY_DARK_THEME_MODE)] ?: DarkPreference.FOLLOW_SYSTEM.ordinal
        val appIconOrdinal = it[intPreferencesKey(KEY_APP_ICON)] ?: LauncherIcons.NEW_ICON.ordinal
        val bottomNavLabelOrdinal = it[intPreferencesKey(KEY_BOTTOM_NAV_LABEL)] ?: NavigationLabel.ALWAYS.ordinal

        UISettings(
            appIcon = LauncherIcons.entries[appIconOrdinal],
            appIconThemed = it[booleanPreferencesKey(KEY_APP_THEMED_ICON)] == true,
            bottomNavFloating = it[booleanPreferencesKey(KEY_BOTTOM_NAV_FLOATING)] == true,
            bottomNavLabel = NavigationLabel.entries[bottomNavLabelOrdinal],
            darkAmoled = it[booleanPreferencesKey(KEY_DARK_AMOLED)] == true,
            darkPreference = DarkPreference.entries[darkPrefOrdinal],
            darkenImage = it[booleanPreferencesKey(KEY_DARKEN_IMAGE_ON_NIGHT)] ?: true,
            hideExplore = it[booleanPreferencesKey(KEY_HIDE_EXPLORE)] == true,
            reduceEffect = it[booleanPreferencesKey(KEY_REDUCE_EFFECT)] ?: (PRODUCT_FIRST_API_LEVEL < Build.VERSION_CODES.S),
            reduceMotion = it[booleanPreferencesKey(KEY_REDUCE_MOTION)] ?: (PRODUCT_FIRST_API_LEVEL < Build.VERSION_CODES.S),
            setupFinished = it[booleanPreferencesKey(KEY_SETUP_FINISHED)] == true,
            homeForumList = it[booleanPreferencesKey(KEY_HOME_SINGLE_FORUM_LIST)] == true,
            showHistoryInHome = it[booleanPreferencesKey(KEY_HOME_PAGE_SHOW_HISTORY)] ?: true,
        )
    }

    override val set: (MutablePreferences, UISettings) -> Unit = { it, ui ->
        it[intPreferencesKey(KEY_APP_ICON)] = ui.appIcon.ordinal
        it[booleanPreferencesKey(KEY_APP_THEMED_ICON)] = ui.appIconThemed
        it[booleanPreferencesKey(KEY_BOTTOM_NAV_FLOATING)] = ui.bottomNavFloating
        it[intPreferencesKey(KEY_BOTTOM_NAV_LABEL)] = ui.bottomNavLabel.ordinal
        it[booleanPreferencesKey(KEY_DARK_AMOLED)] = ui.darkAmoled
        it[intPreferencesKey(KEY_DARK_THEME_MODE)] = ui.darkPreference.ordinal
        it[booleanPreferencesKey(KEY_DARKEN_IMAGE_ON_NIGHT)] = ui.darkenImage
        it[booleanPreferencesKey(KEY_HIDE_EXPLORE)] = ui.hideExplore
        it[booleanPreferencesKey(KEY_REDUCE_EFFECT)] = ui.reduceEffect
        it[booleanPreferencesKey(KEY_REDUCE_MOTION)] = ui.reduceMotion
        it[booleanPreferencesKey(KEY_SETUP_FINISHED)] = ui.setupFinished
        it[booleanPreferencesKey(KEY_HOME_SINGLE_FORUM_LIST)] = ui.homeForumList
        it[booleanPreferencesKey(KEY_HOME_PAGE_SHOW_HISTORY)] = ui.showHistoryInHome
    }

    private const val KEY_APP_ICON = "app_icon"
    private const val KEY_APP_THEMED_ICON = "app_themed_icon"
    private const val KEY_BOTTOM_NAV_FLOATING = "ui_bottom_nav_floating"
    private const val KEY_BOTTOM_NAV_LABEL = "ui_bottom_nav_label"

    private const val KEY_DARK_AMOLED = "dark_amoled"

    /**
     * Dark mode preferences, Default mode is [DarkPreference.FOLLOW_SYSTEM]
     * */
    private const val KEY_DARK_THEME_MODE = "dark_mode"
    private const val KEY_DARKEN_IMAGE_ON_NIGHT = "ui_dark_img"
    private const val KEY_HIDE_EXPLORE = "ui_hide_explore"
    private const val KEY_SETUP_FINISHED = "ui_setup"
    private const val KEY_REDUCE_EFFECT = "ui_reduce_effect"
    private const val KEY_REDUCE_MOTION = "ui_reduce_motion"
    private const val KEY_HOME_SINGLE_FORUM_LIST = "ui_forum_list_in_home"
    private const val KEY_HOME_PAGE_SHOW_HISTORY = "ui_history_in_home"
}

private object BlockTransformer: PreferenceTransformer<BlockSettings> {
    override val get: (Preferences) -> BlockSettings = {
        BlockSettings(
            blockVideo = it[booleanPreferencesKey(KEY_BLOCK_VIDEO)] == true,
            hideBlocked = it[booleanPreferencesKey(KEY_HIDE_BLOCKED)] == true
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
        val hmTime = it[longPreferencesKey(KEY_OKSIGN_AUTO_TIME)]?.let { t -> HmTime(t) }
        SignConfig(
            autoSign = it[booleanPreferencesKey(KEY_OKSIGN_AUTO)] == true,
            autoSignSlow = it[booleanPreferencesKey(KEY_OKSIGN_SLOW)] ?: true,
            autoSignTime = hmTime ?: randomSignTime(),
            okSignOfficial = it[booleanPreferencesKey(KEY_OKSIGN_OFFICIAL)] ?: true,
        )
    }

    override val set: (MutablePreferences, SignConfig) -> Unit = { it, config ->
        it[booleanPreferencesKey(KEY_OKSIGN_AUTO)] = config.autoSign
        it[booleanPreferencesKey(KEY_OKSIGN_SLOW)] = config.autoSignSlow
        it[longPreferencesKey(KEY_OKSIGN_AUTO_TIME)] = config.autoSignTime.value
        it[booleanPreferencesKey(KEY_OKSIGN_OFFICIAL)] = config.okSignOfficial
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
        config.clientId?.let { id -> it[stringPreferencesKey(KEY_CLIENT_ID)] = id }
        config.sampleId?.let { id -> it[stringPreferencesKey(KEY_SAMPLE_ID)] = id }
        config.baiduId?.let { id -> it[stringPreferencesKey(KEY_BAIDU_ID)] = id }
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