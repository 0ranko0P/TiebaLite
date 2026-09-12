package com.huanchengfly.tieba.post.utils;

import com.google.gson.Gson;

/**
 * @author HuanChengFly
 *
 * @since 4.0.0 dev 2
 * */
public class GsonUtil {
    private static volatile Gson gson;

    public static synchronized Gson getGson() {
        if (gson == null) {
            synchronized (GsonUtil.class) {
                if (gson == null) {
                    gson = new Gson();
                }
            }
        }
        return gson;
    }
}
