package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.ForumRecommend.LikeForum
import com.huanchengfly.tieba.post.api.models.GetForumListBean
import com.huanchengfly.tieba.post.api.models.GetForumListBean.ForumInfo
import com.huanchengfly.tieba.post.api.models.MSignBean.Info
import com.huanchengfly.tieba.post.api.models.MSignFailed
import com.huanchengfly.tieba.post.api.models.MSignSuccess
import com.huanchengfly.tieba.post.api.models.SignResultBean.UserInfo
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaMSignException
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.repository.user.OKSignRepositoryImp
import com.huanchengfly.tieba.post.repository.user.OKSignRepositoryImp.Companion.ForumSignParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface OKSignNetworkDataSource {

    /**
     * 获取吧列表
     */
    suspend fun getForumList(): GetForumListBean

    suspend fun getForumRecommendList(): List<LikeForum>

    /**
     * 官方一键签到（实验性）
     * */
    suspend fun requestOfficialSign(forums: List<ForumSignParam>, tbs: String): List<Info>

    suspend fun requestSign(forumId: Long, forumName: String, tbs: String): UserInfo
}

object OKSignNetworkDataSourceIml : OKSignNetworkDataSource {

    override suspend fun getForumList(): GetForumListBean {
        return TiebaApi.getInstance()
            .getForumListFlow()
            .firstOrThrow()
            .apply {
                val errorCode = this.errorCode.toIntOrNull() ?: 0
                if (errorCode != 0) throw TiebaApiException(CommonResponse(errorCode, error.toString()))
            }
    }

    override suspend fun getForumRecommendList(): List<LikeForum> {
        return TiebaApi.getInstance()
            .forumRecommendFlow()
            .firstOrThrow()
            .run {
                val errorCode = this.errorCode.toIntOrNull() ?: 0
                if (errorCode != 0) {
                    throw TiebaApiException(CommonResponse(errorCode, errorMsg))
                } else {
                    this.likeForum
                }
            }
    }

    override suspend fun requestOfficialSign(forums: List<ForumSignParam>, tbs: String): List<Info> {
        require(forums.isNotEmpty())

        val forumIds = withContext(Dispatchers.Default) {
            forums.joinToString(",") { it.forumId.toString() }
        }
        val result = TiebaApi.getInstance()
            .mSign(forumIds, tbs)
            .firstOrThrow()
            .apply {
                val errorCode = this.errorCode.toIntOrNull() ?: 0
                if (errorCode != 0) throw TiebaApiException(CommonResponse(errorCode, error.toString()))
            }

        when (result) {
            is MSignSuccess -> return result.info

            is MSignFailed -> throw TiebaMSignException(result.error, result.signNotice)

            else -> throw RuntimeException("Unknow type: ${result::class.simpleName}")
        }
    }

    override suspend fun requestSign(forumId: Long, forumName: String, tbs: String): UserInfo {
        return TiebaApi.getInstance().signFlow(forumId.toString(), forumName, tbs)
            .firstOrThrow()
            .apply {
                val errorCode = this.errorCode?.toIntOrNull() ?: 0
                if (errorCode != 0) {
                    throw TiebaApiException(CommonResponse(errorCode, errorMsg.orEmpty()))
                }
            }
            .userInfo ?: throw TiebaException("User info is null")
    }
}