package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.models.protos.Agree

/**
 * Represents [Agree] in UI
 *
 * @param liked remapped [Agree.hasAgree] from Int to Boolean
 * @param count [Agree.agreeNum]
 * */
@Stable
data class Like(val liked: Boolean, val count: Long) {

    @Volatile
    var loading: Boolean = false
        private set

    constructor(agree: Agree): this(liked = agree.hasAgree == 1, count = agree.agreeNum)

    fun updateLikeStatus(liked: Boolean): Like {
        return if (this.liked xor liked) {
            copy(liked = liked, count = if (liked) count + 1 else count -1)
        } else {
            this
        }
    }

    fun setLoading(loading: Boolean): Like = synchronized(this) {
        this.loading = loading
        return@synchronized this
    }

    operator fun not(): Like = updateLikeStatus(!liked)
}

val LikeZero: Like = Like(false, count = 0)