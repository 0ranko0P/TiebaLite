package com.huanchengfly.tieba.post.components.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.enableBackgroundBlur
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

abstract class ResultDialog<T>() : DialogFragment() {

    protected val mResult = Channel<T>(capacity = 1)

    protected var backgroundColor by mutableStateOf(Color.Transparent)

    override fun getTheme(): Int = R.style.Dialog_RequestPermissionTip

    open fun getTAG(): String? = null

    @Composable
    abstract fun BoxScope.ContentView(savedInstanceState: Bundle?)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        super.onCreate(savedInstanceState)
        return AlertDialog.Builder(context, theme)
            .setView(ComposeView(context).apply {
                setContent {
                    val theme: ExtendedColors by ThemeUtil.themeState
                    TiebaLiteTheme(theme) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(backgroundColor)
                                .windowInsetsPadding(WindowInsets.systemBars)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContentView(savedInstanceState)
                        }
                    }
                }
            })
            .create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            onSetupWindow(window = this)
        }
    }

    open fun show(activityContext: Context): ResultDialog<T> {
        val fragmentActivity = activityContext.findFragmentActivityOrThrow()
        show(fragmentActivity.supportFragmentManager, this.tag)
        return this
    }

    open suspend fun receiveResult(context: CoroutineContext = Dispatchers.Default): ChannelResult<T> {
        return withContext(context) {
            mResult.receiveCatching()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mResult.close()
    }

    protected open fun onSetupWindow(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes = window.attributes.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT

            backgroundColor = if (this.enableBackgroundBlur(window.context) != null) {
                ThemeUtil.getRawTheme().windowBackground.copy(0.2f)
            } else {
                ThemeUtil.getRawTheme().windowBackground.copy(0.86f)
            }
        }
    }

    companion object {

        private fun Context.findFragmentActivityOrThrow(): FragmentActivity {
            val activity = findActivity() ?: this
            if (activity !is FragmentActivity) {
                throw IllegalArgumentException("${activity::class.simpleName} not an instance of FragmentActivity")
            }
            return activity
        }
    }
}