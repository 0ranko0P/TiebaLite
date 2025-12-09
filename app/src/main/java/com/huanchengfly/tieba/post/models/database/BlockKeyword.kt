package com.huanchengfly.tieba.post.models.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Keyword blocking rule
 *
 * @param keyword keyword
 * @param isRegex whether or not [keyword] is regex pattern
 * @param whitelisted whitelisted or blacklisted
 */
@Entity(
    tableName = "block_keyword",
    indices = [Index(value = ["whitelisted"])]
)
data class BlockKeyword(
    @PrimaryKey
    val id: Long,
    val keyword: String,
    val isRegex: Boolean,
    val whitelisted: Boolean
)
