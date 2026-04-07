package com.huanchengfly.tieba.post.repository

import androidx.annotation.VisibleForTesting
import androidx.core.util.Predicate
import com.huanchengfly.tieba.post.arch.shareInBackground
import com.huanchengfly.tieba.post.models.database.BlockForum
import com.huanchengfly.tieba.post.models.database.BlockKeyword
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.TypedKeyword
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Block Repository that manages blocking rule data.
 * */
@Singleton
class BlockRepository @Inject constructor(
    private val localDataSource: BlockDao
) {

    /**
     * Blacklisted predicates.
     * */
    val blacklist: SharedFlow<List<Predicate<String>>> = localDataSource.observeTypedKeywords(whitelisted = false)
        .map(::mapToPredicates)
        .shareInBackground(started = SharingStarted.Lazily)

    /**
     * Whitelisted predicates. Note that whitelist has the highest priority.
     * */
    val whitelist: SharedFlow<List<Predicate<String>>> = localDataSource.observeTypedKeywords(whitelisted = true)
        .map(::mapToPredicates)
        .shareInBackground(started = SharingStarted.Lazily)

    suspend fun upsertForum(forum: BlockForum) = withContext(NonCancellable) {
        localDataSource.upsertForum(forum)
    }

    suspend fun deleteForum(forumName: String) = withContext(NonCancellable) {
        localDataSource.deleteForum(forumName)
    }

    suspend fun deleteForums(forumNames: List<String>) = withContext(NonCancellable) {
        localDataSource.deleteForums(forumNames)
    }

    suspend fun addKeyword(keyword: String, isRegex: Boolean, whitelisted: Boolean) = withContext(NonCancellable) {
        localDataSource.addKeyword(keyword, isRegex, whitelisted)
    }

    suspend fun deleteKeyword(keyword: BlockKeyword) = withContext(NonCancellable) {
        localDataSource.deleteKeywordById(keyword.id)
    }

    suspend fun deleteKeywords(keywords: List<BlockKeyword>) = withContext(NonCancellable) {
        localDataSource.deleteKeywordByIdList(idList = keywords.map { it.id })
    }

    suspend fun upsertUser(user: BlockUser) = withContext(NonCancellable) {
        localDataSource.upsertUser(user)
    }

    suspend fun deleteUser(uid: Long) = withContext(NonCancellable) {
        localDataSource.deleteUserById(uid)
    }

    suspend fun deleteUsers(users: List<BlockUser>) = withContext(NonCancellable) {
        localDataSource.deleteUserByIdList(uidList = users.map { it.uid })
    }

    fun observeForums(): Flow<List<String>> = localDataSource.observeForums()

    fun observeUser(uid: Long): Flow<Boolean?> = localDataSource.observeUser(uid)

    fun observeUsers(whitelisted: Boolean): Flow<List<BlockUser>> = localDataSource.observeUsers(whitelisted)

    fun observeKeyword(whitelisted: Boolean): Flow<List<BlockKeyword>> = localDataSource.observeKeywordRules(whitelisted)

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

    suspend fun isBlocked(forumName: String, uid: Long, vararg contents: String): Boolean {
        return if (localDataSource.getForum(forumName) != null) {
            true
        } else {
            isBlocked(uid, *contents)
        }
    }

    companion object {

        private class KeywordPredicate(val keyword: String): Predicate<String> {
            override fun test(t: String?): Boolean {
                return !t.isNullOrEmpty() && t.contains(keyword, ignoreCase = true)
            }

            override fun toString(): String = keyword
        }

        private class RegexPredicate(pattern: String): Predicate<String> {
            private val regex = pattern.toRegex()

            override fun test(t: String?): Boolean {
                return !t.isNullOrEmpty() && regex.containsMatchIn(input = t)
            }

            override fun toString(): String = regex.pattern
        }

        // Convert Keywords to Predicates
        private fun mapToPredicates(rules: List<TypedKeyword>): List<Predicate<String>> {
            return rules.map {
                if (it.isRegex) RegexPredicate(pattern = it.keyword) else KeywordPredicate(keyword = it.keyword)
            }
        }

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun Array<out String>.anyMatches(predicates: List<Predicate<String>>): Boolean {
            if (isEmpty() || predicates.isEmpty()) return false

            forEach { content ->
                if (predicates.any { it.test(content) }) {
                    return true
                }
            }
            return false
        }

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun isBlocked(blacklist: List<Predicate<String>>, whitelist: List<Predicate<String>>, vararg contents: String): Boolean {
            // whitelist has the highest priority
            return if (contents.anyMatches(predicates = whitelist)) {
                false
            } else {
                contents.anyMatches(predicates = blacklist)
            }
        }
    }
}