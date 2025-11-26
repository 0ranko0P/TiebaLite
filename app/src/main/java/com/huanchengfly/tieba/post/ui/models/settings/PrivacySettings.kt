package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable

/**
 * 隐私设置
 *
 * @param readClipBoardLink 读取并打开剪贴板中的贴吧链接
 * */
@Immutable
data class PrivacySettings(
    val readClipBoardLink: Boolean = true
)