package com.huanchengfly.tieba.post.ui.models.user

import androidx.annotation.IntDef

@IntDef(ConcernType.NONE, ConcernType.FOLLOWING, ConcernType.MUTUAL, ConcernType.FANS, ConcernType.UPDATING)
@Retention(AnnotationRetention.SOURCE)
annotation class ConcernType {
    companion object {
        /** 未关注 */
        const val NONE = 0
        /** 关注 */
        const val FOLLOWING = 1
        /** 互相关注 */
        const val MUTUAL = 2

        /** 是粉丝但未关注 */
        const val FANS = -9
        /** 正在向服务器请求关注状态更新 */
        const val UPDATING = Int.MAX_VALUE
    }
}

/**
 * UI Model of [com.huanchengfly.tieba.post.core.network.model.FollowListBean.FollowUserBean]
 * */
data class FollowUser(
    val uid: Long,
    val avatar: String,
    val displayName: String,
    val portrait: String,
    val intro: String?,
    @ConcernType val concernType: Int,
)