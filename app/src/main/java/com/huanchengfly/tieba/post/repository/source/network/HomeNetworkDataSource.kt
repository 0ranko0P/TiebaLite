package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.getError
import com.huanchengfly.tieba.post.api.models.MsgBean.MessageBean
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.LikeForum
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.repository.source.network.ExploreNetworkDataSource.commonResponse
import kotlinx.coroutines.flow.catch

/**
 * Main entry point for accessing liked forums and new message data from the network.
 */
interface HomeNetworkDataSource {

    suspend fun getLikedForums(): List<LikeForum>

    suspend fun fetchNewMessage(): MessageBean
}

object HomeNetworkDataSourceImp : HomeNetworkDataSource {

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun getLikedForums(): List<LikeForum> {
        return TiebaApi.getInstance()
            .forumRecommendNewFlow()
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                this.data_?.like_forum ?: throw TiebaApiException(error.commonResponse)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun fetchNewMessage(): MessageBean {
        return TiebaApi.getInstance().msgFlow()
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                if (errorCode != "0") throw TiebaApiException(commonResponse = this.getError())
                this.message ?: throw TiebaException("Null message")
            }
    }
}