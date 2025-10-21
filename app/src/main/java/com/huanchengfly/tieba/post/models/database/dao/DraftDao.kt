package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.huanchengfly.tieba.post.models.database.Draft

/**
 * Data Access Object for the draft table.
 */
@Dao
interface DraftDao {

    /**
     * Insert a draft in the database.
     */
    @Insert
    suspend fun insert(draft: Draft)

    /**
     * Insert or update a draft in the database. If a draft already exists, replace it.
     *
     * @param draft the draft to be inserted or updated.
     */
    @Transaction
    suspend fun upsert(draft: Draft) {
        deleteByIds(threadId = draft.threadId, postId = draft.postId, subpostId = draft.subpostId)
        insert(draft)
    }

    @Query("DELETE FROM draft")
    suspend fun deleteAll()

    /**
     * Delete a draft by unique ids.
     *
     * @return the number of draft deleted. This should always be 1.
     */
    @Query("DELETE FROM draft WHERE threadId = :threadId AND postId = :postId AND subpostId = :subpostId")
    suspend fun deleteByIds(threadId: Long, postId: Long, subpostId: Long): Int

    @Query("SELECT content FROM draft WHERE threadId = :threadId AND postId = :postId AND subpostId = :subpostId")
    suspend fun getByIds(threadId: Long, postId: Long, subpostId: Long): List<String>
}