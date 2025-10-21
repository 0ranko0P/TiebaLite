package com.huanchengfly.tieba.post.repository

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.util.fastMap
import com.huanchengfly.tieba.post.api.models.SearchForumBean.ForumInfoBean
import com.huanchengfly.tieba.post.api.models.SearchThreadBean.MediaInfo.Companion.TYPE_PICTURE
import com.huanchengfly.tieba.post.api.models.SearchThreadBean.MediaInfo.Companion.TYPE_VIDEO
import com.huanchengfly.tieba.post.api.models.SearchThreadBean.ThreadInfoBean
import com.huanchengfly.tieba.post.api.models.SearchUserBean.UserBean
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import com.huanchengfly.tieba.post.models.database.dao.SearchDao
import com.huanchengfly.tieba.post.models.database.dao.SearchPostDao
import com.huanchengfly.tieba.post.repository.source.network.SearchNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_USER
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.search.SearchForum
import com.huanchengfly.tieba.post.ui.models.search.SearchMedia
import com.huanchengfly.tieba.post.ui.models.search.SearchSuggestion
import com.huanchengfly.tieba.post.ui.models.search.SearchThreadInfo
import com.huanchengfly.tieba.post.ui.models.search.SearchUser
import com.huanchengfly.tieba.post.ui.widgets.compose.buildThreadContent
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

// HasMore, Threads
typealias SearchThreadResult = Pair<Boolean, List<SearchThreadInfo>>

data class SearchResult<T>(
    val exactMatch: T?,
    val fuzzyMatch: List<T>
)

