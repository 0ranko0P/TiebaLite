package com.huanchengfly.tieba.post.core.network.model.web;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class HotTopicThreadBean extends WebBaseBean<HotTopicThreadBean.HotTopicThreadDataBean> {
    public static final class HotTopicThreadDataBean {
        @SerializedName("thread_list")
        private List<HotTopicMainBean.ThreadBean> threadList;

        public List<HotTopicMainBean.ThreadBean> getThreadList() {
            return threadList;
        }
    }
}
