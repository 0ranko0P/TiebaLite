package com.huanchengfly.tieba.post.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.DefaultColors
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorStackTraceScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.TextButton
import com.huanchengfly.tieba.post.utils.CrashLogUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val stackTrace: String = intent.getStringExtra(KEY_THROWABLE)!!
        var dumpLogJob: Job? by mutableStateOf(null)

        setContent {
            val colors = if (isSystemInDarkTheme()) DefaultDarkColors else DefaultColors
            TiebaLiteTheme(colors) {
                ErrorStackTraceScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = colors.windowBackground)
                        .windowInsetsPadding(insets = WindowInsets.safeDrawing),
                    stackTrace = stackTrace
                ) {
                    Button(
                        onClick = {
                            finishAffinity()
                            startActivity(Intent(this@CrashActivity, MainActivityV2::class.java))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.button_restart))
                    }

                    TextButton(
                        onClick = {
                            dumpLogJob = lifecycleScope
                                .launch { CrashLogUtil.dumpLogs(this@CrashActivity, Throwable(stackTrace)) } // Bug
                                .apply { invokeOnCompletion { dumpLogJob = null }}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = dumpLogJob == null
                    ) {
                        Text(text = stringResource(id = R.string.desc_share))
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_THROWABLE = "Throwable"
    }
}