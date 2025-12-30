package com.huanchengfly.tieba.post.ui.page.user

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import java.util.Objects

/** 用户头像过渡动画唯一标识键 */
@Immutable
@JvmInline
value class UserAvatarSharedBoundsKey(val value: Int) {

    /**
     * @param uid 用户ID
     * @param extraKey 额外标识键. 确保推荐页, 搜索页中包含多个相同用户时过渡动画的唯一性
     * */
    constructor(uid: Long, extraKey: Any?): this(Objects.hash(uid, extraKey?.toString()))
}

/** 用户昵称过渡动画唯一标识键 */
@Immutable
@JvmInline
value class UserNicknameSharedBoundsKey(val value: Int) {

    /**
     * @param nickname 用户昵称
     * @param extraKey 额外标识键. 确保推荐页, 搜索页中包含多个相同用户时过渡动画的唯一性
     * */
    constructor(nickname: String, extraKey: Any?): this(Objects.hash(nickname, extraKey?.toString()))
}

/** 用户名过渡动画唯一标识键 */
@Immutable
@JvmInline
value class UsernameSharedBoundsKey(val value: Int) {

    /**
     * @param name 用户名
     * @param extraKey 额外标识键. 确保推荐页, 搜索页中包含多个相同用户时过渡动画的唯一性
     * */
    constructor(name: String, extraKey: Any?): this(Objects.hash(name, extraKey?.toString()))
}

fun Modifier.sharedUserAvatar(
    uid: Long,
    extraKey: Any? = null
) = this.localSharedBounds(
    key = UserAvatarSharedBoundsKey(uid = uid, extraKey = extraKey)
)

fun Modifier.sharedUserNickname(
    nickname: String,
    extraKey: Any? = null
) = this.localSharedBounds(
    key = UserNicknameSharedBoundsKey(nickname, extraKey = extraKey),
    boundsTransform = NameBoundsTransform,
)

fun Modifier.sharedUsername(
    username: String,
    extraKey: Any? = null
) = this.localSharedBounds(
    key = UsernameSharedBoundsKey(username, extraKey = extraKey),
    boundsTransform = NameBoundsTransform,
)

private val NameBoundsTransform = BoundsTransform { _, _ -> DefaultTextSpring }

private val DefaultTextSpring =
    spring(stiffness = Spring.StiffnessLow, visibilityThreshold = Rect.VisibilityThreshold)
