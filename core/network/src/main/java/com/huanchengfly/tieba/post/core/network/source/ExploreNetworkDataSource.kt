package com.huanchengfly.tieba.post.core.network.source

import com.huanchengfly.tieba.post.core.network.retrofit.commonResponse
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.exception.TiebaException
import com.huanchengfly.tieba.post.core.network.model.protos.hotThreadList.HotThreadListResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.personalized.PersonalizedResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.userLike.UserLikeResponseData
import com.huanchengfly.tieba.post.core.network.model.web.DislikeBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Main entry point for accessing explore date from the network.
 */
interface ExploreNetworkDataSource {

    suspend fun loadHotThread(tabCode: String): HotThreadListResponseData

    suspend fun loadMorePersonalizedThread(page: Int): PersonalizedResponseData

    suspend fun refreshPersonalizedThread(): PersonalizedResponseData

    suspend fun submitDislikePersonalizedThread(
        threadId: Long,
        forumId: Long?,
        clickTimeMill: Long,
        dislikeIds: String,
        extra: String
    )

    suspend fun refreshUserLikeThread(lastRequestUnix: Long): UserLikeResponseData

    suspend fun loadMoreUserLikeThread(pageTag: String, lastRequestUnix: Long): UserLikeResponseData
}

internal class RetrofitExploreDataSource @Inject constructor(
    private val tiebaApi: ITiebaApi,
): ExploreNetworkDataSource {

    override suspend fun loadHotThread(tabCode: String): HotThreadListResponseData {
        return tiebaApi.hotThreadListFlow(tabCode)
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaException(message = this.error?.error_msg)
            }
    }

    override suspend fun loadMorePersonalizedThread(page: Int): PersonalizedResponseData {
        require(page > 1)
        return personalizedThread(page, loadType = 2)
    }

    override suspend fun refreshPersonalizedThread(): PersonalizedResponseData {
        return personalizedThread(page = 1, loadType = 1)
    }

    /**
     * 个性推荐
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    private suspend fun personalizedThread(page: Int = 1, loadType: Int): PersonalizedResponseData {
        val data = tiebaApi
            .personalizedProtoFlow(loadType, page)
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = this.error.commonResponse)
            }

        return withContext(Dispatchers.Default) {
            // 直播
            val liveThreadIds = hashSetOf<Long>()
            val threadList = data.thread_list.filter {
                if (it.ala_info != null) liveThreadIds.add(it.id) // record live threads id
                it.ala_info == null
            }

            data.copy(
                thread_list = threadList,
                thread_personalized = data.thread_personalized.filter { !liveThreadIds.contains(it.tid) }
            )
        }
    }

    override suspend fun submitDislikePersonalizedThread(
        threadId: Long,
        forumId: Long?,
        clickTimeMill: Long,
        dislikeIds: String,
        extra: String
    ) {
        tiebaApi
            .submitDislikeFlow(
                DislikeBean(
                    threadId = threadId.toString(),
                    dislikeIds = dislikeIds,
                    forumId = forumId?.toString(),
                    clickTime = clickTimeMill,
                    extra = extra,
                )
            )
            .firstOrThrow()
            .let {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }

    override suspend fun refreshUserLikeThread(lastRequestUnix: Long): UserLikeResponseData {
        return loadUserLikeThread(pageTag = "", lastRequestUnix, loadType = 1)
    }

    override suspend fun loadMoreUserLikeThread(pageTag: String, lastRequestUnix: Long): UserLikeResponseData {
        require(lastRequestUnix > 0) { "Invalid Unix timestamp: $lastRequestUnix" }
        return loadUserLikeThread(pageTag, lastRequestUnix, loadType = 2)
    }

    private suspend fun loadUserLikeThread(
        pageTag: String,
        lastRequestUnix: Long,
        loadType: Int
    ): UserLikeResponseData {
        return tiebaApi
            .userLikeFlow(pageTag, lastRequestUnix, loadType)
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = this.error.commonResponse)
            }
    }
}