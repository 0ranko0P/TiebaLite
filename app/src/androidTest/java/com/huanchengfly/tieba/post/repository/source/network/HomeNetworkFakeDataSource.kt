package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.models.GetForumListBean.ForumInfo
import com.huanchengfly.tieba.post.api.models.MsgBean
import com.huanchengfly.tieba.post.api.models.ForumGuideBean.LikeForum
import com.huanchengfly.tieba.post.repository.source.TestData
import javax.inject.Inject
import kotlin.random.Random

class HomeNetworkFakeDataSource @Inject constructor(): HomeNetworkDataSource {

    var nextMessage: MsgBean.MessageBean? = null

    var nextLikedForums = TestData.DummyGetForumListBean.forumInfo

    override suspend fun getLikedForums(): List<LikeForum> {
        val signForumList: List<ForumInfo> = nextLikedForums
        val forums = signForumList.map {
            LikeForum(forumId = it.forumId, forumName = it.forumName, avatar = "", isSign = it.isSignIn, levelId = it.userLevel)
        }
        return forums
    }

    override suspend fun fetchNewMessage(): MsgBean.MessageBean {
        return nextMessage ?: MsgBean.MessageBean(replyMe = Random.nextInt(), atMe = 0, fans = 0)
    }
}