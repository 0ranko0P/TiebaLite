package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.models.protos.Page
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.repository.source.network.ThreadNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

object EmptyDataException : TiebaException("data is empty!") {
    override val code: Int
        get() = -2
}

/**
 * UiModel of [Page]
 * */
data class PageData(
    val current: Int = 0,
    val total: Int = 0,
    val nextPagePostId: Long = 0,
    val hasMore: Boolean = false,
    val hasPrevious: Boolean = false
)

/**
 * UiModel of [PbPageResponse]
 * */
class PbPageUiResponse(
    val user: UserData?,
    val firstPost: PostData?,
    val posts: List<PostData>,
    val tbs: String,
    val thread: ThreadInfoData,
    val page: PageData,
)

@Singleton
class PbPageRepository @Inject constructor(
    settingsRepo: SettingsRepository
) {

    private val networkDataSource = ThreadNetworkDataSource

    private val blockSettings = settingsRepo.blockSettings.flow

    /**
     * 发送帖子点赞请求
     *
     * @param thread 帖子
     * */
    suspend fun requestLikeThread(thread: ThreadInfoData) {
        val liked = thread.like.liked
        networkDataSource.requestLikeThread(thread.id, postId = thread.firstPostId, !liked)
    }

    suspend fun pbPage(
        threadId: Long,
        page: Int = 1,
        postId: Long = 0,
        forumId: Long? = null,
        seeLz: Boolean = false,
        sortType: Int = 0,
        back: Boolean = false,
        from: String = "",
        lastPostId: Long? = null,
    ): PbPageUiResponse {
        val data = networkDataSource.pbPage(
            threadId,
            page,
            postId = postId,
            seeLz = seeLz,
            sortType = sortType,
            back = back,
            forumId = forumId,
            from = from,
            lastPostId = lastPostId
        )
        val pageData = data.page ?: throw TiebaException("Null page")
        val lz = data.thread!!.author!!
        val nextPagePostId = if (sortType == ThreadSortType.BY_ASC) {
            0
        } else {
            data.thread.getNextPagePostId(data.post_list, sortType)
        }
        val hideBlocked = blockSettings.first().hideBlocked

        return PbPageUiResponse(
            user = data.user?.takeIf { it.is_login == 1 }?.run { UserData(user = this, isLz = id == lz.id) },
            firstPost = data.first_floor_post?.let { PostData.from(post = it, lzId = lz.id) },
            posts = data.post_list.mapToUiModel(lzId = lz.id, hideBlocked),
            tbs = data.anti!!.tbs,
            thread = ThreadInfoData(data.thread),
            page = PageData(
                current = pageData.current_page,
                total = pageData.new_total_page,
                nextPagePostId = nextPagePostId,
                hasMore = pageData.has_more != 0,
                hasPrevious = pageData.has_prev != 0,
            )
        )
    }

    suspend fun deleteThread(thread: ThreadInfoData, tbs: String?, isSelfThread: Boolean) {
        val forumId = thread.simpleForum.first
        require(forumId > 0)
        networkDataSource.delete(
            forumId = forumId,
            forumName = thread.simpleForum.second,
            threadId = thread.id,
            tbs = tbs,
            isSelfThread = isSelfThread
        )
    }

    /**
     * 加载帖子预览
     * */
    suspend fun loadPreview(threadId: Long): PbPageResponseData {
        return networkDataSource.pbPageRaw(
            threadId = threadId,
            page = 1,
            postId = 0,
            forumId = null,
            seeLz = false,
            sortType = 0,
            back = false,
            from = "",
            lastPostId = null,
        )
    }

    private suspend fun List<Post>.mapToUiModel(lzId: Long, hideBlocked: Boolean): List<PostData> {
        return withContext(Dispatchers.Default) {
            mapNotNull {
                // 0楼: 伪装的广告, 1楼: 楼主
                if (it.floor > 1) {
                    PostData.from(post = it, lzId, hideBlocked)
                        .takeUnless { post -> hideBlocked && post.blocked }
                } else {
                    null
                }
            }
        }
    }

    private suspend fun ThreadInfo.getNextPagePostId(newData: List<Post>, sortType: Int): Long {
        return withContext(Dispatchers.Default) {
            val postIds = newData.mapTo(HashSet()) { it.id }
            val fetchedPostIds = pids
                .split(",")
                .filterNot { it.isBlank() }
                .map { it.toLong() }
            if (sortType == ThreadSortType.BY_DESC) {
                fetchedPostIds.firstOrNull() ?: 0
            } else {
                val nextPostIds = fetchedPostIds.filterNot { pid -> postIds.contains(pid) }
                if (nextPostIds.isNotEmpty()) nextPostIds.last() else 0
            }
        }
    }
}