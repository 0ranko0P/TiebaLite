package com.huanchengfly.tieba.post.models

import android.os.Parcelable
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class PhotoViewData(
    val data: LoadPicPageData? = null,
    val picItems: List<PicItem> = persistentListOf(),
    val index: Int = 0,
) : Parcelable

@Serializable
@Parcelize
data class PicItem(
    val picId: String,
    val picIndex: Int,
    val originUrl: String,
    val postId: Long? = null,
) : Parcelable

@Serializable
@Parcelize
data class LoadPicPageData(
    val forumId: Long,
    val forumName: String,
    val seeLz: Boolean,
    val objType: String,
    val picId: String,
    val picIndex: Int,
    val threadId: Long,
    val postId: Long,
    val originUrl: String?,
) : Parcelable