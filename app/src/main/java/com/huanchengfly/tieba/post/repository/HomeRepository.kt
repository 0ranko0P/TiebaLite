package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.LikeForum
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.repository.source.local.HomeLocalDataSource
import com.huanchengfly.tieba.post.repository.source.local.TopForumDao
import com.huanchengfly.tieba.post.repository.source.network.ForumNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkDataSource
import com.huanchengfly.tieba.post.ui.models.LikedForum
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Home Repository that manages LikedForum data.
 * */
@Singleton
class HomeRepository @Inject constructor(
    private val networkDataSource: HomeNetworkDataSource,
    private val localDataSource: HomeLocalDataSource
) {

    private val forumNetworkDataSource = ForumNetworkDataSource

    private val localTopForumDataSource = TopForumDao

    private val currentAccount: Flow<Account?> = AccountUtil.getInstance().currentAccount

    private val refresh by lazy { Channel<Unit>(capacity = Channel.CONFLATED) }

    /**
     * In-Memory cache of [HomeNetworkDataSource.getLikedForums].
     *
     * Updated by multiple ViewModels.
     *
     * @see dislikeForum
     * @see onLikeForum
     * @see refreshForumList
     * */
    private val _likedForums: MutableStateFlow<List<LikedForum>?> = MutableStateFlow(null)
    val likedForums: StateFlow<List<LikedForum>?> = _likedForums.asStateFlow()

    suspend fun refreshForumList(cached: Boolean = true) {
        // Retrieve cached forums
        val uid = currentAccount.firstOrNull()?.uid?.toLong() ?: throw TiebaNotLoggedInException()
        if (cached) {
            val cachedForumList = localDataSource.get(uid)?.mapUiModel()
            if (cachedForumList != null) {
                _likedForums.value = cachedForumList
                return
            }
            // else: cache expired or not exists
        }

        val forums = networkDataSource.getLikedForums()
        // Write to cache, even it's empty
        localDataSource.saveOrUpdate(uid, forums)
        val likedForumList: List<LikedForum> = forums.mapUiModel()
        _likedForums.update { likedForumList }
    }

    suspend fun dislikeForum(forum: LikedForum) {
        val account = currentAccount.firstOrNull() ?: throw TiebaNotLoggedInException()
        forumNetworkDataSource.dislike(forum.id, forum.name, account.tbs)
        onDislikeForum(forumId = forum.id)
    }

    fun onDislikeForum(forumId: Long) {
        AppBackgroundScope.launch(Dispatchers.Main) {
            // Update in-memory cache now
            var cache = _likedForums.firstOrNull()
            if (cache != null) {
                cache = withContext(Dispatchers.Default) { cache.filter { it.id != forumId } }
                _likedForums.update { cache }
                val uid = currentAccount.firstOrNull()?.uid ?: throw TiebaNotLoggedInException()
                localDataSource.delete(uid.toLong())
            }
            // Remove TopForum
            // localTopForumDataSource.delete(TopForum(forum.id))
        }
    }

    fun onLikeForum() {
        AppBackgroundScope.launch(Dispatchers.Main) { refreshForumList(cached = false) }
    }

    // TODO: Migrate to Room DataBase for native Flow support
    fun getTopForumIds(): Flow<Set<Long>> = flow {
        val iterator = refresh.iterator()
        do {
            val topForums = localTopForumDataSource.getTopForums()
            val forumIds = if (topForums.isNotEmpty()) {
                withContext(Dispatchers.Default) { topForums.mapTo(HashSet()) { it.forumId } }
            } else {
                emptySet()
            }
            emit(forumIds)
        } while (iterator.hasNext().apply { iterator.next() })
    }

    suspend fun addTopForum(forum: LikedForum): Boolean {
        return localTopForumDataSource
                .add(forum.id)
                .also { succeed -> notifyDataChanged(succeed) }
    }

    suspend fun removeTopForum(forum: LikedForum): Boolean {
        return localTopForumDataSource
            .delete(forum.id)
            .also { notifyDataChanged(succeed = it) }
    }

    private fun notifyDataChanged(succeed: Boolean) {
        if (succeed) refresh.trySend(Unit)
    }

    private suspend fun List<LikeForum>.mapUiModel() = withContext(Dispatchers.Default) {
        map {
            LikedForum(
                avatar = it.avatar,
                id = it.forum_id,
                name = it.forum_name,
                isSign = it.is_sign == 1,
                level = "Lv.${it.level_id}"
            )
        }
    }
}