package com.huanchengfly.tieba.post.core.network.exception

import java.io.IOException

/**
 * Note: https://github.com/HuanCheng65/TiebaLite/issues/150#issuecomment-1407816656
 * */
class SofireException : IOException {

    constructor() : super()

    constructor(cause: Throwable) : super(cause)

    override val message: String = "连接 [sofire.baidu.com] 失败, 请检查您的广告拦截器"
}