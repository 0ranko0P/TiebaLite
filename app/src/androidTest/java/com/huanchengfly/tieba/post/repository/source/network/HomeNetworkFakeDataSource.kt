package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.models.GetForumListBean.ForumInfo
import com.huanchengfly.tieba.post.api.models.MsgBean
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.LikeForum
import com.huanchengfly.tieba.post.repository.source.TestData
import kotlin.random.Random

object HomeNetworkFakeDataSource: HomeNetworkDataSource {

    var nextMessage: MsgBean.MessageBean? = null

    var nextLikedForums = TestData.DummyGetForumListBean.forumInfo

    override suspend fun getLikedForums(): List<LikeForum> {
        val signForumList: List<ForumInfo> = nextLikedForums
        val forums = signForumList.map {
            LikeForum(forum_id = it.forumId, forum_name = it.forumName, avatar = "", is_sign = it.isSignIn, level_id = it.userLevel)
        }
        return forums
    }

    override suspend fun fetchNewMessage(): MsgBean.MessageBean {
        return nextMessage ?: MsgBean.MessageBean(replyMe = Random.nextInt(), atMe = 0, fans = 0)
    }
}