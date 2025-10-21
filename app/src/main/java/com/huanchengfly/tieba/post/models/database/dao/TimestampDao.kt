package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.Timestamp

/**
 * Data Access Object for the timestamp table.
 */
@Dao
interface TimestampDao {

    @Upsert
    suspend fun upsert(timestamp: Timestamp)

    @Query("DELETE FROM timestamp WHERE uid = :uid AND type = :type")
    suspend fun delete(uid: Long, type: Int): Int

    @Query("UPDATE timestamp SET time = :time WHERE uid = :uid AND type = :type")
    suspend fun update(uid: Long, type: Int, time: Long)

    /**
     * Select a user's timestamp.
     *
     * @param uid user id
     * @param type type of the timestamp
     */
    @Query("SELECT time FROM timestamp WHERE uid = :uid AND type = :type")
    suspend fun get(uid: Long, type: Int): Long?
}