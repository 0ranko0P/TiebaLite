package com.huanchengfly.tieba.post.core.network.source

import com.huanchengfly.tieba.post.core.network.exception.NoConnectivityException
import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.exception.TiebaException
import com.huanchengfly.tieba.post.core.network.model.LikeForumResultBean
import com.huanchengfly.tieba.post.core.network.model.SignResultBean
import com.huanchengfly.tieba.post.core.network.model.protos.GeneralTabList.GeneralTabListResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.RecommendForumInfo
import com.huanchengfly.tieba.post.core.network.model.protos.ThreadInfo
import com.huanchengfly.tieba.post.core.network.model.protos.User
import com.huanchengfly.tieba.post.core.network.model.protos.forumRuleDetail.ForumRuleDetailResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.frsPage.FrsPageResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.threadList.ThreadListResponseData
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.commonResponse
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import com.huanchengfly.tieba.post.core.network.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface ForumNetworkDataSource {

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun loadForumDetail(forumId: Long): RecommendForumInfo

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun frsPage(
        forumName: String,
        page: Int,
        loadType: Int,
        sortType: Int,
        goodClassifyId: Int?
    ): FrsPageResponseData

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun loadThread(
        forumId: Long,
        forumName: String,
        page: Int,
        sortType: Int,
        threadIds: List<Long>,
    ): ThreadListResponseData

    suspend fun loadGeneralTabList(
        forumId: Long,
        forumName: String,
        tabId: Int,
        tabType: Int,
        tabName: String,
        isGeneralTab: Int,
        pn: Int = 1,
        sortType: Int = -1,
        lastThreadId: Long = 0,
        isDefaultNavTab: Int = 0,
    ): GeneralTabListResponseData

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun loadForumRule(forumId: Long): ForumRuleDetailResponseData

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun dislike(forumId: Long, forumName: String, tbs: String)

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun like(forumId: Long, forumName: String, tbs: String): LikeForumResultBean.Info

    @Throws(NoConnectivityException::class, TiebaException::class)
    suspend fun forumSignIn(forumId: Long, forumName: String, tbs: String): SignResultBean.UserInfo
}

internal class RetrofitForumDataSource @Inject constructor(
    private val tiebaApi: ITiebaApi,
    private val networkMonitor: NetworkMonitor,
): ForumNetworkDataSource {

    private val threadFilter: (ThreadInfo) -> Boolean = {
        it.ala_info == null &&  // 去他妈的直播
        it.forumInfo != null    // 去他妈的跨吧广告帖
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun loadForumDetail(forumId: Long): RecommendForumInfo {
        return tiebaApi
            .getForumDetailFlow(forumId)
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
            .firstOrThrow()
            .run {
                data_?.forum_info ?: throw TiebaApiException(this.error.commonResponse)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun frsPage(
        forumName: String,
        page: Int,
        loadType: Int,
        sortType: Int,
        goodClassifyId: Int?
    ): FrsPageResponseData {
        val response = tiebaApi
            .frsPage(forumName, page, loadType, sortType, goodClassifyId)
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
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
    override suspend fun loadThread(
        forumId: Long,
        forumName: String,
        page: Int,
        sortType: Int,
        threadIds: List<Long>,
    ): ThreadListResponseData {
        val threadId = threadIds.joinToString(separator = ",") { "$it" }
        val response = tiebaApi
            .threadList(forumId, forumName, page, sortType, threadId)
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
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

    override suspend fun loadGeneralTabList(
        forumId: Long,
        forumName: String,
        tabId: Int,
        tabType: Int,
        tabName: String,
        isGeneralTab: Int,
        pn: Int,
        sortType: Int ,
        lastThreadId: Long,
        isDefaultNavTab: Int,
    ): GeneralTabListResponseData {
        val response = tiebaApi
            .generalTabList(
                forumId = forumId,
                forumName = forumName,
                tabId = tabId,
                tabType = tabType,
                tabName = tabName,
                isGeneralTab = isGeneralTab,
                pn = pn,
                sortType = sortType,
                lastThreadId = lastThreadId,
                isDefaultNavTab = isDefaultNavTab
            )
            .firstOrThrow()
        if (response.data_?.general_list.isNullOrEmpty()) {
            throw TiebaApiException(commonResponse = response.error.commonResponse)
        }

        return withContext(Dispatchers.Default) {
            response.data_.general_list
                .filter(threadFilter)
                .addUsers(response.data_.user_list)
                .let { new ->
                    response.data_.copy(general_list = new)
                }
        }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun loadForumRule(forumId: Long): ForumRuleDetailResponseData {
        return tiebaApi
            .forumRuleDetailFlow(forumId)
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = error.commonResponse)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun dislike(forumId: Long, forumName: String, tbs: String) {
        tiebaApi
            .unlikeForumFlow(forumId = forumId.toString(), forumName = forumName, tbs = tbs)
            .firstOrThrow()
            .let {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun like(forumId: Long, forumName: String, tbs: String): LikeForumResultBean.Info {
        return tiebaApi
            .likeForumFlow(forumId = forumId.toString(), forumName, tbs = tbs)
            .firstOrThrow()
            .info
    }

    @Throws(NoConnectivityException::class, TiebaException::class)
    override suspend fun forumSignIn(forumId: Long, forumName: String, tbs: String): SignResultBean.UserInfo {
        val response = tiebaApi
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
    return map { thread ->
        val user = userMap[thread.authorId]
        val fallback = thread.author

        thread.copy(
            author = user?.copy(
                name = user.name.takeUnless { it.isBlank() } ?: fallback?.name.orEmpty(),
                nameShow = user.nameShow.takeUnless { it.isBlank() } ?: fallback?.nameShow.orEmpty()
            ) ?: fallback
        )
    }
}