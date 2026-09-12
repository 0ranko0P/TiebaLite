package com.huanchengfly.tieba.post.core.network.api.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.huanchengfly.tieba.post.core.network.model.ThreadContentBean;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class ContentMsgAdapter implements JsonDeserializer<List<ThreadContentBean.ContentBean>> {
    @Override
    public List<ThreadContentBean.ContentBean> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<ThreadContentBean.ContentBean> list = new ArrayList<>();
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    list.add(context.deserialize(element, ThreadContentBean.ContentBean.class));
                }
            }
        }

        return list;
    }
}
