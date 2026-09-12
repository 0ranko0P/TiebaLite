package com.huanchengfly.tieba.post.core.network.source

import android.text.TextUtils
import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.model.SearchForumBean
import com.huanchengfly.tieba.post.core.network.model.SearchThreadBean
import com.huanchengfly.tieba.post.core.network.model.SearchUserBean
import com.huanchengfly.tieba.post.core.network.model.protos.searchSug.SearchSugResponseData
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.commonResponse
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import com.huanchengfly.tieba.post.core.network.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

interface SearchNetworkDataSource {

    suspend fun searchForum(keyword: String): SearchForumBean.DataBean

    suspend fun searchPost(
        keyword: String,
        forumName: String,
        forumId: Long,
        sortType: Int,
        filterType: Int,
        page: Int
    ): SearchThreadBean.DataBean

    suspend fun searchThread(keyword: String, page: Int, sortType: Int): SearchThreadBean.DataBean

    suspend fun searchSuggestions(keyword: String, searchForum: Boolean): SearchSugResponseData

    suspend fun searchUser(keyword: String): SearchUserBean.SearchUserDataBean
}

internal class RetrofitSearchNetworkDataSource @Inject constructor(
    private val tiebaApi: ITiebaApi,
    private val networkMonitor: NetworkMonitor,
): SearchNetworkDataSource {

    override suspend fun searchForum(keyword: String): SearchForumBean.DataBean {
        return tiebaApi
            .searchForumFlow(keyword)
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
            .firstOrThrow()
            .run {
                data ?: throw TiebaApiException(CommonResponse(errorCode ?: -1, errorMsg.orEmpty()))
            }
    }

    override suspend fun searchPost(
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

        return tiebaApi
            .searchPostFlow(keyword, forumName, forumId, sortType, filterType, page)
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
            .firstOrThrow()
            .run {
                if (errorCode == 0) data else throw TiebaApiException(CommonResponse(errorCode, errorMsg))
            }
    }

    override suspend fun searchThread(keyword: String, page: Int, sortType: Int): SearchThreadBean.DataBean {
        return tiebaApi
            .searchThreadFlow(keyword, page, sortType)
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
            .firstOrThrow()
            .run {
                if (errorCode == 0) data else throw TiebaApiException(CommonResponse(errorCode, errorMsg))
            }
    }

    override suspend fun searchSuggestions(keyword: String, searchForum: Boolean): SearchSugResponseData {
        return tiebaApi
            .searchSuggestionsFlow(keyword, searchForum)
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = this.error.commonResponse)
            }
    }

    override suspend fun searchUser(keyword: String): SearchUserBean.SearchUserDataBean {
        return tiebaApi
            .searchUserFlow(keyword)
            .catch { throw ConnectivityInterceptor.wrapException(networkMonitor, it) }
            .firstOrThrow()
            .run {
                data ?: throw TiebaApiException(CommonResponse(errorCode ?: -1, errorMsg.orEmpty()))
            }
    }
}