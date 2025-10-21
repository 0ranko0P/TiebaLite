package com.huanchengfly.tieba.post.models.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represent a pinned forum for all users
 */
@Entity(tableName = "top_forum")
public final class TopForum {

    @PrimaryKey private long forumId;

    public TopForum(long forumId) {
        this.forumId = forumId;
    }

    public long getForumId() {
        return forumId;
    }

    public void setForumId(long forumId) {
        this.forumId = forumId;
    }
}
