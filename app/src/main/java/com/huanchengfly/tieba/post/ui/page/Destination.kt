package com.huanchengfly.tieba.post.ui.page

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.huanchengfly.tieba.post.ui.page.forum.detail.ManagerData
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface Destination {

    @Serializable
    data object Main: Destination

    @Serializable
    data object AppTheme: Destination

    @Serializable
    data object History: Destination

    @Serializable
    data object HotTopicList: Destination

    @Serializable
    data object Notification: Destination

    @Serializable
    data object Login: Destination

    @Serializable
    data object Search: Destination

    @Serializable
    data class Forum(val forumName: String): Destination

    @Serializable
    data class ForumDetail(val params: ForumDetailParams): Destination

    @Serializable
    data class ForumSearchPost(val forumName: String, val forumId: Long): Destination

    @Serializable
    data class ForumRuleDetail(val forumId: Long): Destination

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
    data class SubPosts(
        val threadId: Long,
        val forumId: Long = 0L,
        val postId: Long = 0L,
        val subPostId: Long = 0L
    ): Destination

    @Serializable
    data class CopyText(val text: String): Destination

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
        val tbs: String? = null,
        val isDialog: Boolean = false,
    ): Destination

    @Serializable
    data class UserProfile(val uid: Long): Destination

    @Serializable
    data class WebView(val initialUrl: String): Destination

    @Serializable
    data object Settings: Destination

    companion object {

        @Serializable
        data class ForumDetailParams(
            val forumId: Long,
            val avatar: String,
            val name: String,
            val slogan: String,
            val memberCount: Int,
            val threadCount: Int,
            val postCount: Int,
            val managers: List<ManagerData>
        )

        inline fun <reified T> navTypeOf(
            isNullableAllowed: Boolean = false,
            json: Json = Json
        ) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {

            override fun get(bundle: Bundle, key: String): T? {
                return bundle.getString(key)?.let(json::decodeFromString)
            }

            override fun put(bundle: Bundle, key: String, value: T) {
                bundle.putString(key, json.encodeToString(value))
            }

            override fun parseValue(value: String): T = json.decodeFromString(Uri.decode(value))

            override fun serializeAsValue(value: T): String = Uri.encode(json.encodeToString(value))
        }
    }
}
