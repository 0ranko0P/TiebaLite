package com.huanchengfly.tieba.post.core.network.exception

open class TiebaLocalException(
    override val code: Int,
    msg: String
) : TiebaException(msg) {
    override fun toString(): String {
        return "TiebaLocalException(code=$code, message=$message)"
    }
}