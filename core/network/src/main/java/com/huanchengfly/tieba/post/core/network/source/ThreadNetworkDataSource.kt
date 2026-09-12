package com.huanchengfly.tieba.post.core.network.source

import android.text.TextUtils
import com.huanchengfly.tieba.post.core.network.Error.ERROR_POST_NOMORE
import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.exception.TiebaException
import com.huanchengfly.tieba.post.core.network.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.model.protos.SubPost
import com.huanchengfly.tieba.post.core.network.model.protos.User
import com.huanchengfly.tieba.post.core.network.model.protos.pbFloor.PbFloorResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.pbPage.PbPageResponseData
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.commonResponse
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import com.huanchengfly.tieba.post.core.network.source.ThreadNetworkDataSource.Companion.ST_TYPE_FROM_STORE
import com.huanchengfly.tieba.post.core.network.source.ThreadNetworkDataSource.Companion.ST_TYPE_MENTION
import com.huanchengfly.tieba.post.core.network.source.ThreadNetworkDataSource.Companion.ST_TYPE_STORE_THREAD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Main entry point for accessing thread data from the network.
 */
interface ThreadNetworkDataSource {

    suspend fun requestLikePost(threadId: Long, postId: Long, like: Boolean)

    suspend fun requestLikeSubpost(threadId: Long, subPostId: Long, like: Boolean)

    suspend fun requestLikeThread(threadId: Long, postId: Long, like: Boolean)

    suspend fun requestPollPost(forumId: Long?, threadId: Long, options: String)

    suspend fun pbPageRaw(
        threadId: Long,
        page: Int = 1,
        postId: Long = 0,
        forumId: Long? = null,
        seeLz: Boolean = false,
        sortType: Int = 0,
        back: Boolean = false,
        from: String?,
        lastPostId: Long? = null,
    ): PbPageResponseData

    suspend fun pbPage(
        threadId: Long,
        page: Int = 1,
        postId: Long = 0,
        forumId: Long? = null,
        seeLz: Boolean = false,
        sortType: Int = 0,
        back: Boolean = false,
        from: String?,
        lastPostId: Long? = null,
    ): PbPageResponseData

    suspend fun delete(forumId: Long, forumName: String, threadId: Long, tbs: String?, isSelfThread: Boolean)

    suspend fun deletePost(
        forumId: Long,
        forumName: String,
        threadId: Long,
        postId: Long,
        tbs: String?,
        delMyPost: Boolean
    )

    suspend fun loadReportPostURL(postId: Long): String

    suspend fun pbFloor(threadId: Long, postId: Long, forumId: Long, page: Int = 1, subPostId: Long): PbFloorResponseData

    companion object {
        const val ST_TYPE_FROM_STORE = "store_thread"
        const val ST_TYPE_MENTION = "mention"
        const val ST_TYPE_STORE_THREAD = ST_TYPE_FROM_STORE
    }
}

