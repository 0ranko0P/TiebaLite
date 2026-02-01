package com.huanchengfly.tieba.post.repository.source.network

import android.text.TextUtils
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.FollowBean
import com.huanchengfly.tieba.post.api.models.InitNickNameBean
import com.huanchengfly.tieba.post.api.models.LoginBean
import com.huanchengfly.tieba.post.api.models.PermissionListBean
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.repository.source.network.ExploreNetworkDataSource.commonResponse

/**
 * Main entry point for accessing user profile data from the network.
 */
object UserProfileNetworkDataSource {

    /**
     * 登录
     */
    suspend fun loginWithInit(bduss: String, sToken: String): Pair<LoginBean, InitNickNameBean.UserInfo> {
        require(bduss.isNotEmpty())
        require(sToken.isNotEmpty())

        val loginBean = TiebaApi.getInstance().loginFlow(bduss, sToken).firstOrThrow()
        var errorCode = loginBean.errorCode.toIntOrNull() ?: 0
        // check LoginBean
        if (errorCode != 0) throw TiebaException("Login error: $errorCode")

        val nameBean = TiebaApi.getInstance().initNickNameFlow(bduss, sToken).firstOrThrow()
        // check UserBean
        errorCode = nameBean.errorCode.toIntOrNull() ?: 0
        if (errorCode != 0) throw TiebaException("Load user info failed: $errorCode")

        return loginBean to nameBean.userInfo
    }

    suspend fun loadUserThreadPost(uid: Long, page: Int, isThread: Boolean): List<PostInfoList> {
        require(uid > 0) { "Invalid user ID: $uid." }
        require(page >= 1) { "Invalid page number: $page." }

        return TiebaApi.getInstance()
            .userPostFlow(uid, page, isThread)
            .firstOrThrow()
            .run {
                data_?.post_list ?: throw TiebaApiException(commonResponse = error.commonResponse)
            }
    }

    suspend fun loadUserProfile(uid: Long): User {
        require(uid > 0) { "Invalid user ID: $uid." }

        return TiebaApi.getInstance()
            .userProfileFlow(uid)
            .firstOrThrow()
            .run {
                val data = data_ ?: throw TiebaApiException(commonResponse = error.commonResponse)
                data.user ?: throw TiebaException("Null user data")
            }
    }

    suspend fun requestFollowUser(portrait: String, tbs: String): FollowBean.Info {
        if (TextUtils.isEmpty(tbs)) throw TiebaNotLoggedInException()
        if (TextUtils.isEmpty(portrait)) throw IllegalArgumentException("Invalid user portrait")

        return TiebaApi.getInstance()
            .followFlow(portrait, tbs)
            .firstOrThrow()
            .apply {
                if (errorCode != 0) throw TiebaApiException(CommonResponse(errorCode, errorMsg))
            }
            .info
    }

    suspend fun requestUnfollowUser(portrait: String, tbs: String) {
        if (TextUtils.isEmpty(tbs)) throw TiebaNotLoggedInException()
        if (TextUtils.isEmpty(portrait)) throw IllegalArgumentException("Invalid user portrait")

        TiebaApi.getInstance()
            .unfollowFlow(portrait, tbs)
            .firstOrThrow()
            .run {
                if (errorCode != 0) throw TiebaApiException(commonResponse = this)
            }
    }

    /**
     * 查询单个用户的拉黑信息
     * @param uid 用户 id
     */
    suspend fun getUserBlackInfo(uid: Long): PermissionListBean {
        return TiebaApi.getInstance()
            .getUserBlackInfo(uid)
            .firstOrThrow()
            .run {
                if (errorCode != 0 || permList == null) {
                    throw TiebaApiException(CommonResponse(errorCode, errorMsg ?: "Null"))
                } else {
                    this.permList
                }
            }
    }

    /**
     * 禁止用户互动（转、评、赞踩、@）
     * @param uid 用户 id
     * @param tbs tbs（长）
     * @param permList 参数列表：关注，互动，私信。(0,允许 1,禁止)
     */
    suspend fun setUserBlack(uid: Long, tbs: String, permList: PermissionListBean) {
        TiebaApi.getInstance()
            .setUserBlack(uid, tbs, permList)
            .firstOrThrow()
            .run {
                if (errorCode != 0) throw TiebaApiException(commonResponse = this)
            }
    }
}