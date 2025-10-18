package com.huanchengfly.tieba.post.repository.source.network

import android.text.TextUtils
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.SubPost
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorResponseData
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.repository.EmptyDataException
import com.huanchengfly.tieba.post.repository.source.network.ExploreNetworkDataSource.commonResponse
import com.huanchengfly.tieba.post.ui.page.thread.FROM_STORE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main entry point for accessing thread data from the network.
 */
object ThreadNetworkDataSource {

    const val ST_TYPE_MENTION = "mention"
    const val ST_TYPE_STORE_THREAD = FROM_STORE

    private val ST_TYPES = listOf(ST_TYPE_MENTION, ST_TYPE_STORE_THREAD)

    private suspend fun requestLike(threadId: Long, postId: Long, like: Boolean, objType: Int) {
        require(threadId > 0) { "Illegal Thread ID $threadId" }
        require(postId > 0) { "Illegal Post ID: $postId" }
        TiebaApi.getInstance().opAgreeFlow(
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

    suspend fun requestLikePost(threadId: Long, postId: Long, like: Boolean) {
        requestLike(threadId, postId, like, objType = 1)
    }

    suspend fun requestLikeSubpost(threadId: Long, subPostId: Long, like: Boolean) {
        requestLike(threadId, subPostId, like, objType = 2)
    }

    suspend fun requestLikeThread(threadId: Long, postId: Long, like: Boolean) {
        requestLike(threadId, postId, like, objType = 3)
    }

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
    ): PbPageResponseData {
        return TiebaApi.getInstance()
            .pbPageFlow(
                threadId = threadId,
                page = page,
                postId = postId,
                seeLz = seeLz,
                sortType = sortType,
                back = back,
                forumId = forumId,
                stType = from?.takeIf { ST_TYPES.contains(it) }.orEmpty(),
                mark = if (from == FROM_STORE) 1 else 0,
                lastPostId = lastPostId
            )
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = this.error.commonResponse)
            }
    }

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

        if (data.post_list.isEmpty()) throw EmptyDataException
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
            first_floor_post = firstPost
        )
    }

    suspend fun delete(forumId: Long, forumName: String, threadId: Long, tbs: String?, isSelfThread: Boolean) {
        TiebaApi.getInstance()
            .delThreadFlow(forumId, forumName, threadId, tbs, isSelfThread, false)
            .firstOrThrow()
            .let {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }

    suspend fun deletePost(
        forumId: Long,
        forumName: String,
        threadId: Long,
        postId: Long,
        tbs: String?,
        delMyPost: Boolean = true
    ) {
        require(!TextUtils.isEmpty(forumName)) { "Illegal Forum" }
        require(threadId > 0) { "Illegal Thread ID $threadId" }

        TiebaApi.getInstance()
            .delPostFlow(forumId, forumName, threadId, postId, tbs, isFloor = false, delMyPost)
            .firstOrThrow()
            .let {
                if (it.errorCode != 0) throw TiebaApiException(commonResponse = it)
            }
    }

    suspend fun pbFloor(threadId: Long, postId: Long, forumId: Long, page: Int = 1): PbFloorResponseData {
        require(threadId > 0) { "Illegal Thread ID $threadId" }
        require(page > 0) { "Illegal Page: $page" }

        return TiebaApi.getInstance()
            .pbFloorFlow(threadId, postId, forumId, page, subPostId = 0)
            .firstOrThrow()
            .run {
                if (data_ == null) throw TiebaApiException(commonResponse = this.error.commonResponse)
                val forum = data_.forum ?: throw TiebaException("Null forum data")
                val threadInfo = data_.thread ?: throw TiebaException("Null thread data")
                // copy data_.forum to data_.thread.forumInfo
                data_.copy(thread = threadInfo.copy(threadId = threadId, forumInfo = forum))
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