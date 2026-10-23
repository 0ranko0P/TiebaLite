package com.huanchengfly.tieba.post.core.data.repository

import com.huanchengfly.tieba.post.core.data.model.LoadPicPageData
import com.huanchengfly.tieba.post.core.network.model.PicPageBean
import com.huanchengfly.tieba.post.core.network.source.ThreadPictureDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer interface for the photo view feature.
 */
interface PhotoRepository {

    suspend fun picturePage(
        data: LoadPicPageData,
        picId: String = data.picId,
        picIndex: Int = data.picIndex,
        prev: Boolean,
    ): PicPageBean
}

@Singleton
internal class PhotoRepositoryImpl @Inject constructor(
    private val networkDataSource: ThreadPictureDataSource,
): PhotoRepository {

    override suspend fun picturePage(
        data: LoadPicPageData,
        picId: String,
        picIndex: Int,
        prev: Boolean,
    ): PicPageBean {
        return networkDataSource.picturePage(
            forumId = data.forumId,
            forumName = data.forumName,
            threadId = data.threadId,
            seeLz = data.seeLz,
            picId = picId,
            picIndex = picIndex,
            objType = data.objType,
            prev = prev,
        )
    }
}
