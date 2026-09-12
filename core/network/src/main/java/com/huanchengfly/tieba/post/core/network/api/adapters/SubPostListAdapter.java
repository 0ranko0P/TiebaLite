package com.huanchengfly.tieba.post.core.network.api.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.huanchengfly.tieba.post.core.network.model.ThreadContentBean;

import java.lang.reflect.Type;

public final class SubPostListAdapter implements JsonDeserializer<ThreadContentBean.SubPostListBean> {
    @Override
    public ThreadContentBean.SubPostListBean deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonArray()) {
            return new ThreadContentBean.SubPostListBean();
        }
        return context.deserialize(json, typeOfT);
    }
}
