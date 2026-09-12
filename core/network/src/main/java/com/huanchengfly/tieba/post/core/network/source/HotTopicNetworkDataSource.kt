package com.huanchengfly.tieba.post.core.network.source

import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.model.TopicDetailDataBean
import com.huanchengfly.tieba.post.core.network.model.protos.topicList.TopicListResponseData
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.commonResponse
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import javax.inject.Inject

/**
 * Main entry point for accessing topic data from the network.
 */
interface HotTopicNetworkDataSource {

    /**
     * 话题榜
     */
    suspend fun topicList(): TopicListResponseData

    /**
     * 话题详情
     *
     * @param topicId 话题id
     * @param topicName 话题名
     * @param isNew
     * @param isShare
     * @param page 分页页码(初始为1)
     * @param pageSize 分页大小
     * @param offset （分页页码-1）* 分页大小
     * @param lastId 上次返回的最后一个feedid，初次请求留空
     */
    suspend fun topicDetail(
        topicId: Long,
        topicName: String,
        isNew: Int,
        isShare: Int,
        page: Int,
        pageSize: Int,
        offset: Int,
        lastId: String,
    ): TopicDetailDataBean
}

internal class HotTopicNetworkDataSourceImpl @Inject constructor(
    private val tiebaApi: ITiebaApi,
): HotTopicNetworkDataSource {

    override suspend fun topicList(): TopicListResponseData {
        return tiebaApi
            .topicListFlow()
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = this.error.commonResponse)
            }
    }

    override suspend fun topicDetail(
        topicId: Long,
        topicName: String,
        isNew: Int,
        isShare: Int,
        page: Int,
        pageSize: Int,
        offset: Int,
        lastId: String,
    ): TopicDetailDataBean {
        return tiebaApi
            .topicDetailFlow(
                topicId = topicId.toString(),
                topicName = topicName,
                isNew = isNew,
                isShare = isShare,
                page = page,
                pageSize = pageSize,
                offset = offset,
                lastId = lastId
            )
            .firstOrThrow()
            .run {
                if (errorCode == 0) data else throw TiebaApiException(CommonResponse(errorCode, errorMsg))
            }
    }
}