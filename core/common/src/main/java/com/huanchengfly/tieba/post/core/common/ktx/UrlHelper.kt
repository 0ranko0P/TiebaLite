package com.huanchengfly.tieba.post.core.common.ktx

import java.net.URLDecoder
import java.net.URLEncoder

fun String.urlEncode(): String = runCatching { URLEncoder.encode(this, Charsets.UTF_8.name()) }.getOrDefault(this)

fun String.urlDecode(): String = runCatching { URLDecoder.decode(this, Charsets.UTF_8.name()) }.getOrDefault(this)