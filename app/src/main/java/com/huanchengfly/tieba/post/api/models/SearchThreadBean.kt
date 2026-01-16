package com.huanchengfly.tieba.post.api.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Immutable
@Serializable
data class SearchThreadBean(
    @SerialName("no")
    val errorCode: Int,
    @SerialName("error")
    val errorMsg: String,
    val data: DataBean,
) {
    @Immutable
    @Serializable
    data class DataBean(
        @SerialName("has_more")
        val hasMore: Int,
        @SerialName("current_page")
        val currentPage: Int,
        @SerialName("post_list")
        val postList: List<ThreadInfoBean> = emptyList(),
    )

    /**
     * Note: Server returns formatted Tieba short number, handle carefully.
     *
     * @see [com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString]
     * @see [com.huanchengfly.tieba.post.utils.StringUtil.tiebaNumToLong]
     * */
    @Immutable
    @Serializable
    data class ThreadInfoBean(
        val tid: Long,
        val pid: Long,
        val cid: Long,
        val title: String,
        val content: String,
        val time: Long,
        @SerialName("modified_time")
        val modifiedTime: Long,
        @SerialName("post_num")
        val postNum: String,
        @SerialName("like_num")
        val likeNum: String,
        @SerialName("share_num")
        val shareNum: String,
        @SerialName("forum_id")
        val forumId: Long,
        @SerialName("forum_name")
        val forumName: String,
        val user: UserInfoBean,
        val type: Int,
        @SerialName("forum_info")
        val forumInfo: ForumInfo,
        val media: List<MediaInfo>? = null,
        @SerialName("main_post")
        val mainPost: MainPost? = null,
        @SerialName("post_info")
        val postInfo: PostInfo? = null,
    )

    @Immutable
    @Serializable
    data class MediaInfo(
        val type: String,
        val size: String? = null,
        val width: Int?,  // 一些古老视频的尺寸为 null, 一些古老图片的尺寸为 0
        val height: Int?,
        @SerialName("water_pic")
        val waterPic: String? = null,
        @SerialName("small_pic")
        val smallPic: String? = null,
        @SerialName("big_pic")
        val bigPic: String? = null,
        val src: String? = null,
        val vsrc: String? = null,
        val vhsrc: String? = null,
        val vpic: String? = null,
    ) {
        companion object {
            const val TYPE_PICTURE = "pic"
            const val TYPE_VIDEO = "flash"
        }
    }

    @Immutable
    @Serializable
    data class MainPost(
        val title: String,
        val content: String,
        val tid: Long,
        val user: UserInfoBean,
        @SerialName("like_num")
        val likeNum: String,
        @SerialName("share_num")
        val shareNum: String,
        @SerialName("post_num")
        val postNum: String,
    )

    @Immutable
    @Serializable
    data class PostInfo(
        val tid: Long,
        val pid: Long,
        val title: String,
        val content: String,
        val user: UserInfoBean,
    )

    @Immutable
    @Serializable
    data class ForumInfo(
        @SerialName("forum_name")
        val forumName: String,
        val avatar: String,
    )

    @Immutable
    @Serializable(UserInfoSerializer::class)
    data class UserInfoBean(
        val userName: String,
        val showNickname: String,
        val userId: Long,
        val portrait: String,
    )

    // vip info of UserInfoBean, for serialization use only
    @Immutable
    @Serializable
    data class VipInfo(
        @SerialName("v_level")
        val level: Int,
    )
}

val NullUserInfo by lazy {
    SearchThreadBean.UserInfoBean("用户已注销", "", -1, "")
}

/**
 * KSerializer of [SearchThreadBean.UserInfoBean]
 *
 * Baidu returns 'valid' user if it's deregistered -> "user": { "user_name":null, "user_id":null ... }
 *
 * in this condition, [NullUserInfo] will be used as decode result to avoid null checks everywhere in UI
 * */
@OptIn(ExperimentalSerializationApi::class)
private object UserInfoSerializer: KSerializer<SearchThreadBean.UserInfoBean> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SearchThreadBean.UserInfoBean") {
        element<String?>("user_name")
        element<Long?>("user_id")
        element<String>("portrait")
        element<String?>("portraith")
        element<SearchThreadBean.VipInfo?>("vipInfo")
        element<String>("show_nickname")
    }

    override fun deserialize(decoder: Decoder): SearchThreadBean.UserInfoBean = decoder.decodeStructure(descriptor) {
        var userName: String? = null
        var userId: Long? = null
        var portrait = ""
        var showNickname = ""

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                DECODE_DONE -> break
                0 -> userName = decodeNullableSerializableElement(descriptor, index, String.serializer())
                1 -> userId = decodeNullableSerializableElement(descriptor, index, Long.serializer())
                2 -> portrait = decodeStringElement(descriptor, index)
                3 -> decodeStringElement(descriptor, index)
                4 -> decodeNullableSerializableElement(descriptor, index, SearchThreadBean.VipInfo.serializer())
                5 -> showNickname = decodeStringElement(descriptor, index)
                else -> error("Unexpected index: $index")
            }
        }
        if (userName == null || userId == null) {
            NullUserInfo // 用户已注销
        } else {
            SearchThreadBean.UserInfoBean(userName, showNickname, userId, portrait)
        }
    }

    override fun serialize(encoder: Encoder, value: SearchThreadBean.UserInfoBean) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.userName)
        encodeLongElement(descriptor, 1, value.userId)
        encodeStringElement(descriptor, 2, value.portrait)
        encodeNullableSerializableElement(descriptor, 3, String.serializer(), null)
        encodeNullableSerializableElement(descriptor, 4, SearchThreadBean.VipInfo.serializer(), null)
        encodeStringElement(descriptor, 5, value.showNickname)
    }
}
