package com.huanchengfly.tieba.post.core.data.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class BlockBackupMetadata(
    val version: Int,
    val timestamp: Long,
    val forumRuleCount: Int,
    val keywordRuleCount: Int,
    val userRuleCount: Int,
)