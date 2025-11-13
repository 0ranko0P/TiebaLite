package com.huanchengfly.tieba.post.repository

import android.content.Context
import androidx.collection.LruCache
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.huanchengfly.tieba.post.api.models.SignResultBean
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponseData
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.distinctById
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.mapUiModel
import com.huanchengfly.tieba.post.repository.source.network.ForumNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.ThreadItemList
import com.huanchengfly.tieba.post.ui.models.forum.ForumData
import com.huanchengfly.tieba.post.ui.models.forum.ForumDetail
import com.huanchengfly.tieba.post.ui.models.forum.ForumManager
import com.huanchengfly.tieba.post.ui.models.forum.ForumRule
import com.huanchengfly.tieba.post.ui.models.forum.GoodClassify
import com.huanchengfly.tieba.post.ui.models.forum.Rule
import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.utils.StringUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private typealias CacheKey = String

private typealias ForumPageResult  = Triple<ForumData, ThreadItemList, List<ForumManager>?>

private data class ForumCache(
    val forum: ForumData,
    val managers: List<ForumManager>?,
    val normal: ThreadItemList?,
    val good: ThreadItemList?
) {
    fun getItemsByType(isGood: Boolean): ThreadItemList? = if (isGood) good else normal
}

@Singleton
class ForumRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val blockRepo: BlockRepository,
    private val settingsRepo: SettingsRepository,
    private val homeRepo: HomeRepository
) {

    private val networkDataSource = ForumNetworkDataSource

    private val blockedSettings: Flow<BlockSettings>
        get() = settingsRepo.blockSettings.flow

    private val habitSettings: Flow<HabitSettings>
        get() = settingsRepo.habitSettings.flow

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(name = "forum_preferences")
        }
    }

    private val cache: LruCache<CacheKey, ForumCache> = LruCache(2)

    private suspend fun frsPage(
        forumName: String,
        page: Int,
        loadType: Int,
        sortType: Int,
        goodClassifyId: Int?,
        forceNew: Boolean = false
    ): ForumPageResult {
        var key: CacheKey? = null
        var cached: ForumCache? = null
        val cacheable = if (sortType == -1) (goodClassifyId ?: 0) == 0 else sortType == ForumSortType.BY_REPLY

        // Load first page from lru cache if possible
        if (page == 1 && cacheable && loadType == 1) {
            key = forumName
            cached = cache[key]
            val typedItemList = cached?.getItemsByType(isGood = goodClassifyId != null)
            if (!forceNew && typedItemList != null) {
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
            forumManagers = data.getManagers(habit = habitSettings.first())
            val normalThreads = if (sortType != -1) typedThreads else cached?.normal
            val goodThreads = if (sortType == -1) typedThreads else cached?.good
            cache.put(key, ForumCache(forumData, forumManagers, normal = normalThreads, good = goodThreads))
        }
        return ForumPageResult(forumData, typedThreads, forumManagers)
    }

    suspend fun loadForumInfo(forumName: String, forceNew: Boolean = true): ForumData {
        val sortType = getSortType(forumName).first()
        return frsPage(forumName, page = 1, loadType = 1, sortType, null, forceNew).first
    }

    suspend fun loadForumDetail(forumName: String): ForumDetail {
        val (forumData, _, managers) = frsPage(forumName, page = 1, loadType = 1, sortType = -1, null)
        val detail = networkDataSource.loadForumDetail(forumData.id)

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
    ).second

    suspend fun loadGoodPage(forum: String, page: Int, goodClassifyId: Int?, forceNew: Boolean): ThreadItemList = frsPage(
        forumName = forum,
        page = page,
        loadType = 1,
        sortType = -1,
        goodClassifyId = goodClassifyId ?: 0,
        forceNew = forceNew
    ).second

    suspend fun loadMorePage(forum: String, page: Int, sortType: Int): ThreadItemList = frsPage(
        forumName = forum,
        page = page,
        loadType = 2,
        sortType = sortType,
        goodClassifyId = null,
        forceNew = false
    ).second

    suspend fun loadMoreGood(forum: String, page: Int, goodClassifyId: Int?): ThreadItemList = frsPage(
        forumName = forum,
        page = page,
        loadType = 2,
        sortType = -1,
        goodClassifyId = goodClassifyId ?: 0,
        forceNew = false
    ).second

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
        return networkDataSource.forumSignIn(forumId, forumName, tbs)
    }

    /**
     * Sort preference per forum
     *
     * @see [ForumSortType]
     * */
    fun getSortType(forumName: String): Flow<Int> {
        return dataStore.data
            // Note: No need to track habit changes
            .map { it[forumName.sortKey] ?: habitSettings.first().forumSortType }
    }

    suspend fun saveSortType(forumName: String, @ForumSortType sortType: Int) {
        // Default from app_preference
        val default: Int = habitSettings.first().forumSortType
        dataStore.edit {
            if (sortType == default) { // Keep DataStore clean
                it.remove(key = forumName.sortKey)
            } else {
                it[forumName.sortKey] = sortType
            }
        }
    }

    private val String.sortKey: Preferences.Key<Int>
        get() = intPreferencesKey("${hashCode()}_sort")
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
        slogan = forum.slogan.trim().takeUnless { slogan -> slogan.isEmpty() },
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
            ?.map { c -> GoodClassify(c.class_name, c.class_id) }
    )
}

private fun FrsPageResponseData.getManagers(habit: HabitSettings): List<ForumManager>? {
    return forum
        ?.managers
        ?.takeUnless { it.isEmpty() }
        ?.map {
            ForumManager(
                id = it.id,
                name = StringUtil.getUserNameString(habit.showBothName, it.name, it.show_name),
                avatarUrl = StringUtil.getAvatarUrl(it.portrait)
            )
        }
}
