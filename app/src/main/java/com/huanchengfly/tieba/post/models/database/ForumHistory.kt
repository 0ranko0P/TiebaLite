package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Forum history
 *
 * Database entity and UI Model
 *
 * @param id forum ID
 * @param avatar forum avatar url
 * @param name forum name
 * @param timestamp timestamp
 * */
@Entity(
    tableName = "forum_history",
    indices = [Index("timestamp", unique = true)]
)
@Immutable
data class ForumHistory(
    @PrimaryKey
    override val id: Long,
    override val name: String,
    override val avatar: String,
    override val timestamp: Long = System.currentTimeMillis()
): History()