package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.models.TopicDetailDataBean
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListResponseData
import javax.inject.Inject

class HotTopicFakeNetworkDataSource @Inject constructor(): HotTopicNetworkDataSource {

    override suspend fun topicList(): TopicListResponseData {
        throw RuntimeException("Not yet implemented")
    }

    override suspend fun topicDetail(topicId: Long, topicName: String, isNew: Int, isShare: Int, page: Int, pageSize: Int, offset: Int, lastId: String): TopicDetailDataBean {
        throw RuntimeException("Not yet implemented")
    }
}