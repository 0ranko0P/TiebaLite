package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Keyword blocking rule
 *
 * @param keyword keyword
 * @param whitelisted whitelisted or blacklisted
 */
@Entity(
    tableName = "block_keyword",
    indices = [Index(value = ["whitelisted"])]
)
@Immutable
data class BlockKeyword(
    @PrimaryKey
    val id: Long,
    val keyword: String,
    val whitelisted: Boolean
)
