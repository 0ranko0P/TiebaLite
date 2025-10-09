package com.huanchengfly.tieba.post.ui.page.search.forum

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.repository.SearchRepository
import com.huanchengfly.tieba.post.ui.models.search.SearchForum
import com.huanchengfly.tieba.post.ui.page.search.SearchBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Stable
@HiltViewModel
class SearchForumViewModel @Inject constructor(
    private val searchRepo: SearchRepository
) : SearchBaseViewModel<SearchForum>() {

    override suspend fun search(keyword: String): Pair<SearchForum?, List<SearchForum>> { // ExactMatch, FuzzyMatch
        return searchRepo.searchForum(keyword)
    }
}
