package com.huanchengfly.tieba.post.ui.page

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import kotlinx.serialization.Serializable
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
    data class Notification(
        val type: Int = NotificationsType.ReplyMe.ordinal
    ): Destination

    @Serializable
    data object Login: Destination

    @Serializable
    data object Search: Destination

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
    object Welcome: Destination

    @Serializable
    data object Settings: Destination

    companion object {

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
        inline fun <reified T : Parcelable> parcelableListType(
            isNullableAllowed: Boolean = false,
            json: Json = Json,
        ) = object : NavType<List<T>>(isNullableAllowed = isNullableAllowed) {
            override fun get(bundle: Bundle, key: String): List<T>? {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bundle.getParcelableArrayList(key, T::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    bundle.getParcelableArrayList(key)
                }
            }

            override fun parseValue(value: String): List<T> = json.decodeFromString(Uri.decode(value))

            override fun serializeAsValue(value: List<T>): String = Uri.encode(json.encodeToString(value))

            override fun put(bundle: Bundle, key: String, value: List<T>) {
                bundle.putParcelableArrayList(key, value as? ArrayList ?: ArrayList(value))
            }
        }
    }
}
