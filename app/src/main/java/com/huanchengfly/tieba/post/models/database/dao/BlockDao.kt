package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.BlockKeyword
import com.huanchengfly.tieba.post.models.database.BlockUser
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the block_keyword and block_user table.
 */
@Dao
interface BlockDao {

    /**
     * Insert a keyword blocking rule in the database.
     *
     * @param keyword the keyword to be inserted
     * @param whitelisted is this keyword whitelisted or blacklisted
     */
    @Query("INSERT INTO block_keyword (keyword, whitelisted) VALUES (:keyword, :whitelisted)")
    suspend fun addKeyword(keyword: String, whitelisted: Boolean)

    @Query("DELETE FROM block_keyword WHERE keyword = :keyword")
    suspend fun deleteKeyword(keyword: String): Int

    @Query("DELETE FROM block_keyword WHERE id = :id")
    suspend fun deleteKeywordById(id: Long): Int

    /**
     * Observes list of keywords
     *
     * @param whitelisted whitelist or blacklist
     * */
    @Query("SELECT keyword FROM block_keyword WHERE whitelisted = :whitelisted")
    fun observeKeywords(whitelisted: Boolean): Flow<List<String>>

    /**
     * Observes list of keyword blocking rules
     *
     * @param whitelisted whitelist or blacklist
     * */
    @Query("SELECT * FROM block_keyword WHERE whitelisted = :whitelisted")
    fun observeKeywordRules(whitelisted: Boolean): Flow<List<BlockKeyword>>

    /**
     * Insert or update a user blocking rule in the database. If a rule already exists, replace it.
     *
     * @param blockUser the user blocking rule to be inserted or updated.
     */
    @Upsert
    suspend fun upsertUser(blockUser: BlockUser)

    @Query("DELETE FROM block_user WHERE uid = :uid")
    suspend fun deleteUserById(uid: Long): Int

    /**
     * Observes a user blocking state by uid.
     *
     * @param uid user id
     */
    @Query("SELECT whitelisted FROM block_user WHERE uid = :uid")
    fun observeUser(uid: Long): Flow<Boolean?>

    /**
     * Observes list of user blocking rules.
     *
     * @param whitelisted whitelist or blacklist
     */
    @Query("SELECT * FROM block_user WHERE whitelisted = :whitelisted")
    fun observeUsers(whitelisted: Boolean): Flow<List<BlockUser>>

    /**
     * Select a user blocking rule by uid.
     *
     * @param uid user id
     */
    @Query("SELECT * FROM block_user WHERE uid = :uid")
    suspend fun getUser(uid: Long): BlockUser?
}