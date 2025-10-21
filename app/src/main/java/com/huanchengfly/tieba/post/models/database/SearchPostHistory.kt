package com.huanchengfly.tieba.post.models.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Objects

/**
 * Search post history
 *
 * @param id database primary key
 * @param forumId forum id
 * @param keyword search keyword
 * @param timestamp search timestamp
 */
@Entity(
    tableName = "search_post",
    indices = [
        Index(value = ["forumId"]),
        Index(value = ["timestamp"], unique = true)
    ]
)
class SearchPostHistory(
    @PrimaryKey
    override val id: Int,
    val forumId: Long,
    override val keyword: String,
    override val timestamp: Long = System.currentTimeMillis(),
) : Search() {

    constructor(forumId: Long, keyword: String) : this(Objects.hash(forumId, keyword), forumId, keyword)
}
