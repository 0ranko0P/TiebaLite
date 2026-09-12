package com.huanchengfly.tieba.post.core.network.source

import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.exception.TiebaException
import com.huanchengfly.tieba.post.core.network.exception.TiebaMSignException
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.model.ForumGuideBean
import com.huanchengfly.tieba.post.core.network.model.ForumRecommend.LikeForum
import com.huanchengfly.tieba.post.core.network.model.GetForumListBean
import com.huanchengfly.tieba.post.core.network.model.MSignBean.Info
import com.huanchengfly.tieba.post.core.network.model.MSignFailed
import com.huanchengfly.tieba.post.core.network.model.MSignSuccess
import com.huanchengfly.tieba.post.core.network.model.SignResultBean.UserInfo
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import com.huanchengfly.tieba.post.core.network.source.OKSignNetworkDataSource.Companion.ForumSignParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

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

    companion object {
        data class ForumSignParam(val name: String, val forumId: Long, val signed: Boolean) {

            constructor(forum: ForumGuideBean.LikeForum): this(
                name = forum.forumName,
                forumId = forum.forumId,
                signed = forum.isSign == 1
            )
        }
    }
}

internal class OKSignNetworkDataSourceImpl @Inject constructor(
    private val tiebaApi: ITiebaApi,
) : OKSignNetworkDataSource {

    override suspend fun getForumList(): GetForumListBean {
        return tiebaApi
            .getForumListFlow()
            .firstOrThrow()
            .apply {
                val errorCode = this.errorCode.toIntOrNull() ?: 0
                if (errorCode != 0) throw TiebaApiException(CommonResponse(errorCode, error.toString()))
            }
    }

    override suspend fun getForumRecommendList(): List<LikeForum> {
        return tiebaApi
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
        val result = tiebaApi
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
        return tiebaApi.signFlow(forumId.toString(), forumName, tbs)
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