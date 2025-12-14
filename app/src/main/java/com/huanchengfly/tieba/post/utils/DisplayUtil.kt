@file:Suppress("NOTHING_TO_INLINE")

package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.huanchengfly.tieba.post.App

object DisplayUtil {

    const val GESTURE_3BUTTON = 48

    const val GESTURE_DEFAULT = 24

    const val GESTURE_NONE = 0

    val isLandscape: Boolean
        @ReadOnlyComposable
        @Composable
        get() = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

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

    /**
     * @return 屏幕宽高大小
     */
    fun getScreenPixels(context: Context, ignoreOrientation: Boolean = true): IntSize {
        val manager: WindowManager? = if (context is Activity) {
            context.windowManager
        } else {
            ContextCompat.getSystemService(App.INSTANCE, WindowManager::class.java)
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

    fun IntSize.toDpSize(density: Density): DpSize = if (this != IntSize.Zero) {
        with(density) { DpSize(width = width.toDp(), height = height.toDp()) }
    } else {
        DpSize.Zero
    }

    fun Offset.toDpOffset(density: Density): DpOffset = if (this != Offset.Zero) {
        with(density) { DpOffset(x = x.toDp(), y = y.toDp()) }
    } else {
        DpOffset.Zero
    }

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
     * @see View.setOnApplyWindowInsetsListener
     * */
    inline fun View.doOnApplyWindowInsets(crossinline onApply: View.(insets: WindowInsetsCompat) -> Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            if (onApply(insets)) WindowInsetsCompat.CONSUMED else insets
        }
    }
}