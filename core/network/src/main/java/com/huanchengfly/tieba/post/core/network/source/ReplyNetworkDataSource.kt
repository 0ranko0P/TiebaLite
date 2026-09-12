package com.huanchengfly.tieba.post.core.network.source

import android.content.Context
import android.net.Uri
import com.huanchengfly.tieba.post.core.network.model.AddThreadBean
import com.huanchengfly.tieba.post.core.network.model.MessageListBean
import com.huanchengfly.tieba.post.core.network.model.UploadPictureResultBean
import com.huanchengfly.tieba.post.core.network.model.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import com.huanchengfly.tieba.post.core.network.util.ImageUploader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ReplyNetworkDataSource {

    fun addThread(
        content: String,
        forumId: Long,
        forumName: String,
        title: String?,
        isHide: Int?,
        isTitle: Int?
    ): Flow<AddThreadBean>

    fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String?,
        nameShow: String?,
        postId: Long?,
        subPostId: Long?,
        replyUserId: Long?,
    ): Flow<AddPostResponse>

    fun atMeFlow(page: Int = 1): Flow<MessageListBean>

    fun replyMeFlow(page: Int = 1): Flow<MessageListBean>

    suspend fun upload(
        forumName: String,
        images: List<Uri>,
        watermarkType: Int,
        isOriginImage: Boolean = false
    ): List<UploadPictureResultBean>
}

internal class RetrofitReplyNetworkDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clientConfigProvider: ClientConfigProvider,
    private val tiebaApi: ITiebaApi,
    private val retrofitTiebaApi: RetrofitTiebaApi,
) : ReplyNetworkDataSource {

    override fun addThread(
        content: String,
        forumId: Long,
        forumName: String,
        title: String?,
        isHide: Int?,
        isTitle: Int?
    ): Flow<AddThreadBean> {
        return tiebaApi.addThreadFlow(
            content,
            forumName,
            forumId.toString(),
            title.orEmpty(),
            requireNotNull(isHide),
            requireNotNull(isTitle)
        )
    }

    override fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String?,
        nameShow: String?,
        postId: Long?,
        subPostId: Long?,
        replyUserId: Long?
    ): Flow<AddPostResponse> {
        return tiebaApi.addPostFlow(
            content,
            forumId.toString(),
            forumName,
            threadId.toString(),
            tbs,
            nameShow,
            postId?.toString(),
            subPostId?.toString(),
            replyUserId?.toString()
        )
    }

    override fun atMeFlow(page: Int): Flow<MessageListBean> {
        return tiebaApi.atMeFlow(page)
    }

    override fun replyMeFlow(page: Int): Flow<MessageListBean> {
        return tiebaApi.replyMeFlow(page)
    }

    override suspend fun upload(
        forumName: String,
        images: List<Uri>,
        watermarkType: Int,
        isOriginImage: Boolean
    ): List<UploadPictureResultBean> {
        return ImageUploader(clientConfigProvider, forumName, tiebaApi = retrofitTiebaApi.OFFICIAL_TIEBA_API)
            .upload(context, images, watermarkType, isOriginImage)
    }

}