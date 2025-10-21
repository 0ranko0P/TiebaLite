package com.huanchengfly.tieba.post.models.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represent a timestamp record, e.g. last sign-in time, forum cache expire time.
 *
 * @param uid user id
 * @param type unique type of this timestamp
 * @param time epoch timestamp
 */
@Entity(
    tableName = "timestamp",
    indices = [
        Index(value = ["uid"])
    ],
    primaryKeys = ["uid", "type"],
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["uid"],
            childColumns = ["uid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class Timestamp(
    val uid: Long,
    val type: Int,
    val time: Long = System.currentTimeMillis()
)