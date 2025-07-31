package com.huanchengfly.tieba.post.arch

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.utils.LocalAccountProvider
import com.huanchengfly.tieba.post.utils.ThemeUtil

abstract class BaseComposeActivity : BaseActivity() {

    private var _nightMode by mutableStateOf(
        ThemeUtil.shouldUseNightMode(appPreferences.darkMode)
    )

    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            TiebaLiteTheme(darkTheme = _nightMode) {
                val colors = LocalExtendedColors.current
                LaunchedEffect(colors) {
                    windowInsetsController.isAppearanceLightStatusBars = ThemeUtil.isStatusBarFontDark(colors)
                    windowInsetsController.isAppearanceLightNavigationBars = ThemeUtil.isNavigationBarFontDark(colors)
                }

                LocalAccountProvider {
                    Content()
                }
            }
        }
    }

    @Composable
    abstract fun Content()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Update night theme if needed
        setNightMode(ThemeUtil.shouldUseNightMode(appPreferences.darkMode))
    }

    fun handleCommonEvent(event: CommonUiEvent) {
        when (event) {
            is CommonUiEvent.Toast -> {
                Toast.makeText(this, event.message, event.length).show()
            }

            else -> {}
        }
    }

    companion object {

        // Expose night mode setter for settings page
        fun Context.setNightMode(nightMode: Boolean) {
            val activity = findActivity() ?: return
            with(activity as BaseComposeActivity) {
                if (_nightMode xor nightMode) _nightMode = nightMode
            }
        }
    }
}

sealed interface CommonUiEvent : UiEvent {
    object ScrollToTop : CommonUiEvent

    object NavigateUp : CommonUiEvent

    data class Toast(
        val message: CharSequence,
        val length: Int = android.widget.Toast.LENGTH_SHORT
    ) : CommonUiEvent

    @Composable
    fun BaseViewModel<*, *, *, *>.bindScrollToTopEvent(lazyListState: LazyListState) {
        onEvent<ScrollToTop> {
            lazyListState.scrollToItem(0, 0)
        }
    }
}