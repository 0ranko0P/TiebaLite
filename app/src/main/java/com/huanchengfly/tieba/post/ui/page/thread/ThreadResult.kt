package com.huanchengfly.tieba.post.ui.page.thread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ThreadResult(
    val threadId: Long,
    val liked: Boolean,
    val likes: Long,
    val markedPostId: Long?
) : Parcelable
