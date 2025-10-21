package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.SearchHistory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the search table.
 */
@Dao
interface SearchDao {

    @Upsert
    suspend fun upsert(search: SearchHistory)

    @Query("DELETE FROM search")
    suspend fun deleteAll()

    @Query("DELETE FROM search WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("DELETE FROM search WHERE keyword = :keyword")
    suspend fun delete(keyword: String): Int

    /**
     * Observes list of search keywords.
     */
    @Query("SELECT keyword FROM search ORDER BY timestamp DESC")
    fun observeAllKeywords(): Flow<List<String>>
}