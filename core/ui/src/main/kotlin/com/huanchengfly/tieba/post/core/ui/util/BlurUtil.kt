package com.huanchengfly.tieba.post.core.ui.util

import android.os.Build
import android.view.WindowManager

fun WindowManager.LayoutParams.enableBackgroundBlur(radius: Int = 56) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        blurBehindRadius = radius
    } else {
        dimAmount = 0.6f
    }
}