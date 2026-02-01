@file:Suppress("NOTHING_TO_INLINE")

package com.huanchengfly.tieba.post.api

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.App.ScreenInfo


private val defaultUserAgent: String =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.0.0 Mobile Safari/537.36"

fun getUserAgent(appendString: String? = null): String {
    val append = " ${appendString?.trim()}".takeIf { !appendString.isNullOrEmpty() }.orEmpty()
    return "${App.Config.userAgent ?: defaultUserAgent}$append"
}

fun getCookie(vararg cookies: Pair<String, () -> String?>): String {
    return cookies.map { it.first to it.second() }.filterNot { it.second.isNullOrEmpty() }
        .joinToString("; ") { "${it.first}:${it.second}" }
}

fun getScreenHeight(): Int = ScreenInfo.EXACT_SCREEN_HEIGHT

fun getScreenWidth(): Int = ScreenInfo.EXACT_SCREEN_WIDTH

inline fun Boolean.booleanToString(): String = if (this) "1" else "0"

inline fun Boolean.booleanToInt(): Int = if (this) 1 else 0
