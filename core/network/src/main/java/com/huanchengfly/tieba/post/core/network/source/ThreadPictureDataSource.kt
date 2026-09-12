package com.huanchengfly.tieba.post.core.network.source

import com.huanchengfly.tieba.post.core.network.model.PicPageBean
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import javax.inject.Inject

interface ThreadPictureDataSource {

    suspend fun picturePage(
        forumId: Long,
        forumName: String,
        threadId: Long,
        seeLz: Boolean,
        picId: String,
        picIndex: Int,
        objType: String,
        prev: Boolean
    ): PicPageBean
}

internal class RetrofitThreadPictureDataSource @Inject constructor(
    private val tiebaApi: ITiebaApi,
) : ThreadPictureDataSource {

    override suspend fun picturePage(
        forumId: Long,
        forumName: String,
        threadId: Long,
        seeLz: Boolean,
        picId: String,
        picIndex: Int,
        objType: String,
        prev: Boolean
    ): PicPageBean {
        return tiebaApi.picPageFlow(
            forumId = forumId.toString(),
            forumName = forumName,
            threadId = threadId.toString(),
            seeLz = seeLz,
            picId = picId,
            picIndex = picIndex.toString(),
            objType = objType,
            prev = prev
        ).firstOrThrow()
    }

}
