package com.huanchengfly.tieba.post.models.database;

import org.litepal.crud.LitePalSupport;

public class TopForum extends LitePalSupport {
    private long forumId;
    private int id;

    public TopForum(long forumId) {
        this.forumId = forumId;
    }

    public int getId() {
        return id;
    }

    public long getForumId() {
        return forumId;
    }

    public TopForum setForumId(long forumId) {
        this.forumId = forumId;
        return this;
    }
}
