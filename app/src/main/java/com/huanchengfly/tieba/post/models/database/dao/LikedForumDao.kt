package com.huanchengfly.tieba.post.models.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.LocalLikedForum
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the liked forum table.
 */
@Dao
interface LikedForumDao {

    @Upsert
    suspend fun upsert(forum: LocalLikedForum)

    /**
     * Insert or update user liked forums in the database.
     *
     * @param uid user id
     * @param forums forums to be updated.
     */
    @Transaction
    suspend fun upsertAll(uid: Long, forums: List<LocalLikedForum>) {
        deleteAllByUid(uid)
        forums.forEach { forum -> upsert(forum) }
    }

    /**
     * Update the last sign-in timestamp of a liked forum.
     *
     * @param uid user id
     * @param forumId id of the forum
     * @param timestamp last sign-in time
     */
    @Query("UPDATE liked_forum SET sign = :timestamp WHERE id = :forumId AND uid = :uid")
    suspend fun updateSignIn(uid: Long, forumId: Long, timestamp: Long)

    @Query("DELETE FROM liked_forum WHERE uid = :uid AND id = :forumId")
    suspend fun deleteById(uid: Long, forumId: Long): Int

    @Query("DELETE FROM liked_forum WHERE uid = :uid")
    suspend fun deleteAllByUid(uid: Long): Int

    /**
     * Observes list of liked forums.
     */
    @Query("SELECT * FROM liked_forum WHERE uid = :uid ORDER BY level DESC")
    fun observeAllSorted(uid: Long): Flow<List<LocalLikedForum>>

    /**
     * Get user forums paging source.
     *
     * For top pinned forums, use [pagingSourcePinned].
     *
     * @param uid user id
     */
    @Query("SELECT * FROM liked_forum liked " +
            "WHERE uid = :uid AND NOT EXISTS (SELECT forumId from top_forum WHERE top_forum.forumId = liked.id)" +
            "ORDER BY level DESC"
    )
    fun pagingSource(uid: Long): PagingSource<Int, LocalLikedForum>

    /**
     * Get pinned liked forums paging source.
     *
     * @param uid user id
     */
    @Query("SELECT * FROM liked_forum " +
            "WHERE uid = :uid AND EXISTS (SELECT forumId from top_forum WHERE top_forum.forumId = liked_forum.id)" +
            "ORDER BY level DESC"
    )
    fun pagingSourcePinned(uid: Long): PagingSource<Int, LocalLikedForum>

    /**
     * Pin this forum to top (for all users).
     *
     * @param forumId forum id
     */
    @Query("INSERT INTO top_forum (forumId) VALUES (:forumId)")
    suspend fun pinForum(forumId: Long)

    @Query("DELETE FROM top_forum WHERE forumId = :forumId")
    suspend fun unpinForum(forumId: Long)

    /**
     * Observes list of pinned forum ids.
     */
    @Query("SELECT forumId FROM top_forum")
    fun observePinnedForums(): Flow<List<Long>>
}