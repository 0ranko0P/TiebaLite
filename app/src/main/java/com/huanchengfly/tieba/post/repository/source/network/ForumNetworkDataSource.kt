package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.SignResultBean
import com.huanchengfly.tieba.post.api.models.protos.RecommendForumInfo
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.forumRuleDetail.ForumRuleDetailResponseData
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponseData
import com.huanchengfly.tieba.post.api.models.protos.threadList.ThreadListResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.arch.firstOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ForumNetworkDataSource @Inject constructor() {

    /**
     * Note: ForumDetailFlow 未登录时返回的数据不全/为空, 需额外提供 ForumData
     * */
    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun loadForumDetail(forumId: Long): RecommendForumInfo {
        return TiebaApi.getInstance()
            .getForumDetailFlow(forumId)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                data_?.forum_info ?: throw TiebaException(this.error?.error_msg)
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
        if (response.data_?.forum == null) throw TiebaException(response.error?.error_msg)

        return withContext(Dispatchers.Default) {
            response.data_.thread_list
                .filter { it.ala_info == null } // 去他妈的直播
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
        if (response.data_?.thread_list == null) throw TiebaException(response.error?.error_msg)

        return withContext(Dispatchers.Default) {
            response.data_.thread_list
                .filter { it.ala_info == null } // 去他妈的直播
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
                data_ ?: throw TiebaException(message = this.error?.error_msg)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun forumSignIn(forumId: Long, forumName: String, tbs: String): SignResultBean.UserInfo {
        val response = TiebaApi.getInstance()
            .signFlow(forumId = forumId.toString(), forumName, tbs = tbs)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
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