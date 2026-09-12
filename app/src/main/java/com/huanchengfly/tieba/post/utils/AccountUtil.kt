package com.huanchengfly.tieba.post.utils

import androidx.compose.runtime.staticCompositionLocalOf
import com.huanchengfly.tieba.post.core.database.model.Account

val LocalAccount = staticCompositionLocalOf<Account?> { error("No Account provided") }

object AccountUtil {

    fun getBdussCookie(bduss: String): String {
        return "BDUSS=$bduss; Path=/; Max-Age=315360000; Domain=.baidu.com; Httponly"
    }

    fun parseCookie(cookie: String): Map<String, String> {
        return cookie
            .split(";")
            .map { it.trim().split("=") }
            .filter { it.size > 1 }
            .associate { it.first() to it.drop(1).joinToString("=") }
    }
}