internal class RetrofitThreadNetworkDataSource @Inject constructor(
    private val tiebaApi: ITiebaApi,
): ThreadNetworkDataSource {

    private val ST_TYPES = listOf(ST_TYPE_MENTION, ST_TYPE_STORE_THREAD)

    private suspend fun requestLike(threadId: Long, postId: Long, like: Boolean, objType: Int) {
        require(threadId > 0) { "Illegal Thread ID $threadId" }
        require(postId > 0) { "Illegal Post ID: $postId" }
        tiebaApi.opAgreeFlow(
            threadId = threadId.toString(),
            postId = postId.toString(),
            opType = if (like) 0 else 1, // 操作 0 = 点赞, 1 = 取消点赞
            objType = objType
        )
        .firstOrThrow()
        .let {
            if (it.data == null || it.errorCode != "0" ) throw TiebaException(message = it.errorMsg)
        }
    }

    override suspend fun requestLikePost(threadId: Long, postId: Long, like: Boolean) {
        requestLike(threadId, postId, like, objType = 1)
    }

    override suspend fun requestLikeSubpost(threadId: Long, subPostId: Long, like: Boolean) {
        requestLike(threadId, subPostId, like, objType = 2)
    }

    override suspend fun requestLikeThread(threadId: Long, postId: Long, like: Boolean) {
        requestLike(threadId, postId, like, objType = 3)
    }

    override suspend fun requestPollPost(forumId: Long?, threadId: Long, options: String) {
        tiebaApi
            .addPollPostProtobuf(forumId, threadId, options)
            .firstOrThrow()
    }

    override suspend fun pbPageRaw(
        threadId: Long,
        page: Int,
        postId: Long,
        forumId: Long?,
        seeLz: Boolean,
        sortType: Int,
        back: Boolean,
        from: String?,
        lastPostId: Long?,
    ): PbPageResponseData {
        return tiebaApi
            .pbPageFlow(
                threadId = threadId,
                page = page,
                postId = postId,
                seeLz = seeLz,
                sortType = sortType,
                back = back,
                forumId = forumId,
                stType = from?.takeIf { ST_TYPES.contains(it) }.orEmpty(),
                mark = if (from == ST_TYPE_FROM_STORE) 1 else 0,
                lastPostId = lastPostId
            )
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = this.error.commonResponse)
            }
    }

    override suspend fun pbPage(
        threadId: Long,
        page: Int,
        postId: Long,
        forumId: Long?,
        seeLz: Boolean,
        sortType: Int,
        back: Boolean,
        from: String?,
        lastPostId: Long?,
    ): PbPageResponseData {
        val data = pbPageRaw(
            threadId = threadId,
            page = page,
            postId = postId,
            forumId = forumId,
            seeLz = seeLz,
            sortType = sortType,
            back = back,
            from = from,
            lastPostId = lastPostId
        )

        if (data.post_list.isEmpty()) {
            throw TiebaApiException(CommonResponse(errorCode = ERROR_POST_NOMORE))
        }
        if (data.page == null || data.forum == null || data.anti == null) throw TiebaUnknownException

        val lz = data.thread?.author ?: throw TiebaException("Null Lz data")
        val userMap = data.user_list.associateBy { it.id }
        val postList = withContext(Dispatchers.Default) {
            data.post_list.map {
                val author = it.author ?: userMap[it.author_id] ?: throw TiebaException("Null author of post: ${it.id}")
                it.copy(
                    author_id = author.id,
                    author = author,
                    from_forum = data.forum,
                    tid = data.thread.id,
                    sub_post_list = it.sub_post_list?.associateAuthor(userMap),
                )
            }
        }

        // find 1L if possible
        val firstPost = postList.firstOrNull { it.floor == 1 }
            ?: data.first_floor_post?.copy(
                author_id = lz.id,
                author = lz,
                from_forum = data.forum,
                tid = data.thread.id,
                sub_post_list = null
            )

        return data.copy(
            post_list = postList,
            thread = data.thread.let {
                // fill missing properties
                it.copy(threadId = it.id, firstPostId = firstPost?.id ?: it.firstPostId, forumInfo = data.forum)
            },
            banner_list = null,
            ala_info = null,
            first_floor_post = firstPost
        )
    }

    override suspend fun delete(forumId: Long, forumName: String, threadId: Long, tbs: String?, isSelfThread: Boolean) {
        tiebaApi
            .delThreadFlow(forumId, forumName, threadId, tbs, isSelfThread, false)
            .firstOrThrow()
            .let {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }

    override suspend fun deletePost(
        forumId: Long,
        forumName: String,
        threadId: Long,
        postId: Long,
        tbs: String?,
        delMyPost: Boolean
    ) {
        require(!TextUtils.isEmpty(forumName)) { "Illegal Forum" }
        require(threadId > 0) { "Illegal Thread ID $threadId" }

        tiebaApi
            .delPostFlow(forumId, forumName, threadId, postId, tbs, isFloor = false, delMyPost)
            .firstOrThrow()
            .let {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }

    override suspend fun loadReportPostURL(postId: Long): String {
        require(postId > 0) { "Illegal Post ID $postId" }
        return tiebaApi.checkReportPost(postId.toString())
            .run {
                if (errorCode != 0) {
                    throw TiebaApiException(CommonResponse(errorCode ?: -1, errorMsg.orEmpty()))
                }
                data.url
            }
    }

    override suspend fun pbFloor(threadId: Long, postId: Long, forumId: Long, page: Int, subPostId: Long): PbFloorResponseData {
        require(threadId > 0) { "Illegal Thread ID $threadId" }
        require(page > 0) { "Illegal Page: $page" }

        return tiebaApi
            .pbFloorFlow(threadId, postId, forumId, page, subPostId)
            .firstOrThrow()
            .run {
                if (data_ == null) throw TiebaApiException(commonResponse = this.error.commonResponse)
                val forum = data_.forum ?: throw TiebaException("Null forum data")
                val threadInfo = data_.thread ?: throw TiebaException("Null thread data")
                data_.copy(
                    post = data_.post?.copy(sub_post_list = null),
                    // copy data_.forum to data_.thread.forumInfo
                    thread = threadInfo.copy(threadId = threadId, forumInfo = forum)
                )
            }
    }

    private suspend fun SubPost.associateAuthor(users: Map<Long, User>): SubPost {
        return if (sub_post_list.isNotEmpty()) {
            withContext(Dispatchers.Default) {
                copy(
                    sub_post_list = sub_post_list.map { subPost ->
                        if (subPost.author == null) {
                            subPost.copy(author = users[subPost.author_id])
                        } else {
                            subPost
                        }
                    }
                )
            }
        } else {
            this
        }
    }
}
