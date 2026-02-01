package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.TopicDetailDataBean
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.repository.source.network.ExploreNetworkDataSource.commonResponse
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

class HotTopicNetworkDataSourceImpl @Inject constructor(): HotTopicNetworkDataSource {

    override suspend fun topicList(): TopicListResponseData {
        return TiebaApi.getInstance()
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
        return TiebaApi.getInstance()
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