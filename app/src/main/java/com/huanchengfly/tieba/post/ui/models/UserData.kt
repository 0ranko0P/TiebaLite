package com.huanchengfly.tieba.post.ui.models

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.utils.StringUtil

/**
 * Represents [User] in UI
 * */
data class UserData(
    val id: Long,
    val annotatedName: AnnotatedString,
    val name: String,
    val nameShow: String,
    val avatarUrl: String,
    val portrait: String,
    val ip: String,
    val levelId: Int,
    val bawuType: String?
) {
    constructor(user: User, isLz: Boolean) : this(
        user.id,
        getAnnotatedString(user, isLz),
        user.name,
        user.nameShow,
        StringUtil.getAvatarUrl(user.portrait),
        user.portrait,
        user.ip_address,
        user.level_id,
        user.bawu_type
    )

    companion object {

        val Empty = UserData(-1, AnnotatedString("?"), "?", "bug", "", "", "Void", 2, null)

        private fun getAnnotatedString(user: User, isLz: Boolean): AnnotatedString =
            buildAnnotatedString {
                val levelId = user.level_id
                val bawu = user.bawu_type
                append(StringUtil.getUsernameAnnotatedString(App.INSTANCE, user.name, user.nameShow))
                append(" ")
                if (levelId > 0) appendInlineContent("Level", alternateText = "$levelId")
                if (bawu.isNotBlank()) {
                    append(" ")
                    appendInlineContent("Bawu", alternateText = bawu)
                }
                if (isLz) {
                    append(" ")
                    appendInlineContent("Lz")
                }
            }
    }
}