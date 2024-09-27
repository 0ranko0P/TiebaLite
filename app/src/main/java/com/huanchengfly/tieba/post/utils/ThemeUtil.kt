package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.annotation.StyleRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.dataStoreScope
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.getString
import com.huanchengfly.tieba.post.putBoolean
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import kotlinx.coroutines.launch
import java.util.Locale

object ThemeUtil {
    val themeState: MutableState<String> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableStateOf(
            if (App.isInitialized) dataStore.getString(KEY_THEME, THEME_DEFAULT)
            else THEME_DEFAULT
        )
    }

    const val TAG = "ThemeUtil"

    const val KEY_THEME = "theme"
    const val KEY_DARK_THEME = "dark_theme"
    const val KEY_OLD_THEME = "old_theme"
    const val KEY_USE_DYNAMIC_THEME = "useDynamicColorTheme"

    const val KEY_CUSTOM_PRIMARY_COLOR = "custom_primary_color" // Int: Custom ARGB 主题色
    const val KEY_CUSTOM_STATUS_BAR_FONT_DARK = "custom_status_bar_font_dark"
    const val KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR = "custom_toolbar_primary_color" // Bool: 顶栏跟随主题色

    /**
     * Bool: is Dark/Light Translucent Theme
     * */
    const val KEY_TRANSLUCENT_THEME_DARK_COLOR = "translucent_dark_color"
    const val KEY_TRANSLUCENT_BACKGROUND_FILE = "translucent_background_path"
    const val THEME_TRANSLUCENT = "translucent"
    const val THEME_TRANSLUCENT_LIGHT = "translucent_light"
    const val THEME_TRANSLUCENT_DARK = "translucent_dark"

    const val THEME_CUSTOM = "custom"
    const val THEME_DEFAULT = "tieba"
    const val THEME_BLACK = "black"
    const val THEME_BLUE = "blue"
    const val THEME_PURPLE = "purple"
    const val THEME_PINK = "pink"
    const val THEME_RED = "red"
    const val THEME_BLUE_DARK = "blue_dark"
    const val THEME_GREY_DARK = "grey_dark"
    const val THEME_AMOLED_DARK = "amoled_dark"

    val dataStore: DataStore<Preferences>
        get() = INSTANCE.dataStore

    private fun refreshUI(activity: Activity?) {
        if (activity is BaseActivity) {
            activity.refreshUIIfNeed()
            return
        }
        ThemeUtils.refreshUI(activity)
    }

    private fun getOldTheme(): String {
        val oldTheme =
            dataStore.getString(KEY_OLD_THEME, THEME_DEFAULT).takeUnless { isNightMode(it) }

        return oldTheme ?: THEME_DEFAULT
    }

    fun switchTheme(newTheme: String, recordOldTheme: Boolean = true) {
        dataStoreScope.launch {
            dataStore.edit {
                if (recordOldTheme) {
                    val oldTheme = getRawTheme()
                    if (!isNightMode(oldTheme)) {
                        it[stringPreferencesKey(KEY_OLD_THEME)] = oldTheme
                    }
                }
                it[stringPreferencesKey(KEY_THEME)] = newTheme
            }
            themeState.value = newTheme
        }
    }

    fun setUseDynamicTheme(useDynamicTheme: Boolean) {
        dataStore.putBoolean(KEY_USE_DYNAMIC_THEME, useDynamicTheme)
    }

    fun isUsingDynamicTheme(): Boolean = dataStore.getBoolean(KEY_USE_DYNAMIC_THEME, false)

    fun switchNightMode() {
        if (isNightMode()) {
            switchTheme(getOldTheme(), recordOldTheme = false)
        } else {
            switchTheme(dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK))
        }
    }

    fun switchToNightMode(context: Activity, recreate: Boolean) {
        switchTheme(dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK))
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmOverloads
    fun switchFromNightMode(context: Activity, recreate: Boolean = true) {
        switchTheme(getOldTheme(), recordOldTheme = false)
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmStatic
    fun isNightMode(): Boolean {
        return isNightMode(getRawTheme())
    }

    @JvmStatic
    fun isNightMode(theme: String): Boolean {
        return theme.lowercase(Locale.getDefault()).contains("dark") && !theme.contains(
            THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isTranslucentTheme(): Boolean {
        return isTranslucentTheme(getRawTheme())
    }

    @JvmStatic
    fun isTranslucentTheme(theme: String): Boolean {
        return theme.equals(
            THEME_TRANSLUCENT,
            ignoreCase = true
        ) || theme.contains(
            THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isDynamicTheme(theme: String): Boolean {
        return theme.endsWith("_dynamic")
    }

    fun isStatusBarFontDark(): Boolean {
        val theme = getRawTheme()
        val dataStore = INSTANCE.dataStore
        val isToolbarPrimaryColor: Boolean = INSTANCE.appPreferences.toolbarPrimaryColor
        return if (theme == THEME_CUSTOM) {
            dataStore.getBoolean(KEY_CUSTOM_STATUS_BAR_FONT_DARK, false)
        } else if (isTranslucentTheme(theme)) {
            theme.contains("dark", ignoreCase = true)
        } else if (!isToolbarPrimaryColor) {
            !isNightMode(theme)
        } else {
            false
        }
    }

    fun isNavigationBarFontDark(): Boolean {
        return !isNightMode()
    }

    fun setTheme(context: Activity) {
        val nowTheme = getCurrentTheme()
        context.setTheme(getThemeByName(nowTheme))
    }

    @JvmOverloads
    fun getCurrentTheme(
        theme: String = getRawTheme(),
        checkDynamic: Boolean = false,
    ): String {
        var nowTheme = theme
        if (isTranslucentTheme(nowTheme)) {
            val isDarkTranslucent = INSTANCE.appPreferences.getBoolean(KEY_TRANSLUCENT_THEME_DARK_COLOR, false)
            nowTheme = if (isDarkTranslucent) {
                THEME_TRANSLUCENT_DARK
            } else {
                THEME_TRANSLUCENT_LIGHT
            }
        } else if (checkDynamic && isUsingDynamicTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            nowTheme = "${nowTheme}_dynamic"
        }
        return nowTheme
    }

    @JvmStatic
    fun setTranslucentDialogBackground(view: View?) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        view.backgroundTintList = null
        view.setBackgroundColor(
            ThemeUtils.getColorById(
                view.context,
                R.color.theme_color_card_grey_dark
            )
        )
    }

    @StyleRes
    private fun getThemeByName(themeName: String): Int {
        return when (themeName.lowercase(Locale.getDefault())) {
            THEME_TRANSLUCENT, THEME_TRANSLUCENT_LIGHT -> R.style.TiebaLite_Translucent_Light
            THEME_TRANSLUCENT_DARK -> R.style.TiebaLite_Translucent_Dark
            THEME_DEFAULT -> R.style.TiebaLite_Tieba
            THEME_BLACK -> R.style.TiebaLite_Black
            THEME_PURPLE -> R.style.TiebaLite_Purple
            THEME_PINK -> R.style.TiebaLite_Pink
            THEME_RED -> R.style.TiebaLite_Red
            THEME_BLUE_DARK -> R.style.TiebaLite_Dark_Blue
            THEME_GREY_DARK -> R.style.TiebaLite_Dark_Grey
            THEME_AMOLED_DARK -> R.style.TiebaLite_Dark_Amoled
            THEME_CUSTOM -> R.style.TiebaLite_Custom
            else -> R.style.TiebaLite_Tieba
        }
    }

    @JvmStatic
    fun getRawTheme(): String {
        val theme = themeState.value
        return when (theme.lowercase(Locale.getDefault())) {
            THEME_TRANSLUCENT,
            THEME_TRANSLUCENT_LIGHT,
            THEME_TRANSLUCENT_DARK,
            THEME_CUSTOM,
            THEME_DEFAULT,
            THEME_BLACK,
            THEME_PURPLE,
            THEME_PINK,
            THEME_RED,
            THEME_BLUE_DARK,
            THEME_GREY_DARK,
            THEME_AMOLED_DARK,
            -> theme.lowercase(Locale.getDefault())

            else -> THEME_DEFAULT
        }
    }
}