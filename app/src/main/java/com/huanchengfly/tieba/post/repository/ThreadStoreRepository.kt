package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.network.model.ThreadStoreBean.ThreadStoreInfo
import com.huanchengfly.tieba.post.core.network.session.CredentialProvider
import com.huanchengfly.tieba.post.core.network.source.ThreadStoreNetworkDataSource
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.ThreadStore
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.toLongOrDefault
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that manages user thread collection
 * */
@Singleton
class ThreadStoreRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val networkDataSource: ThreadStoreNetworkDataSource,
    private val credentialProvider: CredentialProvider,
) {

    /**
     * 加载收藏的帖子
     * */
    suspend fun load(page: Int = 0, limit: Int = LOAD_LIMIT): List<ThreadStore> {
        val data = networkDataSource.load(page, limit)
        val showBothName = settingsRepository.habitSettings.snapshot().showBothName
        return data.mapUiModel(showBothName)
    }

    suspend fun add(threadId: Long, postId: Long) = runCatching {
        networkDataSource.add(threadId, postId)
    }

    /**
     * 取消收藏这个帖子
     * */
    suspend fun remove(thread: ThreadStore) = runCatching {
        networkDataSource.remove(threadId = thread.id, tbs = credentialProvider.requireTbs())
    }

    /**
     * 取消收藏这个帖子
     * */
    suspend fun remove(threadId: Long, forumId: Long?, tbs: String?) {
        networkDataSource.remove(threadId, forumId, tbs = tbs ?: credentialProvider.requireTbs())
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