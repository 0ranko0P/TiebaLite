package com.huanchengfly.tieba.post.models.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represent a user profile stored locally in the database.
 * */
@Entity(
    tableName = "user",
    indices = [Index("last_visit", unique = true)]
)
data class UserProfile(
    @PrimaryKey
    val uid: Long,
    val portrait: String  = "",
    val name: String = "",
    val nickname: String? = null,
    val tiebaUid: String = "",
    val intro: String? = null,
    val sex: String = "?",
    val tbAge: String = 0.toString(),
    val address: String? = null,
    val following: Boolean = false,
    val thread: Int = 0,
    val post: Int = 0,
    val forum: Int = 0,
    val follow: Int = 0,
    val fans: Int = 0,
    val agree: Int = 0,
    val bazuDesc: String? = null,
    val newGod: String? = null,
    val privateForum: Boolean = true,
    val isOfficial: Boolean = false,
    @ColumnInfo(name = "last_update")
    val lastUpdate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_visit")
    val lastVisit: Long = System.currentTimeMillis(),
)
