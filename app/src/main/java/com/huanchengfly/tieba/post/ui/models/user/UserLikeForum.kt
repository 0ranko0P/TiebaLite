package com.huanchengfly.tieba.post.ui.models.user

class UserLikeForum(
    val id: Long,
    val avatar: String,
    val name: String,
    val levelId: String,
    val levelName: String? = null,
    val slogan: String? = null,
)