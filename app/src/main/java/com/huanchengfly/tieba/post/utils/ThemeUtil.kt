package com.huanchengfly.tieba.post.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.Window
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import com.huanchengfly.tieba.post.theme.DefaultColors
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.BlueColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.GreenColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.OrangeColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.PinkColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.PurpleColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.dynamicColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.monetColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.translucentColorScheme
import com.huanchengfly.tieba.post.theme.createTopAppBarColors
import com.huanchengfly.tieba.post.theme.isDarkScheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.models.settings.DarkPreference
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.utils.ThemeUtil._darkConfigUiMode
import com.huanchengfly.tieba.post.utils.ThemeUtil.onUpdateSystemUiMode
import com.huanchengfly.tieba.post.utils.ThemeUtil.overrideDarkMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
     * Is current [Configuration.uiMode] dark.
     *
     * @see onUpdateSystemUiMode
     * */
    private val _darkConfigUiMode: MutableStateFlow<Boolean> = MutableStateFlow(INSTANCE.isAppDark)

    /**
     * User can (temporary) override it with [overrideDarkMode], this state has higher
     * priority over [_darkConfigUiMode] and [DarkPreference]
     * */
    private val _darkModeOverride: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    /**
     * Synced extend ColorScheme for old View UI
     * */
    private val _colorState: MutableState<ExtendedColorScheme> = mutableStateOf(
        if (_darkConfigUiMode.value) DefaultDarkColors else DefaultColors
    )
    val colorState: State<ExtendedColorScheme>
        get() = _colorState

    private fun shouldUseNightMode(darkPreference: DarkPreference, isAppDark: Boolean): Boolean {
        return when (darkPreference) {
            DarkPreference.FOLLOW_SYSTEM -> isAppDark // Follow app dark mode
            DarkPreference.ALWAYS -> true
            DarkPreference.DISABLED -> false
        }
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
     * Implement setAppearanceLightNavigationBars in [WindowInsetsControllerCompat.Impl23]
     *
     * Note: Remove this when minSdk bumped to 26
     * */
    fun WindowInsetsControllerCompat.setAppearanceLightNavigationBars(window: Window, colorScheme: ColorScheme) {
        val isLight: Boolean = isNavigationBarFontDark(colorScheme)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            isAppearanceLightNavigationBars = isLight
        } else {
            window.navigationBarColor = (if (isLight) Color.Black else colorScheme.surfaceContainer).toArgb()
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

    fun isDarkColorScheme(): Boolean = currentColorScheme().isDarkScheme

    fun onUpdateSystemUiMode(context: Context) {
        _darkConfigUiMode.update { context.isAppDark }
        _darkModeOverride.update { null } // clear override
    }

    fun overrideDarkMode(darkMode: Boolean) = _darkModeOverride.update { darkMode }

    fun currentColorScheme(): ColorScheme = colorState.value.colorScheme

    fun getRawTheme() = colorState.value

    // Retrieve latest ColorSchemeDayNight from settings
    private fun savedColorSchemeFlow(themeSettings: Settings<ThemeSettings>, context: Context): Flow<ColorSchemeDayNight> {
        return themeSettings.map {
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

    private fun createBlurColorScheme(colorScheme: ColorScheme, isDark: Boolean) = ExtendedColorScheme(
        colorScheme = colorScheme,
        darkTheme = isDark,
        appBarColors = colorScheme.createTopAppBarColors(
            scrolledContainerColor = colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.86f else 0.74f),
        ),
        navigationContainer = colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.9f else 0.78f)
    )

    fun getDarkModeFlow(settingsRepository: SettingsRepository) = combine(
        flow = settingsRepository.uiSettings.map { it.darkPreference },
        flow2 = _darkConfigUiMode,
        flow3 = _darkModeOverride,
        transform = { darkPreference, isAppDark, overrideDark -> // overrideDark has highest priority
            overrideDark ?: shouldUseNightMode(darkPreference, isAppDark)
        }
    )
    .distinctUntilChanged()
    .flowOn(Dispatchers.Default)

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
            flow2 = settingsRepository.uiSettings.map { it.darkAmoled to it.reduceEffect }.distinctUntilChanged(),
            flow3 = getDarkModeFlow(settingsRepository),
            transform = { colorSchemeDayNight, (darkAmoled, reduceEffect), isDark ->
                val colorScheme = colorSchemeDayNight.getColorScheme(isDark, darkAmoled)
                when {
                    colorScheme.isTranslucent -> ExtendedColorScheme(colorScheme, isDark, navigationContainer = Color.Transparent)

                    !reduceEffect -> createBlurColorScheme(colorScheme, isDark)

                    else -> ExtendedColorScheme(colorScheme, isDark)
                }
                .also { _colorState.value = it }
            }
        )
    }
}