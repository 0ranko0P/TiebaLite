package com.huanchengfly.tieba.post.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

public final class CacheUtil {
    private CacheUtil() {
    }

    public static String base64Encode(String s) {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
    }

    public static String base64Decode(String s) {
        return new String(Base64.decode(s.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT), StandardCharsets.UTF_8);
    }
}
