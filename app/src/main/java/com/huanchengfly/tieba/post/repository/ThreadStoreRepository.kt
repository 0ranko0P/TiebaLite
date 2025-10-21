package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.models.ThreadStoreBean.ThreadStoreInfo
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.repository.source.network.ThreadStoreNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.ThreadStore
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.internal.toLongOrDefault
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that manages user thread collection
 * */
@Singleton
class ThreadStoreRepository @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

    private val networkDataSource = ThreadStoreNetworkDataSource

    private val tbs: String
        get() = AccountUtil.getInstance().currentAccount.value?.tbs ?: throw TiebaNotLoggedInException()

    /**
     * 加载收藏的帖子
     * */
    suspend fun load(page: Int = 0, limit: Int = LOAD_LIMIT): Result<List<ThreadStore>> = runCatching {
        val data = networkDataSource.load(page, limit)
        val showBothName = settingsRepository.habitSettings.flow.first().showBothName
        data.mapUiModel(showBothName)
    }

    suspend fun add(threadId: Long, postId: Long) = runCatching {
        networkDataSource.add(threadId, postId)
    }

    /**
     * 取消收藏这个帖子
     * */
    suspend fun remove(thread: ThreadStore) = runCatching {
        networkDataSource.remove(threadId = thread.id, tbs = tbs)
    }

    /**
     * 取消收藏这个帖子
     * */
    suspend fun remove(threadId: Long, forumId: Long?, tbs: String?) {
        networkDataSource.remove(threadId, forumId, tbs ?: this.tbs)
    }

    companion object {

        const val LOAD_LIMIT = 20

        private suspend fun List<ThreadStoreInfo>.mapUiModel(showBothName: Boolean): List<ThreadStore> {
            return if (isNotEmpty()) {
                withContext(Dispatchers.Default) {
                    map {
                        ThreadStore(
                            id = it.threadId,
                            title = it.title,
                            forumName = it.forumName,
                            isDeleted = it.isDeleted == 1,
                            maxPid = it.maxPid.toLongOrDefault(0),
                            markPid = it.markPid.toLongOrDefault(0),
                            postNo = it.postNo,
                            count = it.count,
                            author = it.author.run {
                                Author(
                                    id = requireNotNull(lzUid) { "Null author id of thread: ${it.threadId}" },
                                    name = StringUtil.getUserNameString(showBothName, name ?: "", nameShow),
                                    avatarUrl = StringUtil.getAvatarUrl(userPortrait)
                                )
                            }
                        )
                    }
                }
            } else {
                emptyList()
            }
        }
    }
}