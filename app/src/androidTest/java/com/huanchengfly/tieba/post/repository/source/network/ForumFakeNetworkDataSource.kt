package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.core.network.model.LikeForumResultBean
import com.huanchengfly.tieba.post.core.network.model.SignResultBean
import com.huanchengfly.tieba.post.core.network.model.protos.GeneralTabList.GeneralTabListResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.RecommendForumInfo
import com.huanchengfly.tieba.post.core.network.model.protos.forumRuleDetail.ForumRuleDetailResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.frsPage.FrsPageResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.threadList.ThreadListResponseData
import com.huanchengfly.tieba.post.core.network.source.ForumNetworkDataSource

/**
 * Fake implementation of the [ForumNetworkDataSource]
 */
internal object ForumFakeNetworkDataSource: ForumNetworkDataSource {
    override suspend fun loadForumDetail(forumId: Long): RecommendForumInfo {
        TODO("Not yet implemented")
    }

    override suspend fun frsPage(
        forumName: String,
        page: Int,
        loadType: Int,
        sortType: Int,
        goodClassifyId: Int?
    ): FrsPageResponseData {
        TODO("Not yet implemented")
    }

    override suspend fun loadThread(
        forumId: Long,
        forumName: String,
        page: Int,
        sortType: Int,
        threadIds: List<Long>
    ): ThreadListResponseData {
        TODO("Not yet implemented")
    }

    override suspend fun loadGeneralTabList(
        forumId: Long,
        forumName: String,
        tabId: Int,
        tabType: Int,
        tabName: String,
        isGeneralTab: Int,
        pn: Int,
        sortType: Int,
        lastThreadId: Long,
        isDefaultNavTab: Int
    ): GeneralTabListResponseData {
        TODO("Not yet implemented")
    }

    override suspend fun loadForumRule(forumId: Long): ForumRuleDetailResponseData {
        TODO("Not yet implemented")
    }

    override suspend fun dislike(forumId: Long, forumName: String, tbs: String) {
    }

    override suspend fun like(
        forumId: Long,
        forumName: String,
        tbs: String
    ): LikeForumResultBean.Info {
        TODO("Not yet implemented")
    }

    override suspend fun forumSignIn(
        forumId: Long,
        forumName: String,
        tbs: String
    ): SignResultBean.UserInfo {
        TODO("Not yet implemented")
    }
}