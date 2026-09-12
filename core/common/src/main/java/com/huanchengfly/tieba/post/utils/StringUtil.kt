package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.core.common.ktx.unsafeLazy

object StringUtil {

    private val CONTROL_CHAR_REGEX_PATTERN by unsafeLazy { "\\p{C}".toRegex() }

    fun getUserNameString(showBoth: Boolean, username: String, nickname: String?): String {
        val canShowBoth = !nickname.isNullOrBlank() && username != nickname && username.isNotBlank()
        return if (canShowBoth && showBoth) {
            "$nickname ($username)"
        } else {
            nickname ?: username
        }
            .normalized()
    }

    @JvmStatic
    fun getAvatarUrl(portrait: String?): String {
        if (portrait.isNullOrEmpty()) {
            return ""
        }
        return if (portrait.startsWith("http://") || portrait.startsWith("https://")) {
            portrait
        } else "http://tb.himg.baidu.com/sys/portrait/item/$portrait"
    }

    @JvmStatic
    fun getBigAvatarUrl(portrait: String?): String {
        if (portrait.isNullOrEmpty()) {
            return ""
        }
        return if (portrait.startsWith("http://") || portrait.startsWith("https://")) {
            portrait
        } else "http://tb.himg.baidu.com/sys/portraith/item/$portrait"
    }

    // Convert formatted number string
    fun tiebaNumToLong(str: String): Long {
        if (str == "0") return 0L

        try {
            return str.toLongOrNull() ?: str.run {
                var num = 0L
                forEachIndexed { i, c ->
                    if (c.isDigit()) {
                        if (i != 0) num *= 10
                        num += c.digitToInt()
                    } else if (c.equals('W', ignoreCase = true)) {
                        num *= 10000
                    } else if (c.equals('K', ignoreCase = true)) {
                        num *= 1000
                    }
                }
                return@run num
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0L
    }

    fun Int.getShortNumString(): String = if (this <= 999) toString() else toLong().getShortNumString()

    fun Long.getShortNumString(): String {
        val long = this
        return if (long > 9999) {
            val longW = long * 10 / 10000L / 10F
            if (longW > 999) {
                val longKW = longW.toLong() * 10 / 1000L / 10F
                "${longKW}KW"
            } else {
                "${longW}W"
            }
        } else {
            "$this"
        }
    }

    // remove control chars
    fun String.normalized(): String = if (isNullOrEmpty()) {
        this
    } else {
        replace(CONTROL_CHAR_REGEX_PATTERN, "")
    }

    fun String.toMD5(): String = MD5Util.toMd5(this)
}