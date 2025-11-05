package com.huanchengfly.tieba.post.repository

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import com.huanchengfly.tieba.post.api.models.protos.Page
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorResponseData
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponseData
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.repository.source.network.ThreadNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_LZ
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_USER
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    val postCount: Int = 0,
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

/**
 * UiModel of [PbFloorResponseData]
 * */
class PbFloorUiResponse(
    val post: PostData?,
    val subPosts: List<SubPostItemData>,
    val tbs: String,
    val thread: ThreadInfoData,
    val page: PageData,
)

@Singleton
class PbPageRepository @Inject constructor(
    private val blockRepo: BlockRepository,
    settingsRepo: SettingsRepository
) {

    private val networkDataSource = ThreadNetworkDataSource

    private val blockSettings: Flow<BlockSettings> = settingsRepo.blockSettings.flow

    private val habitSettings: Flow<HabitSettings> = settingsRepo.habitSettings.flow

    /**
     * 发送帖子点赞请求
     *
     * @param thread 帖子
     * */
    suspend fun requestLikeThread(thread: ThreadInfoData) {
        val liked = !thread.like.liked // reverse like status
        networkDataSource.requestLikeThread(thread.id, postId = thread.firstPostId, liked)
    }

    suspend fun requestLikeThread(thread: ThreadItem) {
        val like = !thread.like.liked // reverse like status
        networkDataSource.requestLikeThread(thread.id, thread.firstPostId, like)
    }

    suspend fun requestLikeSubPost(threadId: Long, subPost: SubPostItemData) {
        val like = !subPost.like.liked // reverse like status
        networkDataSource.requestLikeSubpost(threadId, subPost.id, like)
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
        val showBothName = habitSettings.first().showBothName
        val firstPost = data.first_floor_post?.mapToUiModel(lz.id, hideBlocked = false/* non-blockable */, showBothName)

        return PbPageUiResponse(
            user = data.user?.takeIf { it.is_login == 1 }?.mapToUiModel(lzId = lz.id, showBothName),
            firstPost = firstPost,
            posts = data.post_list.mapToUiModel(lzId = lz.id, hideBlocked, showBothName, isBlocked = blockRepo::isBlocked),
            tbs = data.anti!!.tbs,
            thread = ThreadInfoData(data.thread),
            page = PageData(
                current = pageData.current_page,
                total = pageData.new_total_page,
                postCount = pageData.total_count,
                nextPagePostId = nextPagePostId,
                hasMore = pageData.has_more != 0,
                hasPrevious = pageData.has_prev != 0,
            )
        )
    }

    suspend fun pbFloor(threadId: Long, postId: Long, forumId: Long, page: Int): PbFloorUiResponse {
        val data = networkDataSource.pbFloor(threadId, postId, forumId, page)
        val post = data.post ?: throw TiebaException("Null post")
        val pageData = data.page ?: throw TiebaException("Null page data")
        val lzId = data.thread?.author?.id ?: -1L
        val anti = data.anti ?: throw TiebaException("Null anti data")
        val hideBlocked = blockSettings.first().hideBlocked
        val showBothName = habitSettings.first().showBothName
        return PbFloorUiResponse(
            post = post.mapToUiModel(lzId, hideBlocked = false/* non-blockable */, showBothName),
            subPosts = data.subpost_list.mapToUiModel(
                lzId = lzId,
                abstract = false,
                showBothName = showBothName,
                hideBlocked = hideBlocked,
                isBlocked = blockRepo::isBlocked
            ),
            tbs = anti.tbs,
            thread = ThreadInfoData(data.thread!!),
            page = PageData(
                current = pageData.current_page,
                total = pageData.total_page,
                postCount = pageData.total_count,
                nextPagePostId = -1,
                hasMore = pageData.has_more != 0,
                hasPrevious = pageData.has_prev != 0
            )
        )
    }

    suspend fun deletePost(postId: Long, thread: ThreadInfoData, tbs: String?, delMyPost: Boolean) {
        val (forumId, forumName, _) = thread.simpleForum
        networkDataSource.deletePost(forumId, forumName, thread.id, postId, tbs, delMyPost)
    }

    suspend fun deleteSubPost(subPostId: Long, thread: ThreadInfoData, tbs: String?, delMyPost: Boolean) {
        val (forumId, forumName, _) = thread.simpleForum
        networkDataSource.deletePost(forumId, forumName, thread.id, subPostId, tbs, delMyPost)
    }

    suspend fun deleteThread(thread: ThreadInfoData, tbs: String?, delMyThread: Boolean) {
        val forumId = thread.simpleForum.first
        require(forumId > 0)
        networkDataSource.delete(
            forumId = forumId,
            forumName = thread.simpleForum.second,
            threadId = thread.id,
            tbs = tbs,
            isSelfThread = delMyThread
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

/**
 * Convert [User] to UI Model
 *
 * @param lzId 楼主 uid
 * @param showBothName 同时显示用户名和昵称
 * */
private fun User.mapToUiModel(lzId: Long, showBothName: Boolean): UserData = UserData(
    id = id,
    name = name,
    nameShow = nameShow,
    showBothName = showBothName,
    avatarUrl = StringUtil.getAvatarUrl(portrait),
    portrait = portrait,
    ip = ip_address,
    levelId = level_id,
    bawuType = if (is_bawu == 1) { if (bawu_type == "manager") "吧主" else "小吧主" } else null,
    isLz = id == lzId
)

@OptIn(ExperimentalTextApi::class)
private fun buildAbstractContent(content: List<PbContentRender>, user: UserData): AnnotatedString {
    return buildAnnotatedString {
        val primaryColor = ThemeUtil.currentColorScheme().primary
        withAnnotation(TAG_USER, user.id.toString()) {
            withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                append(user.nameShow)
            }
        }
        if (user.isLz) {
            appendInlineContent(TAG_LZ)
        }
        append(": ")
        content.forEach { append(it.toAnnotationString()) }
    }
}

private suspend fun SubPostList.mapToUiModel(
    lzId: Long,
    abstract: Boolean,
    showBothName: Boolean,
    isBlocked: suspend (uid: Long, content: String) -> Boolean,
): SubPostItemData {
    val author = author!!.mapToUiModel(lzId, showBothName)
    val plainText = content.plainText.orEmpty()
    val contentRenders = content.renders
    return SubPostItemData(
        author = author,
        id = id,
        blocked = isBlocked(author.id, plainText),
        time = time.toLong(),
        like = agree?.let { Like(agree = it) } ?: LikeZero,
        plainText = plainText,
        abstractContent = if (abstract) buildAbstractContent(contentRenders, author) else null,
        content = if (abstract) null else contentRenders
    )
}

/**
 * Convert SubPostLists to UI Model
 *
 * @param lzId user ID of LZ
 * @param abstract build abstract content instead of full PbContent, ``true`` for ThreadPage
 * @param showBothName show both username and nickname
 * @param hideBlocked remove blocked subpost from result
 * */
private suspend fun List<SubPostList>.mapToUiModel(
    lzId: Long,
    abstract: Boolean,
    showBothName: Boolean,
    hideBlocked: Boolean,
    isBlocked: suspend (uid: Long, content: String) -> Boolean
): List<SubPostItemData> {
    if (isEmpty()) return emptyList()

    return withContext(Dispatchers.Default) {
        mapNotNull {
            it.mapToUiModel(lzId, abstract, showBothName, isBlocked).takeUnless { i -> i.blocked && hideBlocked }
        }
    }
}

/**
 * Convert Post to UI Model
 *
 * @param lzId user ID of LZ
 * @param hideBlocked remove blocked subpost
 * @param showBothName show both username and nickname
 * @param isBlocked check author, title or content is blocked
 * */
private suspend fun Post.mapToUiModel(
    lzId: Long,
    hideBlocked: Boolean = false,
    showBothName: Boolean,
    isBlocked: suspend (uid: Long, content: String) -> Boolean = { _, _, -> false },
): PostData {
    val plainText = content.plainText.orEmpty()
    val author = author!!.mapToUiModel(lzId, showBothName)
    val subPosts = sub_post_list?.sub_post_list?.mapToUiModel(lzId, abstract = true, showBothName, hideBlocked, isBlocked)

    return PostData(
        id = this.id,
        author = author,
        floor = floor,
        title = title.takeUnless { is_ntitle == 1 || title.isEmpty() || title.isBlank() },
        time = DateTimeUtils.fixTimestamp(time.toLong()),
        like = agree?.let { Like(agree = it) } ?: LikeZero,
        blocked = isBlocked(author.id, plainText),
        plainText = plainText,
        contentRenders = contentRenders,
        subPosts = subPosts,
        subPostNumber = sub_post_number
    )
}

private suspend fun List<Post>.mapToUiModel(
    lzId: Long,
    hideBlocked: Boolean,
    showBothName: Boolean,
    isBlocked: suspend (uid: Long, content: String) -> Boolean,
): List<PostData> {
    return withContext(Dispatchers.Default) {
        mapNotNull {
            // 0楼: 伪装的广告, 1楼: 楼主
            if (it.floor > 1) {
                it.mapToUiModel(lzId, hideBlocked, showBothName, isBlocked).takeUnless { p -> hideBlocked && p.blocked }
            } else {
                null
            }
        }
    }
}
