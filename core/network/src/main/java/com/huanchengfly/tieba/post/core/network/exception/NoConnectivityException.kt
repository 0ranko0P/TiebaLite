package com.huanchengfly.tieba.post.core.network.exception

import com.huanchengfly.tieba.post.core.network.Error.ERROR_NETWORK

class NoConnectivityException(
    msg: String = "No internet!"
) : TiebaLocalException(ERROR_NETWORK, msg) {
    override fun toString(): String {
        return "NoConnectivityException(message=$message)"
    }
}