package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represent a user account stored locally in the database.
 */
@Entity(
    tableName = "account"
)
@Immutable
data class Account(
    @PrimaryKey
    val uid: Long,
    val name: String,
    val nickname: String? = null,
    val bduss: String,
    val tbs: String,
    val portrait: String,
    val sToken: String,
    val cookie: String,
    val intro: String? = null,
    val sex: Int = 0,
    val fans: String = 0.toString(), // formatted short number, see Int.getShortNumString
    val posts: String = 0.toString(),
    val threads: String = 0.toString(),
    val concerned: String = 0.toString(),
    val tbAge: Float = 0f,
    val age: Int = 0,
    @ColumnInfo(name = "birthday_show")
    val birthdayShow: Boolean = true,
    @ColumnInfo(name = "birthday_time")
    val birthdayTime: Long = 0,
    val constellation: String? = null,
    val tiebaUid: String? = null,
    val zid: String? = null,
    @ColumnInfo(name = "last_update")
    val lastUpdate: Long = System.currentTimeMillis()
)
