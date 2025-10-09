package com.huanchengfly.tieba.post.ui.models.search

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.protos.searchSug.SearchSugResponseData

/**
 * UI Model of [SearchSugResponseData]
 * */
@Immutable
class SearchSuggestion(
    val forum: SearchForum?,
    val suggestions: List<String>
)