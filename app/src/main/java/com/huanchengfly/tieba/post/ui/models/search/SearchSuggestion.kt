package com.huanchengfly.tieba.post.ui.models.search

import androidx.compose.runtime.Immutable

/**
 * UI Model of [com.huanchengfly.tieba.post.core.network.model.protos.searchSug.SearchSugResponseData]
 * */
@Immutable
class SearchSuggestion(
    val forum: SearchForum?,
    val suggestions: List<String>
)