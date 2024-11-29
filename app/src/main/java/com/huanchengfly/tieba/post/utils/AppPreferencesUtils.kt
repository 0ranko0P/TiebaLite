package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.StringDef
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.dataStoreScope
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.getLong
import com.huanchengfly.tieba.post.getString
import com.huanchengfly.tieba.post.putInt
import com.huanchengfly.tieba.post.putLong
import com.huanchengfly.tieba.post.putString
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class AppPreferencesUtils private constructor(context: Context) {
    companion object {
        @Volatile private var instance: AppPreferencesUtils? = null

        fun getInstance(context: Context): AppPreferencesUtils {
            return instance ?: synchronized(this) {
                instance ?: AppPreferencesUtils(context.applicationContext).also { instance = it }
            }
        }

        const val KEY_FONT_SCALE = "fontScale"

        /**
         * 吧页面悬浮按钮功能, Default: [ForumFabFunction.BACK_TO_TOP]
         *
         * @see ForumFabFunction
         * */
        const val KEY_FORUM_FAB_FUNCTION = "forumFabFunction"

        /**
         * @see ForumSortType
         * */
        const val KEY_FORUM_SORT_DEFAULT = "default_sort_type"

        const val KEY_TRANSLUCENT_PRIMARY_COLOR = "trans_primary_color"

        const val KEY_LITTLE_TAIL = "little_tail"

        private const val KEY_LAST_REQUEST_UNIX = "userLikeLastRequestUnix"


        /***        BOOLEAN OPTIONS             ***/

        const val KEY_DARKEN_IMAGE_WHEN_NIGHT_MODE = "ui_dark_img"

        const val KEY_IGNORE_BATTERY_OPTIMIZATION = "ui_ignore_battery"

        // 收藏贴自动开启只看楼主
        const val KEY_COLLECTED_SEE_LZ = "ui_fav_see_lz"

        // 收藏贴倒序浏览
        const val KEY_COLLECTED_DESC = "ui_fav_desc_sort"

        const val KEY_SHOW_NICKNAME = "ui_show_both_name"

        const val KEY_HOME_SINGLE_FORUM_LIST = "ui_forum_list_in_home"
        const val KEY_HOME_PAGE_SHOW_HISTORY = "ui_history_in_home"

        const val KEY_POST_HIDE_MEDIA = "ui_post_hide_media"
        const val KEY_POST_HIDE_BLOCKED = "ui_post_hide_blocked"
        const val KEY_POST_BLOCK_VIDEO = "ui_block_video"

        const val KEY_LIFT_BOTTOM_BAR = "ui_lift_bottom"

        const val KEY_REDUCE_EFFECT = "ui_reduce_effect"

        // 隐藏回贴入口
        const val KEY_REPLY_HIDE = "ui_reply_hide"
        const val KEY_REPLY_WARNING = "ui_reply_warning"

        const val KEY_OKSIGN_AUTO = "auto_sign"
        const val KEY_OKSIGN_SLOW = "sign_slow_mode"
        const val KEY_OKSIGN_OFFICIAL = "sign_using_official"

        const val KEY_SETUP_FINISHED = "ui_setup"

        /***        END OF BOOLEAN OPTIONS      ***/

        const val KEY_OKSIGN_LAST_TIME = "sign_day"
        const val KEY_OKSIGN_AUTO_TIME = "auto_sign_time"

        private const val KEY_INSTALL_TIME = "se_install_time"
        private const val KEY_UPDATE_TIME = "se_update_time"

        /**
         * 帖子排序方式
         * */
        @IntDef(ForumSortType.BY_REPLY, ForumSortType.BY_SEND)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ForumSortType {
            companion object {
                const val BY_REPLY = 0
                const val BY_SEND = 1
            }
        }

        /**
         * 吧页面悬浮按钮功能
         * */
        @StringDef(ForumFabFunction.POST, ForumFabFunction.REFRESH, ForumFabFunction.BACK_TO_TOP, ForumFabFunction.HIDE)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ForumFabFunction {
            companion object {
                const val POST = "post"
                const val REFRESH = "refresh"
                const val BACK_TO_TOP = "back_to_top"
                const val HIDE = "hide"
            }
        }
    }

    private val dataStore: DataStore<Preferences> = context.dataStore

    /**
     * DataStore 缓存的缓存, 无法保证数据一致性, 重构前先凑合
     * */
    @Volatile private var cache: Preferences = runBlocking { dataStore.data.first() }

    init {
        dataStoreScope.launch {
            dataStore.data.collect {
                cache = it
                ensureActive()
            }
        }
    }

    var userLikeLastRequestUnix: Long
        get() = dataStore.getLong(KEY_LAST_REQUEST_UNIX, 0L)
        set(value) = dataStore.putLong(KEY_LAST_REQUEST_UNIX, value)

    val autoSign: Boolean
        get() = dataStore.getBoolean(KEY_OKSIGN_AUTO, false)

    var autoSignTime: String
        get() = dataStore.getString(KEY_OKSIGN_AUTO_TIME, "09:00")
        set(value) = dataStore.putString(KEY_OKSIGN_AUTO_TIME, value)

    val blockVideo: Boolean
        get() = dataStore.getBoolean(KEY_POST_BLOCK_VIDEO, false)

    var darkMode: Int
        get() = dataStore.getInt(ThemeUtil.KEY_DARK_THEME_MODE, ThemeUtil.DARK_MODE_FOLLOW_SYSTEM)
        set(value) = dataStore.putInt(ThemeUtil.KEY_DARK_THEME_MODE, value)

    val fontScale: Float
        get() = cache[floatPreferencesKey(KEY_FONT_SCALE)] ?:  1.0f

    val hideBlockedContent: Boolean
        get() = cache[booleanPreferencesKey(KEY_POST_HIDE_BLOCKED)] == true

    val liftUpBottomBar: Boolean
        get() = cache[booleanPreferencesKey(KEY_LIFT_BOTTOM_BAR)] == true

    @get:ForumFabFunction
    val forumFabFunction: String
        get() = dataStore.getString(KEY_FORUM_FAB_FUNCTION, ForumFabFunction.HIDE)

    @get:ForumSortType
    val defaultSortType: Int
        get() = dataStore.getInt(KEY_FORUM_SORT_DEFAULT, ForumSortType.BY_REPLY)

    val reduceEffect: Boolean
        get() = cache[booleanPreferencesKey(KEY_REDUCE_EFFECT)] == true

    val useRenderEffect: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            !reduceEffect && !App.INSTANCE.powerManager.isPowerSaveMode
        } else {
            false
        }

    val showBothUsernameAndNickname: Boolean
        get() = cache[booleanPreferencesKey(KEY_SHOW_NICKNAME)] == true

    var signDay: Int
        get() = dataStore.getInt(KEY_OKSIGN_LAST_TIME, -1)
        set(value) = dataStore.putInt(KEY_OKSIGN_LAST_TIME, value)

    val installTime: Long
        get() = cache[longPreferencesKey(KEY_INSTALL_TIME)] ?: App.INSTANCE.packageInfo.firstInstallTime.apply {
            dataStore.putLong(KEY_INSTALL_TIME, this)
        }

    val updateTime: Long
        get() = cache[longPreferencesKey(KEY_UPDATE_TIME)] ?: App.INSTANCE.packageInfo.lastUpdateTime.apply {
            dataStore.putLong(KEY_UPDATE_TIME, this)
        }

    /**
     * File of cropped background for Translucent Theme
     *
     * @see ThemeUtil.isTranslucentTheme
     * */
    val translucentThemeBackgroundFile: Flow<File?> by lazy {
        dataStore.data
            .map {
                it[stringPreferencesKey(ThemeUtil.KEY_TRANSLUCENT_BACKGROUND_FILE)]?.let { file ->
                    File(context.filesDir, file)
                }
            }
            .distinctUntilChanged()
    }

    val setupFinished: Boolean
        get() = cache[booleanPreferencesKey(KEY_SETUP_FINISHED)] == true
}

val Context.appPreferences: AppPreferencesUtils
    get() = AppPreferencesUtils.getInstance(this)