package com.huanchengfly.tieba.post.models.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represent a liked forum stored locally in the database.
 *
 * @param id forum ID
 * @param uid user ID
 * @param avatar forum avatar url
 * @param name forum name
 * @param level level in forum
 * @param signInTimestamp last sign-in timestamp
 */
@Entity(
    tableName = "liked_forum",
    primaryKeys = ["id", "uid"],
    indices = [
        Index(value = ["uid"]),
        Index(value = ["level"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["uid"],
            childColumns = ["uid"],
            onDelete = ForeignKey.CASCADE,
            deferred = true,
        )
    ]
)
data class LocalLikedForum(
    val id: Long,
    val uid: Long,
    val avatar: String = "",
    val name: String = "",
    val level: Int,
    @ColumnInfo(name = "sign")
    val signInTimestamp: Long,
)
