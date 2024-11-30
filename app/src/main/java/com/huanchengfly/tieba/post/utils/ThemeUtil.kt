package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.view.View
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.material.color.MaterialColors
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.setNightMode
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.dataStoreScope
import com.huanchengfly.tieba.post.putColor
import com.huanchengfly.tieba.post.theme.DarkGreyColors
import com.huanchengfly.tieba.post.theme.DefaultColors
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import kotlinx.coroutines.launch

object ThemeUtil {
    val themeState: MutableState<ExtendedColors> = mutableStateOf(
        if (shouldUseNightMode(INSTANCE.appPreferences.darkMode)) DefaultDarkColors else DefaultColors
    )

    const val TAG = "ThemeUtil"

    const val KEY_THEME = "theme"
    const val KEY_DARK_THEME = "dark_theme"

    /**
     * Dark mode preferences, Default mode is [DARK_MODE_FOLLOW_SYSTEM]
     *
     * @see shouldUseNightMode
     * @see App.isSystemNight
     * */
    const val KEY_DARK_THEME_MODE = "dark_mode"

    const val KEY_CUSTOM_PRIMARY_COLOR = "custom_primary_color" // Int: Custom ARGB 主题色

    // Bool: 顶栏跟随主题色
    const val KEY_TINT_TOOLBAR = "theme_tint_toolbar"

    const val KEY_TRANSLUCENT_BACKGROUND_FILE = "translucent_background_path"

    // Day Themes
    const val THEME_TRANSLUCENT_LIGHT = "translucent_light_text"
    const val THEME_TRANSLUCENT_DARK = "translucent_dark_text"
    const val THEME_CUSTOM = "custom"
    const val THEME_DYNAMIC = "dynamic"
    const val THEME_BLACK = "black"
    const val THEME_BLUE = "blue"
    const val THEME_PURPLE = "purple"
    const val THEME_PINK = "pink"
    const val THEME_RED = "red"

    // Night Themes
    const val THEME_BLUE_DARK = "blue_dark"
    const val THEME_GREY_DARK = "grey_dark"
    const val THEME_AMOLED_DARK = "amoled_dark"

    const val DARK_MODE_FOLLOW_SYSTEM = 1
    const val DARK_MODE_ALWAYS = 2
    const val DARK_MODE_DISABLED = 4

    fun switchTheme(newTheme: String, activityContext: Context? = null) {
        val isNightTheme = newTheme.endsWith("_dark")
        dataStoreScope.launch {
            INSTANCE.dataStore.edit {
                val key = if (isNightTheme) KEY_DARK_THEME else KEY_THEME
                it[stringPreferencesKey(key)] = newTheme
            }
            // Notify night mode manually!
            activityContext?.setNightMode(isNightTheme)
        }
    }

    fun switchCustomTheme(primaryColor: Color, activityContext: Context?) {
        dataStoreScope.launch {
            INSTANCE.dataStore.edit {
                it.putColor(KEY_CUSTOM_PRIMARY_COLOR, primaryColor)
                it[stringPreferencesKey(KEY_THEME)] = THEME_CUSTOM
            }
            activityContext?.setNightMode(false)
        }
    }

    @JvmStatic
    fun isNightMode(): Boolean = getRawTheme().isNightMode

    fun shouldUseNightMode(darkMode: Int? = DARK_MODE_FOLLOW_SYSTEM): Boolean {
        return when (darkMode) {
            DARK_MODE_ALWAYS -> true
            DARK_MODE_DISABLED -> false
            else -> App.isSystemNight // Follow system night mode
        }
    }

    @JvmStatic
    fun isTranslucentTheme(theme: ExtendedColors = getRawTheme()): Boolean {
        return theme.theme == THEME_TRANSLUCENT_LIGHT || theme.theme == THEME_TRANSLUCENT_DARK
    }

    fun isStatusBarFontDark(theme: ExtendedColors): Boolean {
        return if (isTranslucentTheme(theme)) {
            theme.theme == THEME_TRANSLUCENT_DARK
        } else {
            !MaterialColors.isColorLight(theme.onTopBar.toArgb())
        }
    }

    fun isNavigationBarFontDark(theme: ExtendedColors): Boolean = !theme.isNightMode

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