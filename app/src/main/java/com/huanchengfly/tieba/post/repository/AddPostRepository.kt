package com.huanchengfly.tieba.post.repository

import android.net.Uri
import com.huanchengfly.tieba.post.core.network.model.AddThreadBean
import com.huanchengfly.tieba.post.core.network.model.UploadPictureResultBean
import com.huanchengfly.tieba.post.core.network.model.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.core.network.source.ReplyNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AddPostRepository {

    fun addThread(
        content: String,
        forumId: Long,
        forumName: String,
        title: String? = "",
        isHide: Int? = 1,
        isTitle: Int? = 1
    ): Flow<AddThreadBean>

    fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String? = null,
        nameShow: String? = null,
        postId: Long? = null,
        subPostId: Long? = null,
        replyUserId: Long? = null,
    ): Flow<AddPostResponse>

    suspend fun upload(
        forumName: String,
        images: List<Uri>,
        isOriginImage: Boolean = false
    ): List<UploadPictureResultBean>
}

@Singleton
class AddPostRepositoryImpl @Inject constructor(
    private val networkDataSource: ReplyNetworkDataSource,
    private val settingsRepo: SettingsRepository,
): AddPostRepository {

    override fun addThread(
        content: String,
        forumId: Long,
        forumName: String,
        title: String?,
        isHide: Int?,
        isTitle: Int?,
    ): Flow<AddThreadBean> =
        networkDataSource
            .addThread(
                content = content,
                forumId = forumId,
                forumName = forumName,
                title = title.orEmpty(),
                isHide = requireNotNull(isHide),
                isTitle = requireNotNull(isTitle)
            )

    override fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String?,
        nameShow: String?,
        postId: Long?,
        subPostId: Long?,
        replyUserId: Long?,
    ): Flow<AddPostResponse> =
        networkDataSource
            .addPost(
                content,
                forumId = forumId,
                forumName = forumName,
                threadId = threadId,
                tbs = tbs,
                nameShow = nameShow,
                postId = postId,
                subPostId = subPostId,
                replyUserId = replyUserId
            )

    override suspend fun upload(
        forumName: String,
        images: List<Uri>,
        isOriginImage: Boolean
    ): List<UploadPictureResultBean> {
        val watermarkType = settingsRepo.habitSettings.snapshot().imageWatermarkType
        return networkDataSource.upload(
            forumName = forumName,
            images = images,
            watermarkType = watermarkType,
            isOriginImage = isOriginImage
        )
    }
}