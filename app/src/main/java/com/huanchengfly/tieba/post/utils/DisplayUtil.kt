package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.huanchengfly.tieba.post.App
import kotlin.math.roundToInt

object DisplayUtil {

    const val GESTURE_3BUTTON = 48

    const val GESTURE_DEFAULT = 24

    const val GESTURE_NONE = 0

    /**
     * 将dp值转换为px值
     */
    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 将px值转换为sp值
     */
    fun px2sp(context: Context, pxValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    /**
     * 将sp值转换为px值
     */
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    /**
     * @return 屏幕宽高大小
     */
    fun getScreenPixels(context: Context?): IntSize {
        val manager: WindowManager? = if (context != null && context is Activity) {
            context.windowManager
        } else {
            ContextCompat.getSystemService(App.INSTANCE, WindowManager::class.java)
        }

        return if (manager != null) {
            val point = Point()
            manager.defaultDisplay.getRealSize(point)
            IntSize(point.x, point.y)
        } else {
            // Size without Navigation bar
            Resources.getSystem().displayMetrics.let { IntSize(it.widthPixels, it.heightPixels) }
        }
    }

    fun IntSize.toDpSize(density: Density): DpSize = with(density) {
        DpSize(width = width.toDp(), height = height.toDp())
    }

    fun WindowInsets.gestureType(density: Density): Int {
        val height = with(density) { getBottom(density).toDp() }.value.roundToInt()
        return if (height >= GESTURE_3BUTTON) {
            GESTURE_3BUTTON
        } else if (height >= GESTURE_DEFAULT) {
            GESTURE_DEFAULT
        } else {
            GESTURE_NONE
        }
    }

    /**
     * @see View.setOnApplyWindowInsetsListener
     * */
    inline fun View.doOnApplyWindowInsets(crossinline onApply: View.(insets: WindowInsetsCompat) -> Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            ViewCompat.setOnApplyWindowInsetsListener(v, null)
            if (onApply(insets)) WindowInsetsCompat.CONSUMED else insets
        }
    }
}