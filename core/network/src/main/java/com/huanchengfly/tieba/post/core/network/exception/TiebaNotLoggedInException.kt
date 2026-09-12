package com.huanchengfly.tieba.post.core.network.exception

import com.huanchengfly.tieba.post.core.network.Error.ERROR_NOT_LOGGED_IN

class TiebaNotLoggedInException : TiebaLocalException(ERROR_NOT_LOGGED_IN, "") {
    override fun toString(): String {
        return "TiebaNotLoggedInException"
    }
}