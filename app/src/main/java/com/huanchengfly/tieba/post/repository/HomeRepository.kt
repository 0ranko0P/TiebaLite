package com.huanchengfly.tieba.post.repository

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.api.models.MsgBean.MessageBean
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.LikeForum
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.LocalLikedForum
import com.huanchengfly.tieba.post.models.database.Timestamp
import com.huanchengfly.tieba.post.models.database.dao.LikedForumDao
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao.Companion.TYPE_FORUM_LAST_UPDATED
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao.Companion.TYPE_NEW_MESSAGE_COUNT
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao.Companion.TYPE_NEW_MESSAGE_UPDATED
import com.huanchengfly.tieba.post.repository.source.network.ForumNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.LikedForum
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    suspend fun requireAccount(): Account {
       return AccountUtil.getInstance().currentAccount.first() ?: throw TiebaNotLoggedInException()
    }

    /**
     * Observe the current user's liked forums.
     * */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLikedForums(): Flow<List<LikedForum>> = settingsRepo.accountUid
        .flatMapLatest { uid ->
            if (uid != -1L) localDataSource.observeAllSorted(uid) else throw TiebaNotLoggedInException()
        }
        .map { forums -> // Map to UI Model if not empty
            if (forums.isNotEmpty()) forums.mapUiModel() else emptyList()
        }

    /**
     * Refresh the current user's liked forums
     * */
    suspend fun refresh(cached: Boolean) {
        val start = System.currentTimeMillis()
        val uid = requireAccount().uid

        // force refresh or cache is expired
        if (!cached || isCacheExpired(uid)) {
            val forums = networkDataSource.getLikedForums().mapEntity(uid)
            updateLikedForums(uid, forums)
            if (BuildConfig.DEBUG) {
                val cost = System.currentTimeMillis() - start
                Log.i(TAG, "onRefresh: user: $uid, forums: ${forums.size}, cost: ${cost}ms.")
            }
        }
    }

    suspend fun updateLikedForums(uid: Long, forums: List<LocalLikedForum>) {
        localDataSource.upsertAll(forums)
        // save last update timestamp
        timestampDao.upsert(Timestamp(uid, TYPE_FORUM_LAST_UPDATED))
    }

    suspend fun requestDislikeForum(forum: LikedForum) {
        val tbs = requireAccount().tbs
        forumNetworkDataSource.dislike(forumId = forum.id, forumName = forum.name, tbs)
        onDislikeForum(forumId = forum.id)
    }

    suspend fun onDislikeForum(forumId: Long) {
        AppBackgroundScope.async {
            localDataSource.deleteById(uid = requireAccount().uid, forumId = forumId)
        }
        .await()
    }

    suspend fun onLikeForum() = refresh(cached = false)

    fun getPinnedForumIds(): Flow<List<Long>> = localDataSource.observePinnedForums()

    suspend fun addTopForum(forum: LikedForum) {
        localDataSource.pinForum(forumId = forum.id)
    }

    suspend fun removeTopForum(forum: LikedForum) {
        localDataSource.unpinForum(forumId = forum.id)
    }

    private suspend fun isCacheExpired(uid: Long, duration: Long = TimeUnit.DAYS.toMillis(7)): Boolean {
        val lastUpdate = timestampDao.get(uid, TYPE_FORUM_LAST_UPDATED) ?: return true
        return lastUpdate + duration < System.currentTimeMillis()
    }

    suspend fun fetchNewMessage(): MessageBean {
        val timestamp = System.currentTimeMillis()
        val uid = settingsRepo.accountUid.snapshot()
        if (uid == -1L) {
            throw TiebaNotLoggedInException()
        }
        if (BuildConfig.DEBUG) {
            val lastUpdate = timestampDao.get(uid, TYPE_NEW_MESSAGE_UPDATED) ?: timestamp
            val duration = (timestamp - lastUpdate) / 1000 / 60
            Log.i(TAG, "onFetchNewMessage: last update $duration minutes ago.")
        }
        return AppBackgroundScope.async {
            val newMessage = networkDataSource.fetchNewMessage()
            newMessage.apply {
                timestampDao.updateNewMessageCount(uid, timestamp, newMsgCount = replyMe + atMe)
                val cost = System.currentTimeMillis() - timestamp
                Log.w(TAG, "onFetchNewMessage: Done. reply=$replyMe, at=$atMe, cost ${cost}ms.")
            }
        }.await()
    }

    /**
     * Observe the current user's new message count.
     * */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeNewMessage(): Flow<Long?> = settingsRepo.accountUid.flatMapLatest { uid ->
        if (uid != -1L) {
            timestampDao.observe(uid, TYPE_NEW_MESSAGE_COUNT)
        } else {
            throw TiebaNotLoggedInException()
        }
    }

    suspend fun clearNewMessage() {
        AppBackgroundScope.async {
            val uid = settingsRepo.accountUid.snapshot()
            timestampDao.delete(uid, TYPE_NEW_MESSAGE_COUNT)
        }.await()
    }

    companion object {
        private const val TAG = "HomeRepository"

        // Map network model to entity
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        suspend fun List<LikeForum>.mapEntity(uid: Long) = withContext(Dispatchers.Default) {
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