package com.huanchengfly.tieba.post.repository

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.huanchengfly.tieba.post.api.models.ThreadBean
import com.huanchengfly.tieba.post.api.models.ThreadInfoBean
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.repository.source.network.HotTopicNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.widgets.compose.buildThreadContent
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotTopicRepository @Inject constructor(
    private val networkDataSource: HotTopicNetworkDataSource,
    private val blockRepo: BlockRepository,
    private val threadRepo: PbPageRepository,
    private val settingsRepo: SettingsRepository
) {

    val habitSettings: Settings<HabitSettings>
        get() = settingsRepo.habitSettings

    suspend fun loadTopicDetail(
        topicId: Long,
        topicName: String,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0,
        lastId: Long? = null,
    ): TopicDetailData {
        require(topicId > 0)
        require(offset >= 0)
        require(lastId == null || lastId > 0) {
            "The lastId is expected to be null or greater than zero, current: $lastId"
        }

        val start = System.currentTimeMillis()
        val data = networkDataSource.topicDetail(
            topicId = topicId,
            topicName = topicName,
            isNew = 1,
            isShare = 1,
            page = page,
            pageSize = pageSize,
            offset = offset,
            lastId = lastId?.toString().orEmpty()
        )
        val result = TopicDetailData(
            topicInfo = data.topicInfo,
            threads = mapRelateThreadToUiModel(threads = data.relateThread.threadList),
            hasMore = data.hasMore,
            page = data.wreq.page,
            pageSize = data.wreq.pageSize
        )
        val cost = System.currentTimeMillis() - start
        val size = data.relateThread.threadList.size
        Log.i(TAG, "onLoadTopicDetailInternal: Done, size: $size, ID: $topicId, page $page, cost ${cost}ms")
        return result
    }

    suspend fun loadTopicDetailMore(
        topicId: Long,
        topicName: String,
        page: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        lastId: Long,
    ): TopicDetailData {
        val offset = (page - 1) * pageSize
        return loadTopicDetail(topicId, topicName, page, pageSize, offset, lastId)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun mapRelateThreadToUiModel(threads: List<ThreadBean>): List<ThreadItem> {
        if (threads.isEmpty()) return emptyList()

        return withContext(Dispatchers.Default) {
            val habit = habitSettings.snapshot()
            threads.map {
                it.threadInfo.mapUiModel(showBothName = habit.showBothName, isBlocked = blockRepo::isBlocked)
            }
        }
    }

    suspend fun onLikeThread(thread: ThreadItem) = threadRepo.requestLikeThread(thread)

    companion object {
        private const val TAG = "HotTopicRepository"

        const val DEFAULT_PAGE_SIZE: Int = 10

        class TopicDetailData(
            val topicInfo: TopicInfoBean,
            val threads: List<ThreadItem>,
            val hasMore: Boolean,
            val page: Int,
            val pageSize: Int,
        )

        /**
         * Convert ThreadInfoBean to UI Model.
         *
         * @param showBothName show both username and nickname
         * @param isBlocked check thread author, title or content is blocked
         * */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        suspend fun ThreadInfoBean.mapUiModel(
            showBothName: Boolean,
            isBlocked: suspend (uid: Long, content: Array<String>) -> Boolean,
        ): ThreadItem {
            val author = with(author) {
                val nameShow = StringUtil.getUserNameString(showBothName, name, nameShow)
                Author(id = this.id, name = nameShow, avatarUrl = StringUtil.getAvatarUrl(portrait))
            }
            val title = this.title.orEmpty()

            return ThreadItem(
                id = this.id,
                firstPostId = this.firstPostId,
                author = author,
                blocked = isBlocked(author.id, arrayOf(title, abstractText)),
                content = buildThreadContent(title, abstractText),
                title = title,
                lastTimeMill = DateTimeUtils.fixTimestamp(lastTimeInt),
                like = this.agree.run { Like(liked = hasAgree == 1, count = agreeNum.toLong()) },
                replyNum = this.replyNum,
                shareNum = this.shareNum,
                medias = this.media.map {
                    // Use Protobuf Media quality level: srcPic > bigPic
                    val bigPic = it.smallPic
                    val originPic = it.bigPic
                    Media(
                        type = it.type.toIntOrNull() ?: 0,
                        bigPic = bigPic,
                        srcPic = originPic,
                        originPic = originPic,
                        width = it.width.toIntOrNull() ?: 0,
                        height = it.height.toIntOrNull() ?: 0,
                        isLongPic = it.isLongPic
                    )
                },
                simpleForum = SimpleForum(forumId, forumName, null),
            )
        }
    }
}