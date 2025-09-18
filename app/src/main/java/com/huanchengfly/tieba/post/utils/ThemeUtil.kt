package com.huanchengfly.tieba.post.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import com.huanchengfly.tieba.post.theme.DefaultColors
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.theme.TopBarColors
import com.huanchengfly.tieba.post.theme.colorscheme.BlueColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.GreenColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.OrangeColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.PinkColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.PurpleColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.dynamicColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.monetColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.translucentColorScheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.models.settings.DarkPreference
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.utils.ThemeUtil.DARK_MODE_FOLLOW_SYSTEM
import com.huanchengfly.tieba.post.utils.ThemeUtil.darkModeState
import com.huanchengfly.tieba.post.utils.ThemeUtil.onUpdateSystemUiMode
import com.huanchengfly.tieba.post.utils.ThemeUtil.overrideDarkMode
import com.huanchengfly.tieba.post.utils.ThemeUtil.shouldUseNightMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

object ThemeUtil {

    private val uiModeManager by lazy {
        INSTANCE.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    }

    /**
     * App dark mode state, updated by [onUpdateSystemUiMode].
     * */
    private val _darkModeState: MutableStateFlow<Boolean> = MutableStateFlow(INSTANCE.isAppDark)
    val darkModeState: StateFlow<Boolean> = _darkModeState.asStateFlow()

    /**
     * User can (temporary) override it with [overrideDarkMode], this state has higher
     * priority over [darkModeState] and [shouldUseNightMode]
     * */
    private val _darkModeOverride: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    /**
     * Synced extend ColorScheme for old View UI
     * */
    private val _colorState: MutableState<ExtendedColorScheme> = mutableStateOf(
        if (_darkModeState.value) DefaultDarkColors else DefaultColors
    )
    val colorState: State<ExtendedColorScheme>
        get() = _colorState

    /**
     * Dark mode preferences, Default mode is [DARK_MODE_FOLLOW_SYSTEM]
     *
     * @see shouldUseNightMode
     * */
    const val KEY_DARK_THEME_MODE = "dark_mode"

    const val DARK_MODE_FOLLOW_SYSTEM = 1
    const val DARK_MODE_ALWAYS = 2
    const val DARK_MODE_DISABLED = 4

    fun shouldUseNightMode(darkMode: Int? = DARK_MODE_FOLLOW_SYSTEM): Boolean {
        return when (darkMode) {
            DARK_MODE_ALWAYS -> true
            DARK_MODE_DISABLED -> false
            else -> _darkModeState.value // Follow app dark mode
        }
    }

    fun shouldUseNightMode(darkPreference: DarkPreference?): Boolean {
        return when (darkPreference) {
            DarkPreference.ALWAYS -> true
            DarkPreference.DISABLED -> false
            else -> _darkModeState.value // Follow app dark mode
        }
    }

    fun isTranslucentTheme(colorScheme: ExtendedColorScheme): Boolean {
        // colorScheme.theme == THEME_TRANSLUCENT_LIGHT || colorScheme.theme == THEME_TRANSLUCENT_DARK
        return colorScheme.appBarColors.containerColor == Color.Transparent
    }

    fun isTranslucentTheme(colorScheme: ColorScheme = currentColorScheme()): Boolean {
        return colorScheme.surface == Color.Transparent
    }

    fun isStatusBarFontDark(colorScheme: ColorScheme): Boolean {
        return if (isTranslucentTheme(colorScheme)) {
            // Calculate using TopBar's content color (ColorSchemeKeyTokens.OnSurface)
            !ColorUtils.isColorLight(colorScheme.onSurface.toArgb())
        } else {
            // Calculate using TopBar's container color (ColorSchemeKeyTokens.Surface)
            ColorUtils.isColorLight(colorScheme.surface.toArgb())
        }
    }

