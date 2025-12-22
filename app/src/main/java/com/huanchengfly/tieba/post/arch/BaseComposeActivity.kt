package com.huanchengfly.tieba.post.arch

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil.setAppearanceLightNavigationBars

abstract class BaseComposeActivity : BaseActivity() {

    protected val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ThemeUtil.onUpdateSystemUiMode(this)

        setContent {
            StrongBox {
                val colorState by ThemeUtil.colorState
                val colorScheme = colorState.colorScheme

                LaunchedEffect(colorScheme) {
                    windowInsetsController.setAppearanceLightStatusBars(ThemeUtil.isStatusBarFontDark(colorScheme))
                    windowInsetsController.setAppearanceLightNavigationBars(window, colorScheme)
                }
            }
            Content()
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

    object FeatureUnavailable : CommonUiEvent

    object NavigateUp : CommonUiEvent

    class ToastError(override val message: CharSequence, val code: Int): Toast(message) {
        constructor(e: Throwable) : this(message = e.getErrorMessage(), code = e.getErrorCode())
    }

    open class Toast(
        open val message: CharSequence,
        val length: Int = android.widget.Toast.LENGTH_SHORT
    ) : CommonUiEvent
}