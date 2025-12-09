package com.huanchengfly.tieba.post.models.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the user table.
 */
@Dao
interface UserProfileDao {

    @Upsert
    suspend fun upsert(profile: UserProfile)

    @Query("DELETE FROM user")
    suspend fun deleteAll()

    @Query("DELETE FROM user WHERE uid = :uid")
    suspend fun deleteById(uid: Long): Int

    @Query("UPDATE user SET `following` = :following, fans = :fans WHERE uid = :uid")
    suspend fun updateFollowState(uid: Long, following: Boolean, fans: Int)

    @Query("UPDATE user SET last_visit = :timestamp WHERE uid = :uid")
    suspend fun updateLastVisit(uid: Long, timestamp: Long)

    /**
     * Observes list of user profiles.
     */
    @Query("SELECT * FROM user")
    fun observeAll(): Flow<List<UserProfile>>

    /**
     * Observes a single user profile.
     *
     * @param uid user ID
     */
    @Query("SELECT * FROM user WHERE uid = :uid")
    fun observeById(uid: Long): Flow<UserProfile?>

    /**
     * Get user profile paging source.
     * */
    @Query("SELECT * FROM user ORDER BY last_visit DESC")
    fun pagingSourceSorted(): PagingSource<Int, UserProfile>

    /**
     * Select all user profile from the user table.
     */
    @Query("SELECT * FROM user ORDER BY last_visit DESC")
    suspend fun getAllSorted(): List<UserProfile>

    /**
     * Select last update time by given [uid] from the user table.
     */
    @Query("SELECT last_update FROM user WHERE uid = :uid")
    suspend fun getLastUpdate(uid: Long): Long?
}