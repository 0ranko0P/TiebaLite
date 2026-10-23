package com.huanchengfly.tieba.post.core.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast

const val NAVIGATION_MIN_DELAY_MS = 500

var lastClickTime = -1L

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

inline fun <reified T : Activity> Context.goToActivity() {
    startActivity(Intent(this, T::class.java))
}

inline fun <reified T : Activity> Context.goToActivity(pre: Intent.() -> Unit) {
    startActivity(Intent(this, T::class.java).apply(pre))
}

inline fun <reified T : Activity> Context.goToActivityDebounced(pre: Intent.() -> Unit = {}) {
    val currentTime = SystemClock.elapsedRealtime()
    if (currentTime - lastClickTime > NAVIGATION_MIN_DELAY_MS) {
        lastClickTime = currentTime
        startActivity(Intent(this, T::class.java).apply(pre))
    }
}

fun Context.toastShort(text: CharSequence) {
    runCatching { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
}

fun Context.toastShort(resId: Int, vararg args: Any) {
    toastShort(getString(resId, *args))
}
