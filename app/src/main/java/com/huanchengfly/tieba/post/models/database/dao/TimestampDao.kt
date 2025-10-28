package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.Timestamp
import kotlinx.coroutines.flow.Flow

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

    /**
     * Observe a user's timestamp.
     *
     * @param uid user id
     * @param type type of the timestamp
     */
    @Query("SELECT time FROM timestamp WHERE uid = :uid AND type = :type")
    fun observe(uid: Long, type: Int): Flow<Long?>

    @Transaction
    suspend fun updateNewMessageCount(uid: Long, timestamp: Long, newMsgCount: Int) {
        upsert(Timestamp(uid, TYPE_NEW_MESSAGE_COUNT, newMsgCount.toLong()))
        upsert(Timestamp(uid, TYPE_NEW_MESSAGE_UPDATED))
        if (newMsgCount > 0) {
            upsert(Timestamp(uid, TYPE_NEW_MESSAGE_RECEIVED))
        }
    }

    companion object {

        /**
         * Timestamp Type: Last updated time of liked forum.
         * */
        const val TYPE_FORUM_LAST_UPDATED = -2

        /**
         * Timestamp Type: Last updated time of new message.
         * */
        const val TYPE_NEW_MESSAGE_UPDATED = 8

        /**
         * Timestamp Type: Last received time of new message.
         * */
        const val TYPE_NEW_MESSAGE_RECEIVED = 16

        /**
         * Data Type: New message count. Separated from the account table.
         * */
        const val TYPE_NEW_MESSAGE_COUNT = 32
    }
}