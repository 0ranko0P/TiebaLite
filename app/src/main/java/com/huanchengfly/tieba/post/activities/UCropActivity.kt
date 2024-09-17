package com.huanchengfly.tieba.post.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.yalantis.ucrop.UCrop

private typealias UCropRequest = UCrop

class UCropActivity: com.yalantis.ucrop.UCropActivity(), OnApplyWindowInsetsListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.getBooleanExtra(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS, false)) {
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView, this)
        }
    }

    @SuppressLint("PrivateResource")
    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        // Has navigation bar
        if (insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom > 0) {
            // Apply bottom widget color to navigation bar
            window.navigationBarColor = ContextCompat.getColor(this, com.yalantis.ucrop.R.color.ucrop_color_widget_background)
        }
        v.setOnApplyWindowInsetsListener(null)
        return ViewCompat.onApplyWindowInsets(v, insets)
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
                    Activity.RESULT_OK -> Result.success(UCrop.getOutput(intent!!)!!)

                    Activity.RESULT_CANCELED -> null

                    else -> {
                        val e = intent?.let { UCrop.getError(it) } ?: TiebaUnknownException
                        Result.failure(e)
                    }
                }
            }
        }

        /**
         * Register an start [UCropActivity] for result
         * */
        fun ComponentActivity.registerUCropResult(callback: ActivityResultCallback<Result<Uri>?>): ActivityResultLauncher<UCropRequest> =
            registerForActivityResult(UCropContract(), callback)
    }
}