    fun isNavigationBarFontDark(colorScheme: ColorScheme): Boolean {
        return if (isTranslucentTheme(colorScheme)) {
            !ColorUtils.isColorLight(colorScheme.onSurface.toArgb())
        } else {
            ColorUtils.isColorLight(colorScheme.surface.toArgb())
        }
    }

    /**
     * Returns application night mode state
     */
    val Context.isAppDark: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /**
     * Returns system night mode state
     *
     * @see     UiModeManager.getNightMode
     * */
    fun Context.isSystemDark(): Boolean = when (uiModeManager.nightMode) {
        UiModeManager.MODE_NIGHT_AUTO,
        UiModeManager.MODE_NIGHT_CUSTOM -> isAppDark

        UiModeManager.MODE_NIGHT_NO -> false

        UiModeManager.MODE_NIGHT_YES -> true

        else -> isAppDark // -1
    }

    fun onUpdateSystemUiMode(context: Context) {
        _darkModeState.update { context.isAppDark }
        _darkModeOverride.update { null } // clear override
    }

    fun overrideDarkMode(darkMode: Boolean) = _darkModeOverride.update { darkMode }

    fun currentColorScheme(): ColorScheme = colorState.value.colorScheme

    fun getRawTheme() = colorState.value

    // Retrieve latest ColorSchemeDayNight from settings
    private fun savedColorSchemeFlow(themeSettings: Settings<ThemeSettings>, context: Context): Flow<ColorSchemeDayNight> {
        return themeSettings.flow.map {
            when (it.theme) {
                Theme.TRANSLUCENT -> translucentColorScheme(it.transColor, it.transDarkColorMode)

                Theme.CUSTOM -> dynamicColorScheme(it.customColor!!.toArgb(), it.customVariant!!)

                Theme.DYNAMIC -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        monetColorScheme(context)
                    } else {
                        BlueColorScheme
                    }
                }

                Theme.BLUE -> BlueColorScheme
                Theme.GREEN -> GreenColorScheme
                Theme.ORANGE -> OrangeColorScheme
                Theme.PINK -> PinkColorScheme
                Theme.PURPLE -> PurpleColorScheme
            }
        }
    }

    private fun createBlurColorScheme(colorScheme: ColorScheme) = ExtendedColorScheme(
        colorScheme = colorScheme,
        appBarColors = TopBarColors(
            containerColor = colorScheme.surface,
            scrolledContainerColor = colorScheme.surfaceContainer.copy(0.64f),
            contentColor = colorScheme.onSurface
        ),
        navigationContainer = colorScheme.surfaceContainer.copy(0.78f)
    )

    /**
     * Latest TiebaLite ColorScheme
     *
     * @see ThemeSettings
     * @see UISettings.darkPreference
     * @see ThemeUtil.overrideDarkMode
     * */
    fun getExtendedColorFlow(settingsRepository: SettingsRepository, context: Context): Flow<ExtendedColorScheme> {
        return combine(
            flow = savedColorSchemeFlow(settingsRepository.themeSettings, context),
            flow2 = settingsRepository.uiSettings.flow.map { it.darkPreference to it.reduceEffect }.distinctUntilChanged(),
            flow3 = _darkModeState,
            flow4 = _darkModeOverride,
            transform = { colorSchemeDayNight, (darkPreference, reduceEffect), isAppDark, overrideDark ->
                // override has highest priority
                val darkMode = overrideDark ?: (isAppDark && shouldUseNightMode(darkPreference))
                val colorScheme = colorSchemeDayNight.getColorScheme(isDark = darkMode, isAmoled = false)
                when {
                    colorScheme.isTranslucent -> ExtendedColorScheme(colorScheme, navigationContainer = Color.Transparent)

                    !reduceEffect -> createBlurColorScheme(colorScheme)

                    else -> ExtendedColorScheme(colorScheme)
                }
                .also { _colorState.value = it }
            }
        )
        .flowOn(Dispatchers.Default)
    }
}