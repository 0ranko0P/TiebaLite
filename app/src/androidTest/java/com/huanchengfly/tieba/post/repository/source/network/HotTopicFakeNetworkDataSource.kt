package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.core.network.model.TopicDetailDataBean
import com.huanchengfly.tieba.post.core.network.model.protos.topicList.TopicListResponseData
import com.huanchengfly.tieba.post.core.network.source.HotTopicNetworkDataSource

/**
 * Fake implementation of the [HotTopicNetworkDataSource]
 */
internal object HotTopicFakeNetworkDataSource: HotTopicNetworkDataSource {

    override suspend fun topicList(): TopicListResponseData {
        throw RuntimeException("Not yet implemented")
    }

    override suspend fun topicDetail(topicId: Long, topicName: String, isNew: Int, isShare: Int, page: Int, pageSize: Int, offset: Int, lastId: String): TopicDetailDataBean {
        throw RuntimeException("Not yet implemented")
    }
}