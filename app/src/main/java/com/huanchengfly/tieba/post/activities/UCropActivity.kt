package com.huanchengfly.tieba.post.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.ui.graphics.toArgb
import com.huanchengfly.tieba.post.activities.UCropActivity.Companion.registerUCropResult
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.theme.TiebaBlue
import com.huanchengfly.tieba.post.utils.ColorUtils
import com.yalantis.ucrop.UCrop

private typealias UCropRequest = UCrop

/**
 * Extended UCropActivity
 *
 * @see registerUCropResult
 * */
class UCropActivity: com.yalantis.ucrop.UCropActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Setup StatusBar, ToolBar and ColorOnToolBar color based on EXTRA_TOOL_BAR_COLOR
        val toolbarColor = intent.getIntExtra(UCrop.Options.EXTRA_TOOL_BAR_COLOR, TiebaBlue.toArgb())
        val isLightColor = ColorUtils.isColorLight(toolbarColor)
        intent.putExtra(UCrop.Options.EXTRA_STATUS_BAR_LIGHT, isLightColor)
        intent.putExtra(UCrop.Options.EXTRA_TOOL_BAR_COLOR, toolbarColor)
        intent.putExtra(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, if (isLightColor) Color.BLACK else Color.WHITE)

        super.onCreate(savedInstanceState)
    }

    companion object {

        class UCropContract: ActivityResultContract<UCropRequest, Result<Uri>?>() {

            override fun createIntent(context: Context, input: UCropRequest): Intent {
                return  input.getIntent(context).apply {
                    // Override ucrop activity
                    setClass(context, UCropActivity::class.java)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Result<Uri>? {
                return when (resultCode) {
                    RESULT_OK -> Result.success(UCrop.getOutput(intent!!)!!)

                    RESULT_CANCELED -> null

                    else -> {
                        val e = intent?.let { UCrop.getError(it) } ?: TiebaUnknownException
                        Result.failure(e)
                    }
                }
            }
        }

        /**
         * Register a request to start [UCropActivity] for result.
         * */
        fun ComponentActivity.registerUCropResult(callback: ActivityResultCallback<Result<Uri>?>): ActivityResultLauncher<UCropRequest> =
            registerForActivityResult(UCropContract(), callback)
    }
}