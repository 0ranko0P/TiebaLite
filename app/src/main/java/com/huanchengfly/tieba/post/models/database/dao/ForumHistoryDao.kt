package com.huanchengfly.tieba.post.models.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.ForumHistory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the forum history table.
 */
@Dao
interface ForumHistoryDao {

    @Upsert
    fun upsert(history: ForumHistory)

    @Query("DELETE FROM forum_history")
    suspend fun deleteAll()

    /**
     * Delete a history record by id.
     *
     * @return the number of history record deleted. This should always be 1.
     */
    @Query("DELETE FROM forum_history WHERE id = :forumId")
    suspend fun deleteById(forumId: Long): Int

    @Query("SELECT * FROM forum_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeTop(limit: Int = 10): Flow<List<ForumHistory>>

    /**
     * Get forum history paging source.
     * */
    @Query("SELECT * FROM forum_history ORDER BY timestamp DESC")
    fun pagingSource(): PagingSource<Int, ForumHistory>
}