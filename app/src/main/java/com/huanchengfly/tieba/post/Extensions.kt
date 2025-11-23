package com.huanchengfly.tieba.post

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.utils.GsonUtil
import com.huanchengfly.tieba.post.utils.MD5Util
import java.io.File
import kotlin.math.roundToInt

val PaddingNone = PaddingValues(Dp.Hairline)

fun PaddingValues.copy(
    direction: LayoutDirection,
    start: Dp = calculateStartPadding(direction),
    top: Dp = calculateTopPadding(),
    end: Dp = calculateEndPadding(direction),
    bottom: Dp = calculateBottomPadding()
): PaddingValues = PaddingValues(
    start = start,
    top = top,
    end = end,
    bottom = bottom
).takeUnless { it == PaddingNone } ?: PaddingNone

private val Context.scaledDensity: Float
    get() = resources.displayMetrics.scaledDensity

fun Float.dpToPx(): Int =
    dpToPxFloat().roundToInt()

fun Float.dpToPxFloat(): Float =
    this * App.ScreenInfo.DENSITY + 0.5f

fun Float.spToPx(context: Context = App.INSTANCE): Int =
    (this * context.scaledDensity + 0.5f).roundToInt()

fun Float.spToPxFloat(context: Context = App.INSTANCE): Float =
    this * context.scaledDensity + 0.5f

fun Float.pxToDp(): Int =
    (this / App.ScreenInfo.DENSITY + 0.5f).roundToInt()

fun Float.pxToDpFloat(): Float =
    this / App.ScreenInfo.DENSITY + 0.5f

fun Float.pxToSp(context: Context = App.INSTANCE): Int =
    (this / context.scaledDensity + 0.5f).roundToInt()

fun Int.dpToPx(): Int = this.toFloat().dpToPx()

fun Int.spToPx(): Int = this.toFloat().spToPx()

fun Int.pxToDp(): Int = this.toFloat().pxToDp()

fun Int.pxToSp(context: Context = App.INSTANCE): Int = this.toFloat().pxToSp(context)

fun Float.pxToSpFloat(): Float = this / App.INSTANCE.resources.displayMetrics.scaledDensity + 0.5f

fun Int.pxToSpFloat(): Float = this.toFloat().pxToSpFloat()

fun Int.pxToDpFloat(): Float =
    this.toFloat().pxToDpFloat()

inline fun <reified Data> String.fromJson(): Data {
    val type = object : TypeToken<Data>() {}.type
    return GsonUtil.getGson().fromJson(this, type)
}

inline fun <reified Data> File.fromJson(): Data {
    val type = object : TypeToken<Data>() {}.type
    return GsonUtil.getGson().fromJson(reader(), type)
}

fun Any.toJson(): String = GsonUtil.getGson().toJson(this)

fun String.toMD5(): String = MD5Util.toMd5(this)

fun ByteArray.toMD5(): String = MD5Util.toMd5(this)

fun Context.getColorCompat(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun Context.getColorStateListCompat(id: Int): ColorStateList {
    return AppCompatResources.getColorStateList(this, id)
}

inline fun <reified T : Activity> Context.goToActivity() {
    startActivity(Intent(this, T::class.java))
}

inline fun <reified T : Activity> Context.goToActivity(pre: Intent.() -> Unit) {
    startActivity(Intent(this, T::class.java).apply(pre))
}

fun Context.toastShort(text: String) {
    runCatching { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
}

fun Context.toastShort(resId: Int, vararg args: Any) {
    toastShort(getString(resId, *args))
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun WindowManager.LayoutParams.enableBackgroundBlur(radius: Int = 56) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        blurBehindRadius = radius
    } else {
        dimAmount = 0.6f
    }
}
