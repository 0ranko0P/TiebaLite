package com.huanchengfly.tieba.post.api.retrofit.exception

import com.huanchengfly.tieba.post.api.models.MSignBean

class TiebaMSignException(
    error: MSignBean.Error,
    val signNotice: String,
    override val code: Int = error.errno
) : TiebaException(message = error.usermsg)