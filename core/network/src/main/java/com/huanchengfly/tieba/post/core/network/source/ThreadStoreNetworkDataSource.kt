package com.huanchengfly.tieba.post.core.network.source

import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.exception.TiebaException
import com.huanchengfly.tieba.post.core.network.model.ThreadStoreBean.ThreadStoreInfo
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import javax.inject.Inject

/**
 * Main entry point for accessing user's thread collection from the network.
 */
interface ThreadStoreNetworkDataSource {

    suspend fun load(page: Int = 0, limit: Int): List<ThreadStoreInfo>

    suspend fun add(threadId: Long, postId: Long)

    suspend fun remove(threadId: Long, forumId: Long? = null, tbs: String)
}

internal class RetrofitThreadStoreNetworkDataSource @Inject constructor(
    private val tiebaApi: ITiebaApi,
): ThreadStoreNetworkDataSource {

    override suspend fun load(page: Int, limit: Int): List<ThreadStoreInfo> {
        return tiebaApi
            .threadStoreFlow(page = page, pageSize = limit)
            .firstOrThrow()
            .run {
                this.storeThread ?: throw TiebaException(this.error?.errorMsg)
            }
    }

    override suspend fun add(threadId: Long, postId: Long) {
        tiebaApi
            .addStoreFlow(threadId, postId)
            .firstOrThrow()
            .also {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }

    override suspend fun remove(threadId: Long, forumId: Long?, tbs: String) {
        require(threadId > 0) { "Illegal Thread ID: $threadId" }
        tiebaApi
            .removeStoreFlow(threadId = threadId, forumId = forumId, tbs = tbs)
            .firstOrThrow()
            .also {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }
}