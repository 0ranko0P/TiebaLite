package com.huanchengfly.tieba.post.ui.page.search.user

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.repository.SearchRepository
import com.huanchengfly.tieba.post.repository.SearchResult
import com.huanchengfly.tieba.post.ui.models.search.SearchUser
import com.huanchengfly.tieba.post.ui.page.search.SearchBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Stable
@HiltViewModel
class SearchUserViewModel @Inject constructor(
    private val searchRepo: SearchRepository
) : SearchBaseViewModel<SearchUser>() {

    override suspend fun search(keyword: String): SearchResult<SearchUser> {
        return searchRepo.searchUser(keyword)
    }
}
