package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.api.adapters.StringToBooleanAdapter
import com.huanchengfly.tieba.post.models.BaseBean
import com.huanchengfly.tieba.post.utils.ImageUtil

data class PicPageBean(
    @SerializedName("error_code")
    val errorCode: String,
    val forum: ForumBean,
    @SerializedName("pic_amount")
    val picAmount: Int?, // 远古坟贴: Null
    @SerializedName("pic_list")
    val picList: List<PicBean>,
) : BaseBean() {
    data class ForumBean(
        val name: String,
        val id: String
    )

    data class PicBean(
        @SerializedName("overall_index")
        val overAllIndex: String,
        @SerializedName("is_long_pic")
        @JsonAdapter(StringToBooleanAdapter::class)
        val isLongPic: Boolean,
        @SerializedName("show_original_btn")
        @JsonAdapter(StringToBooleanAdapter::class)
        val showOriginalBtn: Boolean,
        @SerializedName("is_blocked_pic")
        @JsonAdapter(StringToBooleanAdapter::class)
        val isBlockedPic: Boolean,
        val img: ImgBean,
        @SerializedName("post_id")
        val postId: String?,
        @SerializedName("user_id")
        val userId: String?,
        @SerializedName("user_name")
        val userName: String?,
    )

    data class ImgBean(
        val original: ImgInfoBean,
        val medium: ImgInfoBean?,
        val screen: ImgInfoBean?,
    )

    data class ImgInfoBean(
        val id: String,
        val width: String?,
        val height: String?,
        val size: String,
        val format: String,
        @SerializedName("waterurl")
        val waterUrl: String,
        @SerializedName("big_cdn_src")
        val bigCdnSrc: String,
        val url: String,
        @SerializedName("original_src")
        val originalSrc: String,
    )
}

// Do not use PicBean#isLongPic, check manually here
fun PicPageBean.ImgInfoBean.isLongPic(): Boolean {
    return runCatching { ImageUtil.isLongImg(width!!.toInt(), height!!.toInt()) }.getOrDefault(false)
}

val PicPageBean.ImgBean.isGif: Boolean
    get() = original.format == "2"

val PicPageBean.ImgBean.bestQualitySrc: String
    get() = with(this.original) {
        if (isGif) return@with originalSrc

        val size = original.size.toLongOrNull() ?: 0

        return if (size >= 1024 * 1024 * 2 && isLongPic()) {
            // Quality: bigCdnSrc > waterUrl = url = originalSrc
            this.bigCdnSrc
        } else if (size > 1024 * 1024 * 1) {
            // Quality: waterUrl = url = originalSrc > bigCdnSrc
            this.originalSrc
        } else {
            // Quality: waterUrl = bigCdnSrc = url = originalSrc
            this.originalSrc
        }
    }