package com.huanchengfly.tieba.post.repository

import android.util.SparseArray
import androidx.annotation.WorkerThread
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListResponseData
import com.huanchengfly.tieba.post.api.models.protos.personalized.DislikeReason
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponseData
import com.huanchengfly.tieba.post.api.models.protos.userLike.ConcernData
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.repository.source.local.ExploreLocalDataSource
import com.huanchengfly.tieba.post.repository.source.network.ExploreNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.explore.Dislike
import com.huanchengfly.tieba.post.ui.models.explore.HotTab
import com.huanchengfly.tieba.post.ui.models.explore.HotTopicData
import com.huanchengfly.tieba.post.ui.models.explore.RecommendTopic
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.widgets.compose.buildThreadContent
import com.huanchengfly.tieba.post.utils.BlockManager
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class UserLikeThreads(
    val requestUnix: Long,
    val pageTag: String,
    val hasMore: Boolean,
    val threads: List<ThreadItem>
)

@Singleton
class ExploreRepository @Inject constructor(
    private val localDataSource: ExploreLocalDataSource,
    private val homeRepo: HomeRepository,
    private val threadRepo: PbPageRepository,
    settingsRepository: SettingsRepository
) {

    private val networkDataSource = ExploreNetworkDataSource

    private val habitSettings = settingsRepository.habitSettings.flow

    private val blockSettings = settingsRepository.blockSettings.flow

    /**
     * [Dislike] 缓存池
     *
     * @see getCachedDislike
     * */
    private val dislikePoll = SparseArray<Dislike>()

    suspend fun loadHotTopic(cached: Boolean = false) = loadHotThreads(HOT_THREAD_TAB_ALL, cached)

    suspend fun loadHotThreads(tabCode: String, cached: Boolean): HotTopicData {
        val habit = habitSettings.first()
        var data: HotThreadListResponseData? = null
        if (cached) {
            data = localDataSource.loadHotThread(tabCode)
        } else if (tabCode == HOT_THREAD_TAB_ALL) {
            // on force-refresh all, purge hot thread caches
            localDataSource.purgeHotThread()
        }

        // no cache or expired, fetch from network
        if (data == null) {
            data = networkDataSource.loadHotThread(tabCode)
            localDataSource.saveHotThread(tabCode, data)
        }

        return data.mapUiModel(habit.showBothName)
    }

    suspend fun loadPersonalized(page: Int, cached: Boolean): List<ThreadItem> {
        var data: PersonalizedResponseData? = null
        if (cached) {
            data = localDataSource.loadPersonalized(page)
        } else if (page == 1) {
            // on force-refresh first page, purge all cached pages
            localDataSource.purgePersonalized()
        }

        if (data == null) { // no cache, fetch from network
            data = if (page == 1) {
                networkDataSource.refreshPersonalizedThread()
            } else {
                networkDataSource.loadMorePersonalizedThread(page)
            }
            localDataSource.savePersonalized(data, page)
        }

        return data.mapUiModel(
            showBothName = habitSettings.first().showBothName,
            blockVideo = blockSettings.first().blockVideo,
            dislikeProvider = this::getCachedDislike
        )
    }

    suspend fun refreshUserLike(lastRequestUnix: Long?, cached: Boolean): UserLikeThreads {
        val uid = homeRepo.currentAccount.first()?.uid?.toLong() ?: throw TiebaNotLoggedInException()
        var data: UserLikeResponseData? = null
        var lastRequestTime = lastRequestUnix ?: 0

        // Obtain cached data when lastRequestUnix is null
        if (cached || lastRequestUnix == null) {
            val (cachedLastRequestTime, cachedData) = localDataSource.loadUserLikeDataFirstPage(uid)
            if (cached) data = cachedData
            if (cachedLastRequestTime > 0) lastRequestTime = cachedLastRequestTime
        }

        if (data == null) { // no cache, fetch from network
            data = networkDataSource.refreshUserLikeThread(lastRequestTime)
            localDataSource.saveUserLikeFirstPage(uid, data)
        }
        val showBothName = habitSettings.first().showBothName
        val threads = data.threadInfo.mapUiModel(showBothName)
        return UserLikeThreads(data.requestUnix, data.pageTag, data.hasMore == 1, threads)
    }

    /**
     * Load user like threads from network
     *
     * @param pageTag tag of next page thread
     * @param lastRequestUnix last request unix time (10-digit Unix timestamp)
     * */
    suspend fun loadUserLike(pageTag: String, lastRequestUnix: Long): UserLikeThreads {
        val data = networkDataSource.loadMoreUserLikeThread(pageTag, lastRequestUnix)
        val threads = data.threadInfo.mapUiModel(habitSettings.first().showBothName)
        return UserLikeThreads(data.requestUnix, data.pageTag, data.hasMore == 1, threads)
    }

    suspend fun onLikeThread(thread: ThreadItem, from: ExplorePageItem) {
        threadRepo.requestLikeThread(thread)
        // update local data is costly, purge caches instead
        purgeCache(targetPage = from)
    }

    fun purgeCache(targetPage: ExplorePageItem) {
        AppBackgroundScope.launch {
            when (targetPage) {
                ExplorePageItem.Concern -> {
                    val uid = homeRepo.currentAccount.first()?.uid?.toLong() ?: throw TiebaNotLoggedInException()
                    localDataSource.purgeUserLike(uid)
                }

                ExplorePageItem.Personalized -> localDataSource.purgePersonalized()

                ExplorePageItem.Hot -> localDataSource.purgeHotThread()
            }
        }
    }

    suspend fun onDislikeThread(thread: ThreadItem, reasons: List<Dislike>) {
        val clickTimeMill = System.currentTimeMillis()
        val (forumId, _, _) = thread.simpleForum
        val dislikeIds = reasons.joinToString(",") { it.id.toString() }
        val extra = reasons.joinToString(",") { it.extra }
        networkDataSource.submitDislikePersonalizedThread(thread.id, forumId, clickTimeMill, dislikeIds, extra)
    }

    private fun getCachedDislike(dislikeReason: DislikeReason): Dislike {
        val id = dislikeReason.dislikeId
        val reason = dislikeReason.dislikeReason
        // 16: 重复、旧闻, 17: 广告软文, 18: 色情低俗, 19: 低质水贴, 200: 恐怖恶心
        return if (id in 16..19 || id == 200) {
            synchronized(dislikePoll) {
                dislikePoll[id] ?: Dislike(id, reason, dislikeReason.extra).also { dislikePoll[id] = it }
            }
        } else {
            Dislike(id, reason, dislikeReason.extra)
        }
    }

    companion object {
        const val HOT_THREAD_TAB_ALL = "all"

        suspend fun List<ThreadItem>.distinctById(): List<ThreadItem> {
            return withContext(Dispatchers.Default) { distinctBy { it.id } }
        }

        @WorkerThread
        fun ThreadInfo.mapUiModel(
            showBothName: Boolean,
            threadDislikeMap: Map<Long, List<Dislike>>? // <ThreadId: ThreadPersonalized.dislikeResource>
        ): ThreadItem {
            val author = with(this.author!!) {
                val nameShow = StringUtil.getUserNameString(showBothName, name, nameShow)
                Author(id = this.id, name = nameShow, avatarUrl = StringUtil.getAvatarUrl(portrait))
            }
            val simpleForum = with(this.forumInfo!!) { SimpleForum(id, name, avatar) }
            return if (this.isTop != 1) {
                val abstractText = this.abstractText
                ThreadItem(
                    id = this.id,
                    firstPostId = this.firstPostId,
                    author = author,
                    blocked = BlockManager.shouldBlock(authorId, title, abstractText),
                    content = buildThreadContent(title, abstractText, tabName, isGood = this.isGood == 1),
                    title = this.title,
                    lastTimeMill = DateTimeUtils.fixTimestamp(lastTimeInt.toLong()),
                    like = this.agree?.let { Like(it) } ?: LikeZero,
                    hotNum = this.hotNum,
                    replyNum = this.replyNum,
                    shareNum = this.shareNum,
                    medias = this.media,
                    video = this.videoInfo?.wrapImmutable(),
                    originThreadInfo = origin_thread_info?.takeIf { is_share_thread == 1 }?.wrapImmutable(),
                    simpleForum = simpleForum,
                    dislikeResource = threadDislikeMap?.get(this.id)
                )
            } else { // 置顶贴: 不可拉黑, 无内容, 无图
                ThreadItem(
                    id = this.id,
                    firstPostId = this.firstPostId,
                    author = author,
                    isTop = true,
                    title = this.title,
                    lastTimeMill = DateTimeUtils.fixTimestamp(lastTimeInt.toLong()),
                    simpleForum = simpleForum
                )
            }
        }

        private suspend fun HotThreadListResponseData.mapUiModel(showBothName: Boolean): HotTopicData {
            return withContext(Dispatchers.Default) {
                HotTopicData(
                    topics = topicList.map { RecommendTopic(it.topicName, it.tag) },
                    tabs = hotThreadTabInfo.map { HotTab(name = it.tabName, tabCode = it.tabCode) },
                    threads = threadInfo.map {
                        it.mapUiModel(showBothName, threadDislikeMap = null)
                    }
                )
            }
        }

        private suspend fun PersonalizedResponseData.mapUiModel(
            showBothName: Boolean,
            blockVideo: Boolean,
            dislikeProvider: (DislikeReason) -> Dislike
        ): List<ThreadItem> {
            // Map<ThreadId: ThreadPersonalized.dislikeResource>
            val threadDislikeMap: Map<Long, List<Dislike>> = thread_personalized.associateBy(
                keySelector = { it.tid },
                // Map dislikeResource to List<Dislike> with cached object pool
                valueTransform = { it.dislikeResource.map(dislikeProvider) }
            )
            return withContext(Dispatchers.Default) {
                thread_list
                    .filter { !blockVideo || it.videoInfo == null }
                    .map { it.mapUiModel(showBothName, threadDislikeMap) }
            }
        }

        private suspend fun List<ConcernData>.mapUiModel(showBothName: Boolean): List<ThreadItem> {
            return withContext(Dispatchers.Default) {
                mapNotNull {
                    if (it.recommendType == 1 && it.threadList != null) {
                        it.threadList.mapUiModel(showBothName, threadDislikeMap = null)
                    } else {
                        null // recommend users
                    }
                }
            }
        }
    }
}