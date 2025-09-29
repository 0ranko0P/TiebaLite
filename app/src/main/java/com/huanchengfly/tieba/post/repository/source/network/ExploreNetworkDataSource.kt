package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.protos.Error
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListResponseData
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponseData
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.models.DislikeBean
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.HOT_THREAD_TAB_ALL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main entry point for accessing explore date from the network.
 */
object ExploreNetworkDataSource {

    suspend fun loadHotThread(tabCode: String = HOT_THREAD_TAB_ALL): HotThreadListResponseData {
        return TiebaApi.getInstance().hotThreadListFlow(tabCode)
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaException(message = this.error?.error_msg)
            }
    }

    suspend fun loadMorePersonalizedThread(page: Int): PersonalizedResponseData {
        require(page > 1)
        return personalizedThread(page, loadType = 2)
    }

    suspend fun refreshPersonalizedThread() = personalizedThread(page = 1, loadType = 1)

    /**
     * 个性推荐
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    private suspend fun personalizedThread(page: Int = 1, loadType: Int): PersonalizedResponseData {
        val data = TiebaApi.getInstance()
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

    suspend fun submitDislikePersonalizedThread(
        threadId: Long,
        forumId: Long?,
        clickTimeMill: Long,
        dislikeIds: String,
        extra: String
    ) {
        TiebaApi.getInstance()
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

    suspend fun refreshUserLikeThread(lastRequestUnix: Long): UserLikeResponseData {
        return loadUserLikeThread(pageTag = "", lastRequestUnix, loadType = 1)
    }

    suspend fun loadMoreUserLikeThread(pageTag: String, lastRequestUnix: Long): UserLikeResponseData {
        require(lastRequestUnix > 0) { "Invalid Unix timestamp: $lastRequestUnix" }
        return loadUserLikeThread(pageTag, lastRequestUnix, loadType = 2)
    }

    private suspend fun loadUserLikeThread(
        pageTag: String,
        lastRequestUnix: Long,
        loadType: Int
    ): UserLikeResponseData {
        return TiebaApi.getInstance()
            .userLikeFlow(pageTag, lastRequestUnix, loadType)
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = this.error.commonResponse)
            }
    }

    val Error?.commonResponse
        get() = CommonResponse(
            errorCode = this?.error_code ?: com.huanchengfly.tieba.post.api.Error.ERROR_UNKNOWN,
            errorMsg = this?.error_msg.orEmpty()
        )

}