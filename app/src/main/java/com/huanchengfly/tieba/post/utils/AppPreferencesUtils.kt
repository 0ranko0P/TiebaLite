package com.huanchengfly.tieba.post.utils

import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.StringDef
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.getFloat
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.getLong
import com.huanchengfly.tieba.post.getString
import com.huanchengfly.tieba.post.putBoolean
import com.huanchengfly.tieba.post.putInt
import com.huanchengfly.tieba.post.putLong
import com.huanchengfly.tieba.post.putString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

        // 隐藏回贴入口
        const val KEY_REPLY_HIDE = "ui_reply_hide"
        const val KEY_REPLY_WARNING = "ui_reply_warning"

        const val KEY_USE_WEB_VIEW = "use_webview"
        const val KEY_WEB_VIEW_CUSTOM_TAB = "use_custom_tabs"

        const val KEY_OKSIGN_AUTO = "auto_sign"
        const val KEY_OKSIGN_SLOW = "sign_slow_mode"
        const val KEY_OKSIGN_OFFICIAL = "sign_using_official"

        const val KEY_SETUP_FINISHED = "ui_setup"

        /***        END OF BOOLEAN OPTIONS      ***/

        const val KEY_OKSIGN_LAST_TIME = "sign_day"
        const val KEY_OKSIGN_AUTO_TIME = "auto_sign_time"

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

    var userLikeLastRequestUnix: Long
        get() = dataStore.getLong(KEY_LAST_REQUEST_UNIX, 0L)
        set(value) = dataStore.putLong(KEY_LAST_REQUEST_UNIX, value)

    var ignoreBatteryOptimizationsDialog: Boolean
        get() = dataStore.getBoolean(KEY_IGNORE_BATTERY_OPTIMIZATION, false)
        set(value) = dataStore.putBoolean(KEY_IGNORE_BATTERY_OPTIMIZATION, value)

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
        get() = dataStore.getFloat(KEY_FONT_SCALE, 1.0f)

    val hideBlockedContent: Boolean
        get() = dataStore.getBoolean(KEY_POST_HIDE_BLOCKED, defaultValue = false)

    val liftUpBottomBar: Boolean
        get() = dataStore.getBoolean(KEY_LIFT_BOTTOM_BAR, false)

    @get:ForumFabFunction
    val forumFabFunction: String
        get() = dataStore.getString(KEY_FORUM_FAB_FUNCTION, ForumFabFunction.HIDE)

    @get:ForumSortType
    val defaultSortType: Int
        get() = dataStore.getInt(KEY_FORUM_SORT_DEFAULT, ForumSortType.BY_REPLY)

    val showBothUsernameAndNickname: Boolean
        get() = dataStore.getBoolean(KEY_SHOW_NICKNAME, false)

    var signDay: Int
        get() = dataStore.getInt(KEY_OKSIGN_LAST_TIME, -1)
        set(value) = dataStore.putInt(KEY_OKSIGN_LAST_TIME, value)

    /**
     * File of cropped background for Translucent Theme
     *
     * @see ThemeUtil.isTranslucentTheme
     * */
    val translucentThemeBackgroundFile: Flow<File?> by lazy {
        dataStore.data
            .map {
                val file = it[stringPreferencesKey(ThemeUtil.KEY_TRANSLUCENT_BACKGROUND_FILE)]
                file?.run { File(context.filesDir, this) }
            }
            .distinctUntilChanged()
    }

    val useCustomTabs: Boolean
        get() = dataStore.getBoolean(KEY_WEB_VIEW_CUSTOM_TAB, true)

    val useWebView: Boolean
        get() = dataStore.getBoolean(KEY_USE_WEB_VIEW, true)

    val setupFinished: Boolean
        get() = dataStore.getBoolean(KEY_SETUP_FINISHED, false)
}

val Context.appPreferences: AppPreferencesUtils
    get() = AppPreferencesUtils.getInstance(this)