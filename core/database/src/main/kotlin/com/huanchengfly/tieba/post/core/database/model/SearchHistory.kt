package com.huanchengfly.tieba.post.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Search history
 *
 * @param id database primary key
 * @param keyword search keyword
 * @param timestamp search timestamp
 */
@Entity(
    tableName = "search",
    indices = [Index(value = ["timestamp"], unique = true)]
)
data class SearchHistory(
    @PrimaryKey
    override val id: Int,
    override val keyword: String,
    override val timestamp: Long = System.currentTimeMillis(),
): Search() {

    constructor(keyword: String) : this(id = keyword.hashCode(), keyword = keyword)
}