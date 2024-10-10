package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.huanchengfly.tieba.post.theme.DarkGreyColors
import com.huanchengfly.tieba.post.theme.DefaultColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

object ThemeUtil {
    val themeState: MutableState<ExtendedColors> = mutableStateOf(DefaultColors)

    const val TAG = "ThemeUtil"

    const val KEY_THEME = "theme"
    const val KEY_DARK_THEME = "dark_theme"
    const val KEY_OLD_THEME = "old_theme"

    const val KEY_CUSTOM_PRIMARY_COLOR = "custom_primary_color" // Int: Custom ARGB 主题色
    const val KEY_CUSTOM_STATUS_BAR_FONT_DARK = "custom_status_bar_font_dark"
    const val KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR = "custom_toolbar_primary_color" // Bool: 顶栏跟随主题色

    const val KEY_TRANSLUCENT_BACKGROUND_FILE = "translucent_background_path"

    const val THEME_TRANSLUCENT_LIGHT = "translucent_light"
    const val THEME_TRANSLUCENT_DARK = "translucent_dark"
    const val THEME_CUSTOM = "custom"
    const val THEME_DYNAMIC = "dynamic"
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

    private fun getOldTheme(): String {
        val oldTheme =
            dataStore.getString(KEY_OLD_THEME, THEME_DEFAULT).takeUnless { isNightMode(it) }

        return oldTheme ?: THEME_DEFAULT
    }

    fun switchTheme(newTheme: String, recordOldTheme: Boolean = true) {
        dataStoreScope.launch {
            dataStore.edit {
                it[stringPreferencesKey(KEY_THEME)] = newTheme
            }
        }
    }

    fun switchNightMode(current: ExtendedColors) = MainScope().launch {
        val data = dataStore.data.first()
        val nightTheme = data[stringPreferencesKey(KEY_DARK_THEME)] ?: THEME_AMOLED_DARK
        val theme = data[stringPreferencesKey(KEY_THEME)] ?: THEME_DEFAULT
        if (current.isNightMode) {
            switchTheme(theme)
        } else {
            switchTheme(nightTheme)
        }
    }

    fun switchToNightMode(context: Activity, recreate: Boolean) {
        switchTheme(dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK))
    }

    @JvmOverloads
    fun switchFromNightMode(context: Activity, recreate: Boolean = true) {
        switchTheme(getOldTheme(), recordOldTheme = false)
    }

    @JvmStatic
    fun isNightMode(): Boolean = getRawTheme().isNightMode

    @JvmStatic
    fun isNightMode(theme: String): Boolean {
        return theme.endsWith("dark")
    }

    @JvmStatic
    fun isTranslucentTheme(theme: ExtendedColors = getRawTheme()): Boolean {
        return theme.theme == THEME_TRANSLUCENT_LIGHT || theme.theme == THEME_TRANSLUCENT_DARK
    }

    fun isStatusBarFontDark(theme: ExtendedColors = getRawTheme()): Boolean {
        val dataStore = INSTANCE.dataStore
        val isToolbarPrimaryColor: Boolean = INSTANCE.appPreferences.toolbarPrimaryColor
        return if (theme.theme == THEME_CUSTOM) {
            dataStore.getBoolean(KEY_CUSTOM_STATUS_BAR_FONT_DARK, false)
        } else if (isTranslucentTheme(theme)) {
            theme.isNightMode
        } else if (!isToolbarPrimaryColor) {
            !theme.isNightMode
        } else {
            false
        }
    }

    fun isNavigationBarFontDark(): Boolean {
        return !isNightMode()
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
        view.setBackgroundColor(DarkGreyColors.card.toArgb())
    }

    @JvmStatic
    fun getRawTheme() = themeState.value
}