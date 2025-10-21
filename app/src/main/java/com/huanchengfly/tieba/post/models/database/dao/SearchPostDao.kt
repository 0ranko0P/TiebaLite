package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the search post table.
 */
@Dao
interface SearchPostDao {

    @Upsert
    suspend fun upsert(search: SearchPostHistory)

    @Query("DELETE FROM search_post")
    suspend fun deleteAll(): Int

    /**
     * Delete all search history of this forum in the database.
     *
     * @param forumId forum id
     * @return the number of history deleted.
     */
    @Query("DELETE FROM search_post WHERE forumId = :forumId")
    suspend fun deleteAll(forumId: Long): Int

    @Query("DELETE FROM search_post WHERE forumId = :forumId AND keyword = :keyword")
    suspend fun delete(forumId: Long, keyword: String): Int

    @Query("DELETE FROM search_post WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    /**
     * Observes list of search keywords of this forum.
     *
     * @param forumId forum id
     */
    @Query("SELECT keyword FROM search_post WHERE forumId = :forumId ORDER BY timestamp DESC")
    fun observeAllKeywords(forumId: Long): Flow<List<String>>
}
