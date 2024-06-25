package com.huanchengfly.tieba.post.components.dialogs

import android.graphics.Color
import androidx.annotation.ColorInt

class CustomThemeDialog {

    companion object {
        fun toString(alpha: Int, red: Int, green: Int, blue: Int): String {
            val hr = Integer.toHexString(red)
            val hg = Integer.toHexString(green)
            val hb = Integer.toHexString(blue)
            val ha = Integer.toHexString(alpha)
            return "#" + fixHexString(ha) + fixHexString(hr) + fixHexString(hg) + fixHexString(hb)
        }

        private fun fixHexString(hex: String): String {
            var hexString = hex
            if (hexString.isEmpty()) {
                hexString = "00"
            }
            if (hexString.length == 1) {
                hexString = "0$hexString"
            }
            if (hexString.length > 2) {
                hexString = hexString.substring(0, 2)
            }
            return hexString
        }

        fun toString(@ColorInt color: Int): String {
            return toString(
                Color.alpha(color),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
        }
    }
}