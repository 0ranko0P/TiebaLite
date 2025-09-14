package com.huanchengfly.tieba.post.ui.models.forum

import androidx.compose.runtime.Immutable

@Immutable
class Rule(
    val title: String,
    val content: String?,
)

@Immutable
class ForumRule(
    val headLine: String,
    val publishTime: String?,
    val preface: String,
    val data: List<Rule>,
    val author: ForumManager?
)