package com.huanchengfly.tieba.post.repository

import android.util.Log
import androidx.collection.LruCache
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.core.data.model.settings.BlockSettings
import com.huanchengfly.tieba.post.core.data.model.settings.ForumSortType
import com.huanchengfly.tieba.post.core.data.model.settings.HabitSettings
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.network.model.SignResultBean
import com.huanchengfly.tieba.post.core.network.model.protos.FrsTabInfo
import com.huanchengfly.tieba.post.core.network.model.protos.ThreadInfo
import com.huanchengfly.tieba.post.core.network.model.protos.frsPage.FrsPageResponseData
import com.huanchengfly.tieba.post.core.network.source.ForumNetworkDataSource
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.distinctById
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.mapUiModel
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.ThreadItemList
import com.huanchengfly.tieba.post.ui.models.forum.ForumData
import com.huanchengfly.tieba.post.ui.models.forum.ForumDetail
import com.huanchengfly.tieba.post.ui.models.forum.ForumManager
import com.huanchengfly.tieba.post.ui.models.forum.ForumRule
import com.huanchengfly.tieba.post.ui.models.forum.GoodClassify
import com.huanchengfly.tieba.post.ui.models.forum.Rule
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private typealias CacheKey = String

private typealias ForumPageResult = Triple<ForumData, ThreadItemList, List<ForumManager>?>

