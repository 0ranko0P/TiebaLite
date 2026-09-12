package com.huanchengfly.tieba.post.core.network.model.web

import com.huanchengfly.tieba.post.core.common.ktx.toJson

open class BaseBean {
    override fun toString(): String {
        return toJson()
    }
}