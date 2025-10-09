package com.huanchengfly.tieba.post.repository.source.network

import android.text.TextUtils
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.SearchForumBean
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.api.models.SearchUserBean
import com.huanchengfly.tieba.post.api.models.protos.searchSug.SearchSugResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.repository.source.network.ExploreNetworkDataSource.commonResponse
import kotlinx.coroutines.flow.catch

object SearchNetworkDataSource {

    suspend fun searchForum(keyword: String): SearchForumBean.DataBean {
        return TiebaApi.getInstance()
            .searchForumFlow(keyword)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                data ?: throw TiebaApiException(CommonResponse(errorCode ?: -1, errorMsg.orEmpty()))
            }
    }

    suspend fun searchPost(
        keyword: String,
        forumName: String,
        forumId: Long,
        sortType: Int,
        filterType: Int,
        page: Int
    ): SearchThreadBean.DataBean {
        if (TextUtils.isEmpty(keyword)) throw IllegalArgumentException("Empty keyword")
        if (page < 1) throw IllegalArgumentException("Invalid page number: $page")
        if (sortType !in 1..2) throw IllegalArgumentException("Invalid sort type: $sortType")
        if (filterType !in 1..2) throw IllegalArgumentException("Invalid filter type: $filterType")

        return TiebaApi.getInstance()
            .searchPostFlow(keyword, forumName, forumId, sortType, filterType, page)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                if (errorCode == 0) data else throw TiebaApiException(CommonResponse(errorCode, errorMsg))
            }
    }

    suspend fun searchThread(keyword: String, page: Int, sortType: Int): SearchThreadBean.DataBean {
        return TiebaApi.getInstance()
            .searchThreadFlow(keyword, page, sortType)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                if (errorCode == 0) data else throw TiebaApiException(CommonResponse(errorCode, errorMsg))
            }
    }

    suspend fun searchSuggestions(keyword: String, searchForum: Boolean): SearchSugResponseData {
        return TiebaApi.getInstance()
            .searchSuggestionsFlow(keyword, searchForum)
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = this.error.commonResponse)
            }
    }

    suspend fun searchUser(keyword: String): SearchUserBean.SearchUserDataBean {
        return TiebaApi.getInstance()
            .searchUserFlow(keyword)
            .catch { throw ConnectivityInterceptor.wrapException(it) }
            .firstOrThrow()
            .run {
                data ?: throw TiebaApiException(CommonResponse(errorCode ?: -1, errorMsg.orEmpty()))
            }
    }
}