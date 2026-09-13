package com.huanchengfly.tieba.post.core.data.model.settings

/**
 * 隐私设置
 *
 * @param readClipBoardLink 读取并打开剪贴板中的贴吧链接
 * */
data class PrivacySettings(
    val readClipBoardLink: Boolean = true
)