package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.Account
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the account table.
 */
@Dao
interface AccountDao {

    /**
     * Insert or update an account in the database. If an account already exists, replace it.
     *
     * @param account the account to be inserted or updated.
     */
    @Upsert
    suspend fun upsert(account: Account)

    @Query("DELETE FROM account")
    suspend fun deleteAll()

    /**
     * Delete an account by id.
     *
     * @return the number of accounts deleted. This should always be 1.
     */
    @Query("DELETE FROM account WHERE uid = :uid")
    suspend fun deleteById(uid: Long): Int

    /**
     * Observes list of accounts.
     */
    @Query("SELECT * FROM account")
    fun observeAll(): Flow<List<Account>>

    /**
     * Observes a single account.
     *
     * @param uid account id
     */
    @Query("SELECT * FROM account WHERE uid = :uid")
    fun observeById(uid: Long): Flow<Account?>

    /**
     * Select all accounts from the account table.
     */
    @Query("SELECT * FROM account")
    suspend fun getAll(): List<Account>

    /**
     * Select an account by id.
     *
     * @param uid account id
     */
    @Query("SELECT * FROM account WHERE uid = :uid")
    suspend fun getById(uid: Long): Account?
}