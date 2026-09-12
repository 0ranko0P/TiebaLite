package com.huanchengfly.tieba.post.core.network.retrofit.interfaces

import com.huanchengfly.tieba.post.core.network.Param
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.model.SearchForumBean
import com.huanchengfly.tieba.post.core.network.model.SearchThreadBean
import com.huanchengfly.tieba.post.core.network.model.SearchUserBean
import com.huanchengfly.tieba.post.core.network.model.TopicDetailBean
import com.huanchengfly.tieba.post.core.common.ktx.urlEncode
import kotlinx.coroutines.flow.Flow
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query
import com.huanchengfly.tieba.post.core.network.Header as TiebaHeaders

/**
 * Retrofit API declaration for Hybrid Tieba Network API
 *
 * @author HuanChengFly
 * @since 4.0.0 Alpha 16
 */
internal interface AppHybridTiebaApi {
    @Headers(
        "${TiebaHeaders.NO_ST_PARAMS}: ${TiebaHeaders.NO_ST_PARAMS_TRUE}",
        "${TiebaHeaders.NO_COMMON_PARAMS}: ${Param.BDUSS},${Param.STOKEN}",
    )
    @GET("/mo/q/search/forum")
    fun searchForumFlow(
        @Query("word") keyword: String,
        @Header("Referer") referer: String = "https://tieba.baidu.com/mo/q/hybrid/search?keyword=$keyword&_webview_time=${System.currentTimeMillis()}".urlEncode(),
    ): Flow<SearchForumBean>

    @Headers(
        "${TiebaHeaders.NO_ST_PARAMS}: ${TiebaHeaders.NO_ST_PARAMS_TRUE}",
        "${TiebaHeaders.NO_COMMON_PARAMS}: ${Param.BDUSS},${Param.STOKEN}",
    )
    @GET("/mo/q/search/thread")
    fun searchThreadFlow(
        @Query("word") keyword: String,
        @Query("pn") page: Int,
        @Query("st") sort: Int,
        @Query("tt") filter: Int = 1,
        @Query("rn") pageSize: Int? = null,
        @Query("fname") forumName: String? = null,
        @Query("ct") ct: Int = 1,
        @Query("is_use_zonghe") isUseZonghe: Int? = 1,
        @Query("cv") clientVersion: String = "99.9.101",
        @Header("Referer") referer: String = "https://tieba.baidu.com/mo/q/hybrid/search?keyword=$keyword&_webview_time=${System.currentTimeMillis()}".urlEncode(),
    ): Flow<SearchThreadBean>

    @Headers(
        "${TiebaHeaders.NO_ST_PARAMS}: ${TiebaHeaders.NO_ST_PARAMS_TRUE}",
        "${TiebaHeaders.NO_COMMON_PARAMS}: ${Param.BDUSS},${Param.STOKEN}",
    )
    @GET("/mo/q/search/user")
    fun searchUserFlow(
        @Query("word") keyword: String,
        @Header("Referer") referer: String = "https://tieba.baidu.com/mo/q/hybrid/search?keyword=$keyword&_webview_time=${System.currentTimeMillis()}".urlEncode(),
    ): Flow<SearchUserBean>

    @GET("/mo/q/newtopic/topicDetail")
    fun topicDetailFlow(
        @Query("topic_id") topicId: String,
        @Query("topic_name") topicName: String,
        @Query("is_new") isNew: Int = 1,
        @Query("is_share") isShare: Int = 1,
        @Query("pn") page: Int = 1,
        @Query("rn") pageSize: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("last_id") lastId: String = "",
        @Query("derivative_to_pic_id") derivativeToPicId: String = "",
    ): Flow<TopicDetailBean>

    @GET("/c/c/post/addPollPost")
    fun addPollPost(
        @Query("forum_id") forumId: Long?,
        @Query("thread_id") threadId: Long,
        @Query("options") options: String,
    ): Flow<CommonResponse>
}