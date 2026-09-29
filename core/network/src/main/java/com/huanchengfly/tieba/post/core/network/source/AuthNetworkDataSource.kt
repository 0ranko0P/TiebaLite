package com.huanchengfly.tieba.post.core.network.source

import com.huanchengfly.tieba.post.core.network.exception.TiebaException
import com.huanchengfly.tieba.post.core.network.model.InitNickNameBean
import com.huanchengfly.tieba.post.core.network.model.LoginBean
import com.huanchengfly.tieba.post.core.network.model.Sync
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import javax.inject.Inject

interface AuthNetworkDataSource {

    /**
     * 登录
     */
    suspend fun loginWithInit(bduss: String, sToken: String): Pair<LoginBean, InitNickNameBean.UserInfo>

    suspend fun syncClient(clientId: String?): Sync
}

internal class RetrofitAuthNetworkDataSource @Inject constructor(
    private val tiebaApi: ITiebaApi,
): AuthNetworkDataSource {

    override suspend fun loginWithInit(
        bduss: String,
        sToken: String
    ): Pair<LoginBean, InitNickNameBean.UserInfo> {
        require(bduss.isNotEmpty())
        require(sToken.isNotEmpty())

        val loginBean = tiebaApi.loginFlow(bduss, sToken).firstOrThrow()
        var errorCode = loginBean.errorCode.toIntOrNull() ?: 0
        if (errorCode != 0) {
            throw TiebaException("Login error: $errorCode")
        }

        val nameBean = tiebaApi.initNickNameFlow(bduss, sToken).firstOrThrow()
        errorCode = nameBean.errorCode.toIntOrNull() ?: 0
        if (errorCode != 0) {
            throw TiebaException("Load user info failed: $errorCode")
        }

        return loginBean to nameBean.userInfo
    }

    override suspend fun syncClient(clientId: String?): Sync {
        return tiebaApi.syncFlow(clientId)
            .firstOrThrow()
            .also {
                requireNotNull(it.client) { "Null Client!" }
                requireNotNull(it.wlConfig) { "Null Wl Config!" }
            }
    }
}