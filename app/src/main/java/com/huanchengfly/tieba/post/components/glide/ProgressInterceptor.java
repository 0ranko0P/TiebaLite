package com.huanchengfly.tieba.post.components.glide;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 描述:
 * <p>
 * 拦截器
 * Created by allens on 2018/1/8.
 */

public class ProgressInterceptor implements Interceptor {

    private static final Map<String, ProgressListener> LISTENER_MAP = new ConcurrentHashMap<>();

    //入注册下载监听
    public static void addListener(String url, ProgressListener listener) {
        LISTENER_MAP.put(url, listener);
    }

    //取消注册下载监听
    public static void removeListener(String url) {
        LISTENER_MAP.remove(url);
    }


    @Override
    public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        String url = request.url().toString();
        ProgressListener listener = ProgressInterceptor.LISTENER_MAP.get(url);
        ResponseBody body = response.body();
        return response.newBuilder().body(new ProgressResponseBody(body, listener)).build();
    }
}