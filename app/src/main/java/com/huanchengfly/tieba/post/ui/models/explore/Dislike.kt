package com.huanchengfly.tieba.post.ui.models.explore

import androidx.compose.runtime.Immutable

@Immutable
/*data */class Dislike(
    val id: Int,
    val reason: String,
    val extra: String
) {
    override fun hashCode(): Int = id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return id == (other as Dislike).id
    }

    companion object {

        /**
         * Dislike Type ID: 屏蔽该贴吧
         * */
        const val TYPE_ID_FORUM = 7

        /**
         * Dislike Type ID: 屏蔽该用户
         * */
        const val TYPE_ID_USER = 220
    }
}