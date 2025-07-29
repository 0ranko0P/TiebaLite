package com.huanchengfly.tieba.post.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.yalantis.ucrop.UCrop

private typealias UCropRequest = UCrop

/**
 * Fix EdgeToEdge for parent activity
 * */
class UCropActivity: com.yalantis.ucrop.UCropActivity(), OnApplyWindowInsetsListener {

    private val hideBottomBar by unsafeLazy { // NPE
        intent.getBooleanExtra(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS, false)
    }

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetBackground = ContextCompat.getColor(this, com.yalantis.ucrop.R.color.ucrop_color_widget_background)
        val windowBackground = intent.getIntExtra(
            UCrop.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR,
            ContextCompat.getColor(this, com.yalantis.ucrop.R.color.ucrop_color_crop_background)
        )
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(if (hideBottomBar) windowBackground else widgetBackground)
        )
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView, this)
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        v.setOnApplyWindowInsetsListener(null)
        val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        findViewById<ViewGroup>(com.yalantis.ucrop.R.id.ucrop_photobox)
            .updatePadding(left = sysBars.left, right = sysBars.right)

        findViewById<View>(com.yalantis.ucrop.R.id.toolbar).updatePadding(top = sysBars.top)

        if (sysBars.bottom > 0 && !hideBottomBar) {
            findViewById<ViewGroup>(com.yalantis.ucrop.R.id.wrapper_states).apply {
                updatePadding(bottom = sysBars.bottom)
                updateLayoutParams<RelativeLayout.LayoutParams> {
                    height = height + sysBars.bottom
                }
            }
        }

        return WindowInsetsCompat.CONSUMED
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
         * Register an start [UCropActivity] for result
         * */
        fun ComponentActivity.registerUCropResult(callback: ActivityResultCallback<Result<Uri>?>): ActivityResultLauncher<UCropRequest> =
            registerForActivityResult(UCropContract(), callback)
    }
}