package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.interfaces.ITiebaApi
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.LikeForum
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.repository.source.network.ExploreNetworkDataSource.commonResponse
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

/**
 * Main entry point for accessing liked forums data from the network.
 *
 * @see ITiebaApi.forumRecommendNewFlow
 */
class HomeNetworkDataSource @Inject constructor() {

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun getLikedForums(): List<LikeForum> {
        return TiebaApi.getInstance()
            .forumRecommendNewFlow()
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                this.data_?.like_forum ?: throw TiebaApiException(error.commonResponse)
            }
    }
}