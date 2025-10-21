package com.huanchengfly.tieba.post.repository

import android.util.Log
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.LikeForum
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.LocalLikedForum
import com.huanchengfly.tieba.post.models.database.Timestamp
import com.huanchengfly.tieba.post.models.database.dao.LikedForumDao
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao
import com.huanchengfly.tieba.post.repository.source.network.ForumNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.LikedForum
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Home Repository that manages LikedForum data.
 *
 * This repository uses the database as the source of truth, observe the database to get
 * consistent and up-to-date forums.
 * */
@Singleton
class HomeRepository @Inject constructor(
    private val networkDataSource: HomeNetworkDataSource,
    private val localDataSource: LikedForumDao,
    private val timestampDao: TimestampDao,
    private val settingsRepo: SettingsRepository
) {

    private val forumNetworkDataSource = ForumNetworkDataSource

    private val currentAccount: Account?
        get() = AccountUtil.getInstance().currentAccount.value

    /**
     * Observe the current user's liked forums, ``null`` if no user logged-in
     * */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLikedForums(): Flow<List<LikedForum>?> {
        return settingsRepo.accountUid.flow
            .flatMapLatest { uid ->
                if (uid != -1L) localDataSource.observeAllSorted(uid) else throw TiebaNotLoggedInException()
            }
            .map { forums -> // Map to UI Model if not empty
                if (forums.isNotEmpty()) forums.mapUiModel() else emptyList()
            }
    }

    /**
     * Refresh the current user's liked forums
     * */
    suspend fun refresh(cached: Boolean) {
        val uid = currentAccount?.uid ?: throw TiebaNotLoggedInException()
        val start = System.currentTimeMillis()

        // force refresh or cache is expired
        if (!cached || isCacheExpired(uid)) {
            val forums = networkDataSource.getLikedForums().mapEntity(uid)
            localDataSource.upsertAll(forums)
            // save last update timestamp
            timestampDao.upsert(Timestamp(uid, TIMESTAMP_TYPE))
            if (BuildConfig.DEBUG) {
                val cost = System.currentTimeMillis() - start
                Log.i(TAG, "onRefresh: user: $uid, forums: ${forums.size}, cost: ${cost}ms.")
            }
        }
    }

    suspend fun onDislikeForum(forum: LikedForum) {
        val tbs = currentAccount?.tbs ?: throw TiebaNotLoggedInException()
        forumNetworkDataSource.dislike(forumId = forum.id, forumName = forum.name, tbs)
        onDislikeForum(forumId = forum.id)
    }

    fun onDislikeForum(forumId: Long) {
        AppBackgroundScope.launch(Dispatchers.Main) {
            val uid = currentAccount?.uid ?: throw TiebaNotLoggedInException()
            localDataSource.deleteById(uid, forumId)
        }
    }

    fun onLikeForum() {
        AppBackgroundScope.launch { refresh(cached = false) }
    }

    fun getPinnedForumIds(): Flow<List<Long>> = localDataSource.observePinnedForums()

    suspend fun addTopForum(forum: LikedForum) {
        localDataSource.pinForum(forumId = forum.id)
    }

    suspend fun removeTopForum(forum: LikedForum) {
        localDataSource.unpinForum(forumId = forum.id)
    }

    private suspend fun isCacheExpired(uid: Long, duration: Long = TimeUnit.DAYS.toMillis(7)): Boolean {
        val lastUpdate = timestampDao.get(uid, TIMESTAMP_TYPE) ?: return true
        return lastUpdate + duration < System.currentTimeMillis()
    }

    companion object {
        private const val TAG = "HomeRepository"

        /**
         * Type key used to retrieve the last forum update time
         * */
        private const val TIMESTAMP_TYPE = -2

        // Map network model to entity
        private suspend fun List<LikeForum>.mapEntity(uid: Long) = withContext(Dispatchers.Default) {
            val now = System.currentTimeMillis()
            map {
                LocalLikedForum(
                    id = it.forum_id,
                    uid = uid,
                    avatar = it.avatar,
                    name = it.forum_name,
                    level = it.level_id,
                    signInTimestamp = if (it.is_sign == 1) now else -1, // set timestamp to now if signed
                )
            }
        }

        // Map entity to ui model
        private suspend fun List<LocalLikedForum>.mapUiModel() = withContext(Dispatchers.Default) {
            val today = DateTimeUtils.todayTimeMill()
            map {
                LikedForum(
                    avatar = it.avatar,
                    id = it.id,
                    name = it.name,
                    signed = it.signInTimestamp >= today,
                    level = "Lv.${it.level}"
                )
            }
        }
    }
}