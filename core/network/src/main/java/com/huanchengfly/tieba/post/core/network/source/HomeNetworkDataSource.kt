package com.huanchengfly.tieba.post.core.network.source

import com.huanchengfly.tieba.post.core.network.exception.NoConnectivityException
import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.exception.TiebaException
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.model.ForumGuideBean.LikeForum
import com.huanchengfly.tieba.post.core.network.model.MsgBean.MessageBean
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import com.huanchengfly.tieba.post.core.network.retrofit.getError
import com.huanchengfly.tieba.post.core.network.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

/**
 * Main entry point for accessing liked forums and new message data from the network.
 */
interface HomeNetworkDataSource {

    suspend fun getLikedForums(): List<LikeForum>

    suspend fun fetchNewMessage(): MessageBean
}

internal class HomeNetworkDataSourceImpl @Inject constructor(
    private val tiebaApi: ITiebaApi,
    private val networkMonitor: NetworkMonitor,
) : HomeNetworkDataSource {

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun getLikedForums(): List<LikeForum> {
        return tiebaApi
            .allForumGuideFlow()
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
            .firstOrThrow()
            .run {
                if (errorCode != 0) throw TiebaApiException(CommonResponse(errorCode, errorMsg))
                this.likeForum
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun fetchNewMessage(): MessageBean {
        return tiebaApi.msgFlow()
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
            .firstOrThrow()
            .run {
                if (errorCode != "0") throw TiebaApiException(commonResponse = this.getError())
                this.message ?: throw TiebaException("Null message")
            }
    }
}