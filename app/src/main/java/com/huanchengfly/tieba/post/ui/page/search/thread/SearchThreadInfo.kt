package com.huanchengfly.tieba.post.ui.page.search.thread

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.api.models.SearchThreadBean.MediaInfo
import com.huanchengfly.tieba.post.api.models.SearchThreadBean.MediaInfo.Companion.TYPE_PICTURE
import com.huanchengfly.tieba.post.api.models.SearchThreadBean.MediaInfo.Companion.TYPE_VIDEO
import com.huanchengfly.tieba.post.api.models.SearchThreadBean.ThreadInfoBean
import com.huanchengfly.tieba.post.api.models.SearchThreadBean.UserInfoBean
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadInfo.Companion.getSearchThreadInfoList
import com.huanchengfly.tieba.post.ui.widgets.compose.buildThreadContent
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.StringUtil.getUserNameString
import com.huanchengfly.tieba.post.utils.ThemeUtil
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Represents [SearchThreadBean.ThreadInfoBean] in UI
 *
 * This class holds contents with highlighted search keywords, to avoid compiling regex patterns or
 * build highlighted content during the compose.
 *
 * @see getSearchThreadInfoList
 *
 * @param tid [SearchThreadBean.ThreadInfoBean.tid]
 * @param pid [SearchThreadBean.ThreadInfoBean.pid]
 * @param cid [SearchThreadBean.ThreadInfoBean.cid]
 * @param content [SearchThreadBean.ThreadInfoBean.content] with highlighted search keywords
 * @param pictures list of picture
 * @param video video
 * @param forumInfo [SearchThreadBean.ForumInfo]
 * @param mainPostTitle title content of [SearchThreadBean.MainPost] with highlighted search keywords
 * @param mainPostContent content of [SearchThreadBean.MainPost] with highlighted search keywords
 * @param postInfoContent content of [SearchThreadBean.PostInfo] with highlighted search keywords
 * @param postText  formatted [SearchThreadBean.ThreadInfoBean.postNum]
 * @param likeText  formatted [SearchThreadBean.ThreadInfoBean.likeNum]
 * @param shareText formatted [SearchThreadBean.ThreadInfoBean.shareNum]
 * @param userId user id, -1 when this user deregistered
 * @param userName formatted user display name of [SearchThreadBean.ThreadInfoBean.user]
 * @param userAvatarUrl user avatar of [SearchThreadBean.ThreadInfoBean.user]
 * @param timeDesc formatted time
 * */
@Immutable
/*data */class SearchThreadInfo(
    val tid: Long,
    val pid: Long,
    val cid: Long,
    val content: AnnotatedString,
    val pictures: List<SearchMedia.Picture>?,
    val video: SearchMedia.Video?,
    val forumInfo: SearchThreadBean.ForumInfo,
    val mainPostTitle: AnnotatedString? = null,
    val mainPostContent: AnnotatedString? = null,
    val postInfoContent: AnnotatedString? = null,
    val postText: String,
    val likeText: String,
    val shareText: String,
    val userId: Long,
    val userName: String,
    val userAvatarUrl: String,
    val timeDesc: String
) {
    companion object {

        private fun MediaInfo.getPixelSize(): IntSize? {
            // 一些古老视频的尺寸为 null, 一些古老图片的尺寸为 0
            return if (width != null && height != null && width != 0 && height != 0) {
                IntSize(width = width, height = height)
            } else {
                null
            }
        }

        sealed class SearchMedia(val url: String, val dimensions: IntSize?) {
            class Picture(media: MediaInfo): SearchMedia(
                url = with(media) { bigPic ?: smallPic ?: waterPic ?: "" },
                dimensions = media.getPixelSize()
            )

            class Video(media: MediaInfo) : SearchMedia(media.vhsrc ?: "", media.getPixelSize()) {
                val thumbnail: String = media.vpic ?: ""
            }

            fun aspectRatio(): Float = dimensions?.let { it.width.toFloat() / it.height } ?: 2.0f
        }

        private fun UserInfoBean.buildAnnotatedString(content: String, context: Context): AnnotatedString {
            val colorScheme = ThemeUtil.currentColorScheme()
            return buildAnnotatedString {
                if (userId == -1L) {
                    withStyle(SpanStyle(colorScheme.primaryContainer, textDecoration = TextDecoration.LineThrough)) {
                        append("@用户已注销")
                    }
                } else {
                    withAnnotation(tag = "user", annotation = userId.toString()) {
                        withStyle(SpanStyle(color = colorScheme.primaryContainer)) {
                            append("@")
                            append(getUserNameString(context, userName, showNickname))
                        }
                    }
                }
                append(": ")
                append(content)
            }
        }

        private fun buildHighlightContent(content: CharSequence, patterns: List<Pattern>): AnnotatedString {
            val colorScheme = ThemeUtil.currentColorScheme()
            return buildAnnotatedString {
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

        /**
         * Convert [ThreadInfoBean] list to [SearchThreadInfo] list
         *
         * @param keyword search keyword, the content of [ThreadInfoBean] will be highlighted by it
         * @param context Context to get user preference
         * */
        @WorkerThread
        fun List<ThreadInfoBean>.getSearchThreadInfoList(
            keyword: String,
            context: Context
        ): List<SearchThreadInfo> {
            val keywords = if (keyword.isNotEmpty()) {
                keyword.split(" ").map { it.toPattern(Pattern.CASE_INSENSITIVE) }
            } else {
                emptyList()
            }

            return map { info ->
                val postHighlightContent: AnnotatedString? = info.postInfo?.run {
                    buildHighlightContent(
                        content = user.buildAnnotatedString(content = content, context = context),
                        patterns = keywords
                    )
                }

                val mainPostTitleHighlight: AnnotatedString? = info.mainPost?.run {
                    buildHighlightContent(
                        content = user.buildAnnotatedString(content = title, context = context),
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
                    content = threadHighlightContent,
                    pictures = pictures?.takeUnless { it.isEmpty() || video != null/* can't fit both */ },
                    video = video,
                    forumInfo = info.forumInfo,
                    mainPostTitle = mainPostTitleHighlight,
                    mainPostContent = mainPostHighlight,
                    postInfoContent = postHighlightContent,
                    postText = if (info.postNum == 0) {
                        context.getString(R.string.title_reply)
                    } else {
                        info.postNum.getShortNumString()
                    },
                    likeText = if (info.likeNum == 0L) {
                        context.getString(R.string.title_agree)
                    } else {
                        info.likeNum.getShortNumString()
                    },
                    shareText = if (info.shareNum == 0L) {
                        context.getString(R.string.title_share)
                    } else {
                        info.shareNum.getShortNumString()
                    },
                    userId = info.user.userId,
                    userName = with(info.user) {
                        if (userId != -1L) getUserNameString(context, userName, showNickname) else userName
                    },
                    userAvatarUrl = StringUtil.getAvatarUrl(info.user.portrait),
                    timeDesc = DateTimeUtils.getRelativeTimeString(context, info.time)
                )
            }
        }
    }
}
