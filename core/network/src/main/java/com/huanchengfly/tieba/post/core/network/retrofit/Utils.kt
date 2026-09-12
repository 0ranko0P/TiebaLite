package com.huanchengfly.tieba.post.core.network.retrofit

private const val defaultUserAgent: String =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.0.0 Mobile Safari/537.36"

internal fun getUserAgent(userAgent: String?, appendString: String?): String {
    val append = " ${appendString?.trim()}".takeIf { !appendString.isNullOrEmpty() }.orEmpty()
    return "${userAgent ?: defaultUserAgent}$append"
}

internal fun getCookie(vararg cookies: Pair<String, () -> String?>): String {
    return cookies.map { it.first to it.second() }.filterNot { it.second.isNullOrEmpty() }
        .joinToString("; ") { "${it.first}:${it.second}" }
}

