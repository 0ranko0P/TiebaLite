package com.huanchengfly.tieba.post.core.network.source

import android.content.Context
import android.net.Uri
import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.model.AddThreadBean
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.model.MessageListBean
import com.huanchengfly.tieba.post.core.network.model.UploadPictureResultBean
import com.huanchengfly.tieba.post.core.network.model.protos.addPost.AddPostResponseData
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import com.huanchengfly.tieba.post.core.network.util.ImageUploader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ReplyNetworkDataSource {

    /**
     * 发帖
     * @param content 帖子内容
     * @param forumId 吧id
     * @param forumName 吧名
     * @param title 标题(无标题是null/留空)
     */
    suspend fun addThread(
        content: String,
        forumId: Long,
        forumName: String,
        title: String?,
    ): AddThreadBean

    /**
     * 回贴（App 接口）
     *
     * @param content 回复内容
     * @param forumId 吧 ID（不可为null，0）
     * @param forumName 吧名
     * @param threadId 贴子 ID
     * @param tbs tbs
     * @param postId 回复楼 ID，为空则回复贴子
     * @param subPostId 回复楼中楼 ID
     * @param replyUserId 楼中楼回复用户 ID
     */
    suspend fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String,
        postId: Long?,
        subPostId: Long?,
        replyUserId: Long?,
    ): AddPostResponseData

    /**
     * 提到我的消息列表
     *
     * @param page 分页页码（从 1 开始）
     */
    fun atMeFlow(page: Int = 1): Flow<MessageListBean>

    /**
     * 回复我的消息列表
     *
     * @param page 分页页码（从 1 开始）
     */
    fun replyMeFlow(page: Int = 1): Flow<MessageListBean>

    /**
     * 上传图片
     *
     * @param forumName 吧名
     * @param images 图片Uri
     * @param watermarkType 图片上传水印, see com.huanchengfly.tieba.post.core.data.model.settings.WaterType
     * @param isOriginImage 上传原图
     * */
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

    override suspend fun addThread(
        content: String,
        forumId: Long,
        forumName: String,
        title: String?,
    ): AddThreadBean {
        require(forumId > 0) { "Illegal Forum ID: $forumId" }

        return tiebaApi.addThreadFlow(
            threadContent = content,
            kw = forumName,
            fid = forumId.toString(),
            title = title.orEmpty(),
            isHide = 1,
            isTitle = if (title.isNullOrEmpty()) 1 else 0
        )
        .firstOrThrow()
        .apply {
            if (errorCode != "0") {
                throw TiebaApiException(CommonResponse(errorCode?.toIntOrNull() ?: -1, errorMsg.orEmpty()))
            }
            requireNotNull(tid) { "Server returned null ThreadID"}
        }
    }

    override suspend fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String,
        postId: Long?,
        subPostId: Long?,
        replyUserId: Long?
    ): AddPostResponseData {
        return tiebaApi.addPostFlow(
            content = content,
            forumId = forumId.toString(),
            forumName = forumName,
            threadId = threadId.toString(),
            tbs = tbs,
            nameShow = null,
            postId = postId?.toString(),
            subPostId = subPostId?.toString(),
            replyUserId = replyUserId?.toString()
        )
        .firstOrThrow()
        .data_!!
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