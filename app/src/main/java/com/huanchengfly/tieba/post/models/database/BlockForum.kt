package com.huanchengfly.tieba.post.models.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Forum blocking rule
 *
 * @param name forum name
 *
 * @since 4.0.0-beta.4.4
 */
@Entity(
    tableName = "block_forum",
)
data class BlockForum(
    @PrimaryKey
    // val forumId: Long,
    val name: String,
)