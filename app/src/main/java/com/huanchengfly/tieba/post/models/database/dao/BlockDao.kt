package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.BlockForum
import com.huanchengfly.tieba.post.models.database.BlockKeyword
import com.huanchengfly.tieba.post.models.database.BlockUser
import kotlinx.coroutines.flow.Flow

data class TypedKeyword(val keyword: String, val isRegex: Boolean)

data class KeywordCSV(val keyword: String, val isRegex: Boolean, val whitelisted: Boolean)

typealias UserCSV = BlockUser

/**
 * Data Access Object for block_forum, block_keyword and block_user table.
 */
@Dao
interface BlockDao {

    /**
     * Insert a keyword blocking rule in the database.
     *
     * @param keyword the keyword to be inserted
     * @param isRegex is [keyword] a regex pattern
     * @param whitelisted is [keyword] whitelisted or blacklisted
     */
    @Query("INSERT INTO block_keyword (keyword, isRegex, whitelisted) VALUES (:keyword, :isRegex, :whitelisted)")
    suspend fun addKeyword(keyword: String, isRegex: Boolean, whitelisted: Boolean)

    /**
     * Insert one or more keyword rule into the database. If a rule already exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeywords(vararg keywords: BlockKeyword)

    @Query("DELETE FROM block_keyword")
    suspend fun deleteAllKeyword()

    @Query("DELETE FROM block_keyword WHERE id = :id")
    suspend fun deleteKeywordById(id: Long): Int

    @Query("DELETE FROM block_keyword WHERE id in (:idList)")
    suspend fun deleteKeywordByIdList(idList: List<Long>): Int

    /**
     * Observes list of keywords
     *
     * @param whitelisted whitelist or blacklist
     * */
    @Query("SELECT keyword, isRegex FROM block_keyword WHERE whitelisted = :whitelisted")
    fun observeTypedKeywords(whitelisted: Boolean): Flow<List<TypedKeyword>>

    /**
     * Observes list of keyword blocking rules
     *
     * @param whitelisted whitelist or blacklist
     * */
    @Query("SELECT * FROM block_keyword WHERE whitelisted = :whitelisted ORDER BY isRegex DESC, keyword")
    fun observeKeywordRules(whitelisted: Boolean): Flow<List<BlockKeyword>>

    /**
     * Select all keyword blocking rules.
     * */
    @Query("SELECT keyword, isRegex, whitelisted FROM block_keyword ORDER BY isRegex, whitelisted")
    suspend fun getAllKeywords(): List<KeywordCSV>

    /**
     * Insert or update a user blocking rule into the database. If a rule already exists, update it.
     *
     * @param blockUser the user blocking rule to be inserted or updated.
     */
    @Upsert
    suspend fun upsertUser(blockUser: BlockUser)

    /**
     * Insert one or more user blocking rule into the database. If a rule already exists, replace it.
     *
     * @param blockUsers the blocking rules to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(vararg blockUsers: BlockUser)

    @Query("DELETE FROM block_user")
    suspend fun deleteAllUser()

    @Query("DELETE FROM block_user WHERE uid = :uid")
    suspend fun deleteUserById(uid: Long): Int

    @Query("DELETE FROM block_user WHERE uid in (:uidList)")
    suspend fun deleteUserByIdList(uidList: List<Long>): Int

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
     * Select all user blocking rule.
     * */
    @Query("SELECT * FROM block_user ORDER BY whitelisted")
    suspend fun getAllUsers(): List<UserCSV>

    /**
     * Select a user blocking rule by uid.
     *
     * @param uid user id
     */
    @Query("SELECT * FROM block_user WHERE uid = :uid")
    suspend fun getUser(uid: Long): BlockUser?

    /**
     * Insert or update a forum blocking rule into the database. If a rule already exists, update it.
     *
     * @param forum the forum blocking rule to be inserted or updated.
     */
    @Upsert
    suspend fun upsertForum(forum: BlockForum)

    /**
     * Insert one or more forum blocking rule into the database. If a rule already exists, replace it.
     *
     * @param forums the forum blocking rules to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForums(vararg forums: BlockForum)

    @Query("DELETE FROM block_forum")
    suspend fun deleteAllForum()

    /**
     * Delete a forum blocking rule by name.
     *
     * @return the number of rule deleted. This should always be 1.
     */
    @Query("DELETE FROM block_forum WHERE name = :forumName")
    suspend fun deleteForum(forumName: String): Int

    /**
     * Delete list of forum blocking rule by name.
     *
     * @return the number of rule deleted.
     */
    @Query("DELETE FROM block_forum WHERE name in (:forumNames)")
    suspend fun deleteForums(forumNames: List<String>): Int

    /**
     * Select a forum blocking rule by name.
     */
    @Query("SELECT name FROM block_forum WHERE name = :forumName")
    suspend fun getForum(forumName: String): String?

    @Query("SELECT name FROM block_forum")
    suspend fun getForums(): List<String>

    /**
     * Observes list of forum blocking rules.
     */
    @Query("SELECT name FROM block_forum")
    fun observeForums(): Flow<List<String>>
}