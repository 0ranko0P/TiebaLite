package com.huanchengfly.tieba.post.api.retrofit.exception

import com.huanchengfly.tieba.post.api.Error.ERROR_NOT_LOGGED_IN

class TiebaNotLoggedInException : TiebaLocalException(ERROR_NOT_LOGGED_IN, "") {
    override fun toString(): String {
        return "TiebaNotLoggedInException"
    }
}