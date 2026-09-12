package com.huanchengfly.tieba.post.core.network.exception

import com.huanchengfly.tieba.post.core.network.model.MSignBean

class TiebaMSignException(
    error: MSignBean.Error,
    val signNotice: String,
    override val code: Int = error.errno
) : TiebaException(message = error.usermsg)