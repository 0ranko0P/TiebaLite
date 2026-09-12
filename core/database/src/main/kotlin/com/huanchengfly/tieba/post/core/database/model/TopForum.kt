package com.huanchengfly.tieba.post.core.database.model

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represent a pinned forum for all users
 */
@Entity(tableName = "top_forum")
data class TopForum(
    @PrimaryKey val forumId: Long,
)
