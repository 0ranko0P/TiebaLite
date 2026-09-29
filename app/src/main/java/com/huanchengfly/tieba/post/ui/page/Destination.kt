package com.huanchengfly.tieba.post.ui.page

import androidx.navigation3.runtime.NavKey
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import kotlinx.serialization.Serializable

sealed interface Destination: NavKey {

    @Serializable
    data object AppTheme: Destination

    @Serializable
    data class CopyText(val text: String): Destination

    /**
     * @param forumName 吧名
     * @param avatar 吧头像Url
     * @param transitionKey 过渡动画额外标识键. 此键将与吧名组合为唯一标识键, 确保推荐页, 搜索页中多个贴子来
     * 自同一个贴吧时过渡动画的唯一性.
     * */
    @Serializable
    data class Forum(val forumName: String, val avatar: String? = null, val transitionKey: String? = null): Destination

    @Serializable
    data class ForumDetail(val forumName: String): Destination

    @Serializable
    data class ForumSearchPost(val forumName: String, val forumId: Long): Destination

    @Serializable
    data class ForumRuleDetail(val forumId: Long): Destination

    @Serializable
    data object History: Destination

    @Serializable
    data object HotTopicList: Destination

    @Serializable
    data class HotTopicDetail(val topicId: Long, val topicName: String): Destination

    @Serializable
    data object Login: Destination

    @Serializable
    data class Notification(
        val type: Int = NotificationsType.ReplyMe.ordinal
    ): Destination

    @Serializable
    data class Reply(
        val forumId: Long,
        val forumName: String,
        val threadId: Long,
        val postId: Long? = null,
        val subPostId: Long? = null,
        val replyUserId: Long? = null,
        val replyUserName: String? = null,
        val replyUserPortrait: String? = null,
        val isDialog: Boolean = false,
    ): Destination

    @Serializable
    data class Report(val postId: Long): Destination

    @Serializable
    data object Search: Destination

    @Serializable
    data class SubPosts(
        val threadId: Long,
        val forumId: Long = 0L,
        val postId: Long = 0L,
        val subPostId: Long = 0L,
        val isSheet: Boolean = true,
    ): Destination

    @Serializable
    data class Thread(
        val threadId: Long,
        val forumId: Long? = null,
        val postId: Long = 0,
        val seeLz: Boolean = false,
        val sortType: Int = 0,
        val from: ThreadFrom? = null,
        val scrollToReply: Boolean = false,
    ): Destination

    @Serializable
    data object ThreadStore: Destination

    @Serializable
    data class UserFollowList(val uid: Long): Destination

    /**
     * @param uid 用户ID
     * @param avatar 用户头像Url
     * @param nickname 昵称
     * @param username 用户名
     * @param transitionKey 过渡动画额外标识键. 确保推荐页, 搜索页中包含多个相同用户时过渡动画的唯一性
     * @param recordHistory 记录访问历史
     * */
    @Serializable
    data class UserProfile(
        val uid: Long,
        val avatar: String? = null,
        val nickname: String? = null,
        val username: String? = null,
        val transitionKey: String? = null,
        val recordHistory: Boolean = true,
    ): Destination {

        constructor(user: Author, transitionKey: String? = null, recordHistory: Boolean = true): this(
            uid = user.id,
            avatar = user.avatarUrl,
            nickname = user.name,
            transitionKey = transitionKey,
            recordHistory = recordHistory
        )

        constructor(user: UserData, transitionKey: String? = null, recordHistory: Boolean = true): this(
            uid = user.id,
            avatar = user.avatarUrl,
            nickname = user.nameShow,
            transitionKey = transitionKey,
            recordHistory = recordHistory
        )
    }

    @Serializable
    data class WebView(val initialUrl: String, val customClient: Boolean = true): Destination

    @Serializable
    object Welcome: Destination

}
