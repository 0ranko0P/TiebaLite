package com.huanchengfly.tieba.post.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorStackTraceScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
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
            TiebaLiteTheme {
                ErrorStackTraceScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(insets = WindowInsets.safeDrawing),
                    stackTrace = stackTrace
                ) {
                    PositiveButton(
                        textRes = R.string.button_restart,
                        onClick = {
                            finishAffinity()
                            startActivity(Intent(applicationContext, MainActivityV2::class.java))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    TextButton(
                        onClick = {
                            dumpLogJob = lifecycleScope
                                .launch {
                                    CrashLogUtil.dumpLogs(applicationContext, Throwable(stackTrace)) // Bug
                                }
                                .apply { invokeOnCompletion { dumpLogJob = null } }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = dumpLogJob == null
                    ) {
                        Text(text = stringResource(id = R.string.title_share))
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_THROWABLE = "Throwable"
    }
}