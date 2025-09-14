package com.huanchengfly.tieba.post.arch

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.utils.LocalAccountProvider
import com.huanchengfly.tieba.post.utils.ThemeUtil

abstract class BaseComposeActivity : BaseActivity() {

    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ThemeUtil.onUpdateSystemUiMode(this)

        setContent {
            LocalAccountProvider {
                Content()
            }

            StrongBox {
                val colorState by ThemeUtil.colorState
                val colorScheme = colorState.colorScheme

                LaunchedEffect(colorScheme) {
                    windowInsetsController.isAppearanceLightStatusBars = ThemeUtil.isStatusBarFontDark(colorScheme)
                    windowInsetsController.isAppearanceLightNavigationBars = ThemeUtil.isNavigationBarFontDark(colorScheme)
                }
            }
        }
    }

    @Composable
    abstract fun Content()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Update night theme if needed
        ThemeUtil.onUpdateSystemUiMode(this)
    }

    fun handleCommonEvent(event: CommonUiEvent) {
        when (event) {
            is CommonUiEvent.Toast -> {
                Toast.makeText(this, event.message, event.length).show()
            }

            else -> {}
        }
    }
}

sealed interface CommonUiEvent : UiEvent {

    object NavigateUp : CommonUiEvent

    data class Toast(
        val message: CharSequence,
        val length: Int = android.widget.Toast.LENGTH_SHORT
    ) : CommonUiEvent
}