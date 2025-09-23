package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.interfaces.ITiebaApi
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean.ThreadStoreInfo
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.arch.firstOrThrow

/**
 * Main entry point for accessing user's thread collection from the network.
 *
 * @see ITiebaApi.threadStoreFlow
 */
object ThreadStoreNetworkDataSource {

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun load(page: Int = 0, limit: Int): List<ThreadStoreInfo> {
        return TiebaApi.getInstance()
            .threadStoreFlow(page = page, pageSize = limit)
            .firstOrThrow()
            .run {
                this.storeThread ?: throw TiebaException(this.error?.errorMsg)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun add(threadId: Long, postId: Long) {
        TiebaApi.getInstance()
            .addStoreFlow(threadId, postId)
            .firstOrThrow()
            .also {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun remove(threadId: Long, forumId: Long? = null, tbs: String) {
        require(threadId > 0) { "Illegal Thread ID: $threadId" }
        TiebaApi.getInstance()
            .removeStoreFlow(threadId = threadId, forumId = forumId, tbs = tbs)
            .firstOrThrow()
            .also {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }
}