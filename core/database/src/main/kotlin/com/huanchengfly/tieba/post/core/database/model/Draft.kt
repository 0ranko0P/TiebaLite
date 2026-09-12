package com.huanchengfly.tieba.post.core.database.model

import androidx.room.Entity;

/**
 * Represent a reply draft stored locally in the database.
 */
@Entity(
    tableName = "draft",
    primaryKeys = ["threadId", "postId", "subpostId"]
)
data class Draft(
    val threadId: Long,
    val postId: Long,
    val subpostId: Long,
    val content: String?,
)
