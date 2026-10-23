@file:Suppress("NOTHING_TO_INLINE")

package com.huanchengfly.tieba.post.core.ui.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.fastIsFinite
import androidx.core.content.getSystemService

/**
 * 屏幕工具类
 *
 * 原位置: com.huanchengfly.tieba.post.utils.DisplayUtil
 *
 * @author 0ranko0P
 * @author HuanChengFly
 *
 * @since 3.8.1 α
 * */
object DisplayUtil {

    const val GESTURE_3BUTTON = 48

    const val GESTURE_DEFAULT = 24

    const val GESTURE_NONE = 0

    fun WindowInsets.gestureType(density: Density): Int {
        val heightDp = with(density) { getBottom(density).toDp() }.value
        return if (heightDp >= GESTURE_3BUTTON) {
            GESTURE_3BUTTON
        } else if (heightDp >= GESTURE_DEFAULT) {
            GESTURE_DEFAULT
        } else {
            GESTURE_NONE
        }
    }

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
     *
     * @throws IllegalStateException 传入了EM值
     */
    @Composable
    @Stable
    inline fun TextUnit.sp2px(): Int = with(LocalDensity.current) { roundToPx() }

    /**
     * 将SP值相加
     *
     * @throws IllegalStateException 传入了EM值
     * */
    @Composable
    @Stable
    inline operator fun TextUnit.plus(other: TextUnit): TextUnit = if (other.isSpecified) {
        with(LocalDensity.current) { (toDp() + other.toDp()).toSp() }
    } else {
        this
    }

    inline fun IntSize.toDpSize(density: Density): DpSize = if (this != IntSize.Zero) {
        with(density) { DpSize(width = width.toDp(), height = height.toDp()) }
    } else {
        DpSize.Zero
    }

    val IntSize.aspectRatio: Float?
        get() = (width / height.toFloat()).takeIf { it.fastIsFinite() }

    @Composable
    @Stable
    inline fun Offset.toDpOffset(): DpOffset = if (this != Offset.Zero) {
        with(LocalDensity.current) { DpOffset(x = x.toDp(), y = y.toDp()) }
    } else {
        DpOffset.Zero
    }

    /**
     * @return 屏幕宽高大小
     */
    fun getScreenPixels(context: Context, ignoreOrientation: Boolean = true): IntSize {
        val manager: WindowManager? = if (context is Activity) {
            context.windowManager
        } else {
            context.getSystemService<WindowManager>()
        }

        val size = if (manager != null) {
            val point = Point()
            manager.defaultDisplay.getRealSize(point)
            IntSize(point.x, point.y)
        } else {
            // Size without Navigation bar
            Resources.getSystem().displayMetrics.let { IntSize(it.widthPixels, it.heightPixels) }
        }
        if (ignoreOrientation) {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return IntSize(width = size.height, height = size.width)
            }
        }
        return size
    }
}