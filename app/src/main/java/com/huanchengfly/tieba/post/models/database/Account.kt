package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import org.litepal.crud.LitePalSupport

@Immutable
data class Account @JvmOverloads constructor(
    val uid: String = "",
    val name: String = "",
    val bduss: String = "",
    val tbs: String = "",
    val portrait: String = "",
    val sToken: String = "",
    val cookie: String = "",
    val nameShow: String? = null,
    val intro: String? = null,
    val sex: String? = null,
    val fansNum: String? = null,
    val postNum: String? = null,
    val threadNum: String? = null,
    val concernNum: String? = null,
    val tbAge: String? = null,
    val age: String? = null,
    val birthdayShowStatus: String? = null,
    val birthdayTime: String? = null,
    val constellation: String? = null,
    val tiebaUid: String? = null,
    val loadSuccess: Boolean = false,
    val uuid: String? = "",
    val zid: String? = "",
) : LitePalSupport() {
    val id: Int = 0
}