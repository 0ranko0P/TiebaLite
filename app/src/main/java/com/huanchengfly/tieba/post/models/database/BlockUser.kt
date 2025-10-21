package com.huanchengfly.tieba.post.models.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User blocking rule
 *
 * @param uid user id
 * @param name user name
 * @param whitelisted is this user whitelisted or blacklisted
 */
@Entity(
    tableName = "block_user",
    indices = [Index(value = ["whitelisted"])]
)
class BlockUser(
    @PrimaryKey
    val uid: Long,
    val name: String?,
    val whitelisted: Boolean
)