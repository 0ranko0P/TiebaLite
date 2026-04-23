package com.huanchengfly.tieba.post.ui.models.settings

import kotlinx.serialization.Serializable

@Serializable
data class BlockBackupMetadata(
    val version: Int,
    val timestamp: Long,
    val forumRuleCount: Int,
    val keywordRuleCount: Int,
    val userRuleCount: Int,
)