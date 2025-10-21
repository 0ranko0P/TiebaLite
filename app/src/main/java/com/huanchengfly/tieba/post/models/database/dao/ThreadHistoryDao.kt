package com.huanchengfly.tieba.post.models.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.ThreadHistory

/**
 * Data Access Object for the thread history table.
 */
@Dao
interface ThreadHistoryDao {

    /**
     * Insert or update a history record in the database. If a history already exists, replace it.
     *
     * @param history the history to be inserted or updated.
     */
    @Upsert
    suspend fun upsert(history: ThreadHistory)

    @Query("DELETE FROM thread_history")
    suspend fun deleteAll()

    /**
     * Delete a history record by id.
     *
     * @return the number of history record deleted. This should always be 1.
     */
    @Query("DELETE FROM thread_history WHERE id = :threadId")
    suspend fun deleteById(threadId: Long): Int

    /**
     * Get thread history paging source.
     * */
    @Query("SELECT * FROM thread_history ORDER BY timestamp DESC")
    fun pagingSourceSorted(): PagingSource<Int, ThreadHistory>

    /**
     * Select all history from the thread history table.
     */
    @Query("SELECT * FROM thread_history ORDER BY timestamp DESC")
    suspend fun getAllSorted(): List<ThreadHistory>
}