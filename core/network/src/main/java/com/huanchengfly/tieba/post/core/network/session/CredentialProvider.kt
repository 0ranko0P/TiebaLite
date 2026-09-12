package com.huanchengfly.tieba.post.core.network.session

import com.huanchengfly.tieba.post.core.network.exception.TiebaNotLoggedInException

interface CredentialProvider {

    fun getBduss(): String?

    fun getCookie(): String?

    fun getNickname(): String?

    fun getSToken(): String?

    fun getTbs() : String?

    fun getUid(): String?

    fun getZid(): String?

    fun isLoggedIn(): Boolean

    fun requireUid(): Long

    fun requireTbs(): String {
        return getTbs() ?: throw TiebaNotLoggedInException()
    }
}