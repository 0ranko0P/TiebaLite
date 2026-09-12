package com.huanchengfly.tieba.post.ui.models.user

data class EditProfile(
    val nickName: String = "",
    val sex: Int = 0,
    val birthdayShowStatus: Boolean = false,
    val birthdayTime: Long = 0L,
    val intro: String? = null,
)