@Singleton
class SearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyDao: SearchDao,
    private val postHistoryDao: SearchPostDao,
    private val settingsRepo: SettingsRepository
) {

    private val networkDataSource = SearchNetworkDataSource

    suspend fun searchForum(keyword: String): SearchResult<SearchForum> {
        val data = networkDataSource.searchForum(keyword)
        return mapSearchResult(
            exactMatch = data.exactMatch,
            fuzzyMatch = data.fuzzyMatch,
            mapUiModel = ::mapUiModel
        )
    }

    suspend fun searchSuggestions(keyword: String): SearchSuggestion {
        val data = networkDataSource.searchSuggestions(keyword, searchForum = false)
        currentCoroutineContext().ensureActive()
        val suggestForum = data.forum_card?.run {
            SearchForum(id = forum_id, forum_name, avatar, slogan = slogan)
        }
        return SearchSuggestion(suggestForum, data.list)
    }

    suspend fun searchPost(
        keyword: String,
        forumName: String,
        forumId: Long,
        sortType: Int,
        filterType: Int,
        page: Int
    ): SearchThreadResult {
        val data = networkDataSource.searchPost(keyword, forumName, forumId, sortType, filterType, page)
        val habitSettings = settingsRepo.habitSettings.flow.first()
        val posts = data.postList.mapUiModel(keyword, context, habitSettings.showBothName)
        return SearchThreadResult(data.hasMore == 1, posts)
    }

    suspend fun searchThread(keyword: String, page: Int, sortType: Int): SearchThreadResult {
        val data = networkDataSource.searchThread(keyword, page, sortType)
        val habitSettings = settingsRepo.habitSettings.flow.first()
        val threads = data.postList.mapUiModel(keyword, context, habitSettings.showBothName)
        return SearchThreadResult(data.hasMore == 1, threads)
    }

    suspend fun searchUser(keyword: String): SearchResult<SearchUser> {
        val data = networkDataSource.searchUser(keyword)
        // Map to UI Model
        return mapSearchResult(
            exactMatch = data.exactMatch,
            fuzzyMatch = data.fuzzyMatch,
            mapUiModel = ::mapUiModel
        )
    }

    suspend fun addHistory(keyword: String) {
        require(keyword.isNotBlank() && keyword.isNotEmpty()) { "Invalid search keyword" }
        historyDao.upsert(SearchHistory(keyword))
    }

    suspend fun clearHistory() = historyDao.deleteAll()

    suspend fun deleteHistory(history: String): Boolean {
        return historyDao.deleteById(history.hashCode()) == 1
    }

    fun getHistoryFlow(): Flow<List<String>> = historyDao.observeAllKeywords()

    suspend fun addPostHistory(forumId: Long, keyword: String) {
        require(keyword.isNotBlank() && keyword.isNotEmpty()) { "Invalid search keyword" }
        require(forumId > 0) { "Invalid forum ID $forumId" }
        postHistoryDao.upsert(SearchPostHistory(forumId, keyword))
    }

    suspend fun clearPostHistory(forumId: Long): Int {
        return postHistoryDao.deleteAll(forumId)
    }

    suspend fun deletePostHistory(forumId: Long, history: String): Boolean {
        return postHistoryDao.delete(forumId, history) == 1
    }

    fun getPostHistoryFlow(forumId: Long): Flow<List<String>> {
        return postHistoryDao.observeAllKeywords(forumId)
    }

    companion object {

        private suspend inline fun <NetModel, UiModel> mapSearchResult(
            exactMatch: NetModel?,
            fuzzyMatch: List<NetModel>?,
            crossinline mapUiModel: (NetModel) -> UiModel
        ): SearchResult<UiModel> {
            return withContext(Dispatchers.Default) {
                SearchResult(
                    exactMatch = exactMatch?.let { mapUiModel(it) },
                    fuzzyMatch = if (fuzzyMatch.isNullOrEmpty()) {
                        emptyList()
                    } else {
                        fuzzyMatch.fastMap(mapUiModel)
                    }
                )
            }
        }

        /**
         * Map ForumInfo to UI Model
         * */
        private fun mapUiModel(info: ForumInfoBean) = with(info) {
            SearchForum(
                id = info.forumId!!,
                name = forumName ?: forumNameShow.orEmpty(),
                avatar = avatar.orEmpty(),
                slogan = slogan?.takeUnless { it.isEmpty() || it.isBlank() }
            )
        }

        /**
         * Map UserBean to UI Model
         * */
        private fun mapUiModel(user: UserBean): SearchUser = with(user) {
            val nickname = (showNickname ?: userNickname)!!
            val userName = name?.takeUnless { it == nickname }
            // show both nickname and username if possible
            val formattedName = if (userName.isNullOrEmpty()) nickname else "$nickname ($userName)"
            return SearchUser(
                id = id?.toLong()?.takeUnless { it <= -1 } ?: throw TiebaException("Illegal User ID $id, $userNickname"),
                avatar = StringUtil.getAvatarUrl(portrait!!),
                formattedName = formattedName,
                intro = intro
            )
        }

        @WorkerThread
        private fun buildHighlightContent(content: CharSequence, patterns: List<Pattern>): AnnotatedString {
            return buildAnnotatedString {
                val colorScheme = ThemeUtil.currentColorScheme()
                val highlightStyle = SpanStyle(colorScheme.primary, fontWeight = FontWeight.Bold)
                append(content)

                var matcher: Matcher
                patterns.forEach { regexPattern ->
                    matcher = regexPattern.matcher(content)
                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        addStyle(highlightStyle, start, end)
                    }
                }
            }
        }

        @WorkerThread
        private fun buildAnnotatedString(author: Author, content: String) = buildAnnotatedString {
            val colorScheme = ThemeUtil.currentColorScheme()
            if (author.id == -1L) { // 用户已注销
                withStyle(SpanStyle(colorScheme.primary, textDecoration = TextDecoration.LineThrough)) {
                    append(author.name)
                }
            } else {
                withAnnotation(tag = TAG_USER, annotation = author.id.toString()) {
                    withStyle(SpanStyle(color = colorScheme.primary)) {
                        append("@")
                        append(author.name)
                    }
                }
            }
            append(": ")
            append(content)
        }

        /**
         * Map thread info to UI Model.
         *
         * @param keyword search keyword
         * @param context application context
         * @param showBothName show both username and nickname
         * */
        private suspend fun List<ThreadInfoBean>.mapUiModel(
            keyword: String,
            context: Context,
            showBothName: Boolean
        ): List<SearchThreadInfo> = withContext(Dispatchers.Default) {
            val keywords = if (keyword.isNotEmpty()) {
                keyword.split(" ").map { it.toPattern(Pattern.CASE_INSENSITIVE) }
            } else {
                emptyList()
            }

            map { info ->
                val author = with(info.user) {
                    Author(
                        id = userId,
                        name = if (userId != -1L) {
                            StringUtil.getUserNameString(showBothName, userName, showNickname)
                        } else {
                            "@用户已注销"
                        },
                        avatarUrl = StringUtil.getAvatarUrl(portrait)
                    )
                }

                val postHighlightContent: AnnotatedString? = info.postInfo?.run {
                    buildHighlightContent(
                        content = buildAnnotatedString(author, content),
                        patterns = keywords
                    )
                }

                val mainPostTitleHighlight: AnnotatedString? = info.mainPost?.run {
                    buildHighlightContent(
                        content = buildAnnotatedString(author, title),
                        patterns = keywords
                    )
                }

                val mainPostHighlight: AnnotatedString? = info.mainPost?.run {
                    content.takeUnless { it.isEmpty() }?.emoticonString
                }

                val threadHighlightContent: AnnotatedString = buildHighlightContent(
                    content = buildThreadContent(
                        title = info.title.takeIf { info.mainPost == null },
                        abstractText = info.content
                    ),
                    patterns = keywords
                )

                val video: SearchMedia.Video? = info.media?.getOrNull(0)?.let {
                    if (it.type == TYPE_VIDEO) SearchMedia.Video(it) else null
                }

                val pictures: List<SearchMedia.Picture>? = info.media?.mapNotNull {
                    if (it.type == TYPE_PICTURE) SearchMedia.Picture(it) else null
                }

                SearchThreadInfo(
                    tid = info.tid,
                    pid = info.pid,
                    cid = info.cid,
                    author = author,
                    content = threadHighlightContent,
                    pictures = pictures?.takeUnless { it.isEmpty() || video != null/* can't fit both */},
                    video = video,
                    forumInfo = info.forumInfo,
                    mainPostTitle = mainPostTitleHighlight,
                    mainPostContent = mainPostHighlight,
                    postInfoContent = postHighlightContent,
                    timeDesc = DateTimeUtils.getRelativeTimeString(context, timestamp = info.time)
                )
            }
        }
    }
}