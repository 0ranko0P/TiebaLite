package com.huanchengfly.tieba.post

import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.ColorRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.huanchengfly.tieba.post.utils.MD5Util
import kotlin.math.roundToInt

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

fun String.toMD5(): String = MD5Util.toMd5(this)

fun ByteArray.toMD5(): String = MD5Util.toMd5(this)

fun Context.getColorCompat(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun Context.getColorStateListCompat(id: Int): ColorStateList {
    return AppCompatResources.getColorStateList(this, id)
}
