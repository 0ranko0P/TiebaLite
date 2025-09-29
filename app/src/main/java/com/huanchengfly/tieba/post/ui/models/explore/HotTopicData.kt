package com.huanchengfly.tieba.post.ui.models.explore

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.ui.models.ThreadItem

// topicName, tag
typealias RecommendTopic = Pair<String, Int>

@Stable
data class HotTab(val name: String, val tabCode: String, var isLoading: Boolean = false)

class HotTopicData(
    val topics: List<RecommendTopic>,
    val tabs: List<HotTab>,
    val threads: List<ThreadItem>,
)