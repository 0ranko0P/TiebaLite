package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Thread history
 *
 * Database entity and UI Model
 *
 * @param id thread ID
 * @param avatar avatar of thread author
 * @param name nickname of thread author
 * @param title thread title
 * @param isSeeLz see lz mode
 * @param pid last visible post
 * @param timestamp timestamp
 * */
@Entity(
    tableName = "thread_history",
    indices = [Index("timestamp", unique = true)]
)
@Immutable
class ThreadHistory(
    @PrimaryKey
    override val id: Long,
    override val avatar: String,
    override val name: String,
    val title: String,
    @ColumnInfo(name = "is_see_lz")
    val isSeeLz: Boolean,
    val pid: Long,
    override val timestamp: Long
): History()