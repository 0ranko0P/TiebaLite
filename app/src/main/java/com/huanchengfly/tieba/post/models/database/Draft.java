package com.huanchengfly.tieba.post.models.database;

import androidx.room.Entity;

/**
 * Represent a reply draft stored locally in the database.
 */
@Entity(
        tableName = "draft",
        primaryKeys = {"threadId", "postId", "subpostId"}
)
public final class Draft {

    public long threadId;

    public long postId;

    public long subpostId;

    public String content;

    public Draft(long threadId, long postId, long subpostId, String content) {
        this.threadId = threadId;
        this.postId = postId;
        this.subpostId = subpostId;
        this.content = content;
    }
}
