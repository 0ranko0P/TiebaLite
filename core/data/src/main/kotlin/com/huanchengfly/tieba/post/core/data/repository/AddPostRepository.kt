package com.huanchengfly.tieba.post.core.data.repository

import android.net.Uri
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.network.model.UploadPictureResultBean
import com.huanchengfly.tieba.post.core.network.model.protos.addPost.AddPostResponseData
import com.huanchengfly.tieba.post.core.network.session.CredentialProvider
import com.huanchengfly.tieba.post.core.network.source.ReplyNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton

data class AddThreadResult(
    val message: String,
    val pid: Long,
    val tid: Long
)

/**
 * Data layer interface for the reply feature.
 *
 * From: com.huanchengfly.tieba.post.repository.AddPostRepository
 *
 * @author HuanChengFly
 * @author zzc10086
 * @author 0Ranko0P
 *
 * @since 4.0.0 beta 1
 */
interface AddPostRepository {

    suspend fun addThread(
        content: String,
        forumId: Long,
        forumName: String,
        title: String?,
    ): AddThreadResult

    suspend fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null,
        replyUserId: Long? = null,
    ): AddPostResponseData

    suspend fun upload(
        forumName: String,
        images: List<Uri>,
        isOriginImage: Boolean = false
    ): List<UploadPictureResultBean>
}

@Singleton
internal class AddPostRepositoryImpl @Inject constructor(
    private val networkDataSource: ReplyNetworkDataSource,
    private val settingsRepo: SettingsRepository,
    private val credentialProvider: CredentialProvider,
): AddPostRepository {

    override suspend fun addThread(
        content: String,
        forumId: Long,
        forumName: String,
        title: String?,
    ): AddThreadResult {
        val result = networkDataSource.addThread(
            content = content,
            forumId = forumId,
            forumName = forumName,
            title = title,
        )
        return AddThreadResult(
            message = result.errorMsg.orEmpty(),
            pid = result.pid?.toLongOrNull() ?: 0,
            tid = result.tid!!.toLong(),
        )
    }

    override suspend fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        postId: Long?,
        subPostId: Long?,
        replyUserId: Long?,
    ): AddPostResponseData {
        return networkDataSource
            .addPost(
                content,
                forumId = forumId,
                forumName = forumName,
                threadId = threadId,
                tbs = credentialProvider.requireTbs(),
                postId = postId,
                subPostId = subPostId,
                replyUserId = replyUserId
            )
    }

    override suspend fun upload(
        forumName: String,
        images: List<Uri>,
        isOriginImage: Boolean
    ): List<UploadPictureResultBean> {
        require(images.isNotEmpty())
        val watermarkType = settingsRepo.habitSettings.snapshot().imageWatermarkType
        return networkDataSource.upload(
            forumName = forumName,
            images = images,
            watermarkType = watermarkType,
            isOriginImage = isOriginImage
        )
    }
}