package com.huanchengfly.tieba.post.ui.models.user

/**
 * UI Model of [com.huanchengfly.tieba.post.core.network.model.FanListBean.FanUserBean]
 *
 * @param uid 用户 UID
 * @param avatar 头像 URL
 * @param displayName 用户名/昵称
 * @param followTime 成为粉丝的时间(Unix)
 * @param intro 简介
 * @param portrait 用户肖像
 * @param concernType 关注类型
 * */
data class FanUser(
    val uid: Long,
    val avatar: String,
    val displayName: String,
    val followTime: Int,
    val intro: String?,
    val portrait: String,
    @ConcernType val concernType: Int,
)