@Singleton
class ForumRepository @Inject constructor(
    settingsRepo: SettingsRepository,
    private val blockRepo: BlockRepository,
    private val homeRepo: HomeRepository,
    private val networkDataSource: ForumNetworkDataSource,
) {
    private val blockedSettings: Flow<BlockSettings> = settingsRepo.blockSettings

    private val habitSettings: Flow<HabitSettings> = settingsRepo.habitSettings

    private val cache: LruCache<CacheKey, ForumCache> = LruCache(MAX_FORUM_CACHE_SIZE)

    private val generalThreadCache: LruCache<CacheKey, ThreadItemList> = LruCache(MAX_GENERAL_THREAD_CACHE_SIZE)

    private suspend fun frsPage(
        forumName: String,
        page: Int,
        loadType: Int,
        sortType: Int,
        goodClassifyId: Int?,
        forceNew: Boolean = false
    ): ForumPageResult {
        val start = System.currentTimeMillis()
        var key: CacheKey? = null
        var cached: ForumCache? = null
        val cacheable = if (sortType == -1) (goodClassifyId ?: 0) == 0 else sortType == ForumSortType.BY_REPLY

        // Load first page from lru cache if possible
        if (page == 1 && cacheable && loadType == 1) {
            key = forumName
            cached = cache[key]
            val typedItemList = cached?.getItemsByType(isGood = goodClassifyId != null)
            if (!forceNew && typedItemList != null) {
                Log.i(TAG, "onFrsPage: Load $forumName from cache, good: $goodClassifyId, size: ${typedItemList.threads.size}")
                return ForumPageResult(cached.forum, typedItemList, cached.managers)
            }
        }

        val data = networkDataSource.frsPage(forumName, page, loadType, sortType, goodClassifyId)
        val forumData = data.toData()
        var forumManagers: List<ForumManager>? = null
        val showBothName = habitSettings.first().showBothName
        val typedThreads = ThreadItemList(
            threads = data.thread_list.mapUiModel(blockedSettings.first(), showBothName, blockRepo::isBlocked),
            threadIds = data.thread_id_list,
            hasMore = data.page!!.has_more == 1
        )

        // is result cacheable
        if (key != null) {
            forumManagers = data.getManagers()
            val normalThreads = if (sortType != -1) typedThreads else cached?.normal
            val goodThreads = if (sortType == -1) typedThreads else cached?.good
            cache.put(key, ForumCache(forumData, forumManagers, normal = normalThreads, good = goodThreads))
        }
        if (BuildConfig.DEBUG) {
            val size = typedThreads.threads.size
            val cost = System.currentTimeMillis() - start
            Log.d(TAG, "onFrsPage: Load $forumName from network, sort $sortType, good: $goodClassifyId, size: $size, cost ${cost}ms")
        }
        return ForumPageResult(forumData, typedThreads, forumManagers)
    }

    suspend fun loadForumInfo(forumName: String, forceNew: Boolean = true): ForumData {
        return frsPage(forumName, page = 1, loadType = 1, sortType = 0, null, forceNew).first
    }

    suspend fun loadForumDetail(forumName: String): ForumDetail {
        val start = System.currentTimeMillis()
        val (forumData, _, managers) = frsPage(forumName, page = 1, loadType = 1, sortType = 0, null)
        val detail = networkDataSource.loadForumDetail(forumData.id)
        val cost = System.currentTimeMillis() - start
        Log.i(TAG, "onLoadForumDetail: $forumName, managers: ${managers?.size}, cost ${cost}ms")

        return ForumDetail(
            avatar = forumData.avatar,
            name = forumData.name,
            id = forumData.id,
            intro = detail.content.plainText,
            slogan = detail.slogan,
            memberCount = detail.member_count,
            threadCount = forumData.threads,
            postCount = forumData.posts,
            managers = managers
        )
    }

    suspend fun loadPage(forum: String, page: Int, sortType: Int, forceNew: Boolean): ThreadItemList = frsPage(
        forumName = forum,
        page = page,
        loadType = 1,
        sortType = sortType,
        goodClassifyId = null,
        forceNew = forceNew
    ).threadList

    suspend fun loadGoodPage(forum: String, page: Int, goodClassifyId: Int?, forceNew: Boolean): ThreadItemList = frsPage(
        forumName = forum,
        page = page,
        loadType = 1,
        sortType = -1,
        goodClassifyId = goodClassifyId ?: 0,
        forceNew = forceNew
    ).threadList

    suspend fun loadMorePage(forum: String, page: Int, sortType: Int): ThreadItemList = frsPage(
        forumName = forum,
        page = page,
        loadType = 2,
        sortType = sortType,
        goodClassifyId = null,
        forceNew = false
    ).threadList

    suspend fun loadMoreGood(forum: String, page: Int, goodClassifyId: Int?): ThreadItemList = frsPage(
        forumName = forum,
        page = page,
        loadType = 2,
        sortType = -1,
        goodClassifyId = goodClassifyId ?: 0,
        forceNew = false
    ).threadList

    suspend fun generalTabList(
        forumId: Long,
        forumName: String,
        tabId: Int,
        tabType: Int,
        tabName: String,
        isGeneralTab: Int,
        pn: Int = 1,
        sortType: Int = -1,
        lastThreadId: Long = 0,
        isDefaultNavTab: Int = 0,
        forceNew: Boolean = false,
    ): ThreadItemList {
        val start = System.currentTimeMillis()
        var cacheKey: CacheKey? = null
        // Load first page from LruCache if possible
        if (pn == 1 && isGeneralTab == 1) {
            cacheKey = "${forumId}_${tabId}_${tabType}_${sortType}_${lastThreadId}"
            val cached = generalThreadCache[cacheKey]
            if (!forceNew && cached != null) {
                Log.i(TAG, "onGeneralTabList: Load $tabName, sort: $sortType from cache, size: ${cached.threads.size}")
                return cached
            }
        }
        val data = networkDataSource.loadGeneralTabList(
            forumId, forumName, tabId, tabType, tabName, isGeneralTab,
            pn, sortType, lastThreadId, isDefaultNavTab
        )
        val blockedSettings = blockedSettings.first()
        val showBothName = habitSettings.first().showBothName
        val threadList = data.general_list.mapUiModel(blockedSettings, showBothName, blockRepo::isBlocked)
        val result = ThreadItemList(threadList, threadIds = emptyList(), hasMore = data.has_more == 1)
        if (cacheKey != null) {
            generalThreadCache.put(cacheKey, result)
        }
        if (BuildConfig.DEBUG) {
            val cost = System.currentTimeMillis() - start
            Log.d(TAG, "onGeneralTabList: Load $tabName pn: $pn from network, sort: $sortType, size: ${threadList.size}, cost ${cost}ms")
        }
        return result
    }

    suspend fun generalTabList(
        forumId: Long,
        forumName: String,
        tabInfo: FrsTabInfo,
        pn: Int = 1,
        sortType: Int = -1,
        lastThreadId: Long = 0,
        forceNew: Boolean = false,
    ): ThreadItemList =
        generalTabList(
            forumId = forumId,
            forumName = forumName,
            tabId = tabInfo.tabId,
            tabType = tabInfo.tabType,
            tabName = tabInfo.tabName,
            isGeneralTab = tabInfo.isGeneralTab,
            pn = pn,
            sortType = sortType,
            lastThreadId = lastThreadId,
            isDefaultNavTab = tabInfo.isGeneralTab,
            forceNew = forceNew,
        )

    suspend fun threadList(forumId: Long, forumName: String, page: Int, sortType: Int, threadIds: List<Long>): List<ThreadItem> {
        return networkDataSource
            .loadThread(forumId, forumName, page, sortType, threadIds)
            .thread_list
            .mapUiModel(
                showBothName = habitSettings.first().showBothName,
                blockedSetting = blockedSettings.first(),
                isBlocked = blockRepo::isBlocked,
            )
    }

    suspend fun loadForumRule(forumId: Long): ForumRule {
        val data = networkDataSource.loadForumRule(forumId)
        // Map ForumRuleDetailResponseData to UI Model
        return withContext(Dispatchers.Default) {
            val showBothName = habitSettings.first().showBothName
            ForumRule(
                headLine = data.title,
                publishTime = data.publish_time.takeUnless { time -> time.isEmpty() },
                preface = data.preface,
                data = data.rules.map {
                    Rule(it.title, it.content.plainText)
                },
                author = data.bazhu?.run {
                    ForumManager(
                        id = user_id,
                        name = StringUtil.getUserNameString(showBothName, user_name, name_show),
                        avatarUrl = StringUtil.getAvatarUrl(portrait)
                    )
                }
            )
        }
    }

    suspend fun likeForum(forum: ForumData): ForumData {
        require(!forum.liked)
        val info = networkDataSource.like(forum.id, forum.name, forum.tbs!!)

        // Notify forum changes to home
        homeRepo.onLikeForum()
        return forum.copy(
            liked = true,
            level = info.levelId.toInt(),
            levelName = info.levelName,
            score = info.curScore.toInt(),
            scoreLevelUp = info.levelUpScore.toInt(),
            members = info.memberSum.toInt()
        )
    }

    suspend fun dislikeForum(forum: ForumData) {
        networkDataSource.dislike(forum.id, forum.name, forum.tbs!!)
        // Notify forum changes to home
        homeRepo.onDislikeForum(forumId = forum.id)
    }

    suspend fun forumSignIn(forumId: Long, forumName: String, tbs: String): SignResultBean.UserInfo {
        val userInfo = networkDataSource.forumSignIn(forumId, forumName, tbs)
        homeRepo.onForumSignedIn(forumId)
        return userInfo
    }

    companion object {
        private const val TAG = "ForumRepository"

        private const val MAX_FORUM_CACHE_SIZE = 2

        private const val MAX_GENERAL_THREAD_CACHE_SIZE = 10

        private inline val ForumPageResult.threadList
            get() = second

        private data class ForumCache(
            val forum: ForumData,
            val managers: List<ForumManager>?,
            val normal: ThreadItemList?,
            val good: ThreadItemList?
        ) {
            fun getItemsByType(isGood: Boolean): ThreadItemList? = if (isGood) good else normal
        }
    }
}

