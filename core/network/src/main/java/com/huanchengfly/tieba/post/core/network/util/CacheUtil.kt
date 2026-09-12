package com.huanchengfly.tieba.post.core.network.util

import android.util.Base64;
import java.nio.charset.StandardCharsets;

internal object CacheUtil {

    fun base64Encode(s: String): String {
        return Base64.encodeToString(s.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT)
    }

    fun base64Decode(s: String): String {
        return String(
            Base64.decode(s.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT),
            StandardCharsets.UTF_8
        )
    }
}