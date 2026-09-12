package com.huanchengfly.tieba.post.core.network.exception

class UploadPictureFailedException(
    override val code: Int = -1,
    override val message: String = "上传图片失败",
) : TiebaException(message)