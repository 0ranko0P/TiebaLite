package com.huanchengfly.tieba.post.core.network.retrofit.interfaces

import com.huanchengfly.tieba.post.core.network.model.SofireResponse
import kotlinx.coroutines.flow.Flow
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit API declaration for Sofire Network API
 *
 * @author HuanChengFly
 * @since 4.0.0 Beta 1
 */
internal interface SofireApi {
    @POST
//    @FormUrlEncoded
    fun post(
        @Url url: String,
        @Query("skey") skey: String,
        @Body body: RequestBody,
        @HeaderMap headers: Map<String, String>,
    ): Flow<SofireResponse>
}