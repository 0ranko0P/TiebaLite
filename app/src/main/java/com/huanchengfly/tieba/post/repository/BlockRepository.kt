package com.huanchengfly.tieba.post.repository

import androidx.annotation.VisibleForTesting
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Block Repository that manages blocking rule data.
 * */
@Singleton
class BlockRepository @Inject constructor(
    private val localDataSource: BlockDao
) {
    private val scope = AppBackgroundScope

    /**
     * Blacklisted keywords
     * */
    val blacklist: StateFlow<List<String>> = localDataSource.observeKeywords(whitelisted = false)
        .stateIn(scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /**
     * Whitelisted keywords
     * */
    val whitelist: StateFlow<List<String>> = localDataSource.observeKeywords(whitelisted = true)
        .stateIn(scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    fun addKeyword(keyword: String, whitelisted: Boolean) {
        scope.launch {
            localDataSource.addKeyword(keyword, whitelisted)
        }
    }

    fun deleteKeyword(keyword: String) {
        scope.launch { localDataSource.deleteKeyword(keyword) }
    }

    fun upsertUser(user: BlockUser) {
        scope.launch { localDataSource.upsertUser(user) }
    }

    fun deleteUser(uid: Long) {
        scope.launch { localDataSource.deleteUserById(uid) }
    }

    fun observeUser(uid: Long): Flow<Boolean?> = localDataSource.observeUser(uid)

    fun observeUsers(whitelisted: Boolean): Flow<List<BlockUser>> = localDataSource.observeUsers(whitelisted)

    /**
     * @return is user or contents blocked
     */
    suspend fun isBlocked(uid: Long, vararg contents: String): Boolean {
        val userRule = localDataSource.getUser(uid)
        // user rule matched, skip keywords check
        return if (userRule != null) {
            !userRule.whitelisted
        } else {
            isBlocked(blacklist = blacklist.first(), whitelist = whitelist.first(), contents = contents)
        }
    }

    companion object {

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun Array<out String>.contains(keywords: List<String>): Boolean {
            if (keywords.isEmpty()) return false

            forEach { content ->
                if (content.isNotEmpty() && keywords.any { content.contains(it, ignoreCase = true) }) {
                    return true
                }
            }
            return false
        }

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun isBlocked(blacklist: List<String>, whitelist: List<String>, vararg contents: String): Boolean {
            // whitelist has highest priority
            return if (contents.contains(whitelist)) {
                false
            } else {
                contents.contains(blacklist)
            }
        }
    }
}