// Note from zzc10086: 像视频,合辑这种需要特殊适配的列表目前做屏蔽处理
private fun List<FrsTabInfo>?.filterNavTab(): List<FrsTabInfo> = if (!isNullOrEmpty()) {
    filter { tab ->
        tab.isGeneralTab == 1 && tab.tabType == 15
    }
} else {
    emptyList()
}

private suspend fun List<ThreadInfo>.mapUiModel(
    blockedSetting: BlockSettings,
    showBothName: Boolean,
    isBlocked: suspend (uid: Long, content: Array<String>) -> Boolean,
): List<ThreadItem> {
    return if (isNotEmpty()) {
        withContext(Dispatchers.Default) {
            mapNotNull {
                val notBlocked = !blockedSetting.blockVideo || it.videoInfo == null
                if (notBlocked) it.mapUiModel(showBothName, isBlocked, threadDislikeMap = null) else null
            }
            .distinctById()
        }
    } else {
        emptyList()
    }
}

// Map FrsPageResponseData.ForumInfo to UI Model
private fun FrsPageResponseData.toData(): ForumData = forum!!.let {
    ForumData(
        id = it.id,
        avatar = it.avatar,
        name = it.name,
        forumRuleTitle = forum_rule?.run {
            title.takeIf { t -> has_forum_rule == 1 && t.isNotEmpty() }
        },
        slogan = forum!!.slogan.trim().takeUnless { slogan -> slogan.isEmpty() },
        tbs = anti?.tbs?.takeUnless { tbs -> tbs.isEmpty() || tbs.isBlank() },
        liked = it.is_like == 1,
        signed = it.sign_in_info?.user_info?.is_sign_in == 1,
        signedDays = it.sign_in_info?.user_info?.cont_sign_num ?: 0,
        signedRank = it.sign_in_info?.user_info?.user_sign_rank ?: 0,
        level = it.user_level,
        levelName = it.level_name,
        score = it.cur_score,
        scoreLevelUp = it.levelup_score,
        members = it.member_num,
        threads = it.thread_num,
        posts = it.post_num,
        goodClassifies = it.good_classify
            .takeUnless { c -> c.size <= 1 }
            ?.map { c -> GoodClassify(c.class_name, c.class_id) },
        navTabInfo = nav_tab_info?.tab.filterNavTab(),
    )
}

private fun FrsPageResponseData.getManagers(): List<ForumManager>? {
    return forum
        ?.managers
        ?.takeUnless { it.isEmpty() }
        ?.map {
            ForumManager(
                id = it.id,
                name = StringUtil.getUserNameString(showBoth = false, it.name, it.show_name),
                avatarUrl = StringUtil.getAvatarUrl(it.portrait)
            )
        }
}
