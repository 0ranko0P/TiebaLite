package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.LikeForumResultBean
import com.huanchengfly.tieba.post.api.models.SignResultBean
import com.huanchengfly.tieba.post.api.models.protos.RecommendForumInfo
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.forumRuleDetail.ForumRuleDetailResponseData
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponseData
import com.huanchengfly.tieba.post.api.models.protos.threadList.ThreadListResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.repository.source.network.ExploreNetworkDataSource.commonResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext

object ForumNetworkDataSource {

    private val threadFilter: (ThreadInfo) -> Boolean = {
        it.ala_info == null &&  // 去他妈的直播
        it.forumInfo != null    // 去他妈的跨吧广告帖
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun loadForumDetail(forumId: Long): RecommendForumInfo {
        return TiebaApi.getInstance()
            .getForumDetailFlow(forumId)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                data_?.forum_info ?: throw TiebaApiException(this.error.commonResponse)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun frsPage(
        forumName: String,
        page: Int,
        loadType: Int,
        sortType: Int,
        goodClassifyId: Int?
    ): FrsPageResponseData {
        val response = TiebaApi.getInstance()
            .frsPage(forumName, page, loadType, sortType, goodClassifyId)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
        if (response.data_?.forum == null) throw TiebaApiException(response.error.commonResponse)

        return withContext(Dispatchers.Default) {
            response.data_.thread_list
                .filter(threadFilter)
                .addUsers(response.data_.user_list)
                .let { new ->
                    response.data_.copy(thread_list = new)
                }
        }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun loadThread(
        forumId: Long,
        forumName: String,
        page: Int,
        sortType: Int,
        threadIds: List<Long>,
    ): ThreadListResponseData {
        val threadId = threadIds.joinToString(separator = ",") { "$it" }
        val response = TiebaApi.getInstance()
            .threadList(forumId, forumName, page, sortType, threadId)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
        if (response.data_?.thread_list == null) throw TiebaApiException(response.error.commonResponse)

        return withContext(Dispatchers.Default) {
            response.data_.thread_list
                .filter(threadFilter)
                .addUsers(response.data_.user_list)
                .let { new ->
                    response.data_.copy(thread_list = new)
                }
        }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun loadForumRule(forumId: Long): ForumRuleDetailResponseData {
        return TiebaApi.getInstance()
            .forumRuleDetailFlow(forumId)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = error.commonResponse)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun dislike(forumId: Long, forumName: String, tbs: String) {
        TiebaApi.getInstance()
            .unlikeForumFlow(forumId = forumId.toString(), forumName = forumName, tbs = tbs)
            .firstOrThrow()
            .let {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun like(forumId: Long, forumName: String, tbs: String): LikeForumResultBean.Info {
        return TiebaApi.getInstance()
            .likeForumFlow(forumId = forumId.toString(), forumName, tbs = tbs)
            .firstOrThrow()
            .info
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun forumSignIn(forumId: Long, forumName: String, tbs: String): SignResultBean.UserInfo {
        val response = TiebaApi.getInstance()
            .signFlow(forumId = forumId.toString(), forumName, tbs = tbs)
            .firstOrThrow()

        val info = response.userInfo ?: throw TiebaException(message = response.errorMsg)
        if (info.signBonusPoint == null || info.userSignRank == null) {
            throw TiebaException("Invalid SignIn data")
        }
        return info
    }
}

private fun List<ThreadInfo>.addUsers(userList: List<User>): List<ThreadInfo> {
    if (isEmpty()) return this
    val userMap = userList.associateBy { it.id }
    return map { it.copy(author = userMap[it.authorId]) }
}