package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.ClipBoardLink
import com.huanchengfly.tieba.post.repository.FrsPageRepository
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.ui.page.forum.ForumViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

object QuickPreviewUtil {
    fun getThreadId(uri: Uri): Long? {
        val path = uri.path?: return null
        if (uri.host == null || path.isEmpty()) return null

        if (path.equals("/f", ignoreCase = true) || path.equals("/mo/q/m", ignoreCase = true)) {
            return uri.getQueryParameter("kz")?.toLongOrNull()
        }
        if (path.startsWith("/p/")) {
            return runCatching { path.substring(3).toLong() }.getOrNull()
        }
        return null
    }

    fun getForumName(uri: Uri): String? {
        val path = uri.path?: return null
        if (uri.host == null || path.isEmpty()) return null

        val word = uri.getQueryParameter("word")
        if (path.equals("/f", ignoreCase = true) || path.equals("/mo/q/m", ignoreCase = true)) {
            val kw = uri.getQueryParameter("kw")
            if (kw != null) return kw
            if (word != null) return word
        }
        return null
    }

    private fun getThreadPreviewInfoFlow(
        context: Context,
        link: ClipBoardLink.Thread,
    ): Flow<PreviewInfo> =
        PbPageRepository
            .pbPage(link.threadId)
            .map {
                PreviewInfo(
                    clipBoardLink = link,
                    title = it.data_?.thread?.title,
                    subtitle = context.getString(
                        R.string.subtitle_quick_preview_thread,
                        it.data_?.forum?.name,
                        it.data_?.thread?.replyNum?.toString()
                    ),
                    icon = Icon(StringUtil.getAvatarUrl(it.data_?.thread?.author?.portrait))
                )
            }

    private fun getForumPreviewInfoFlow(
        context: Context,
        link: ClipBoardLink.Forum
    ): Flow<PreviewInfo> =
        ForumViewModel.getSortType(context, link.forumName)
            .map { sortType ->
                val response = FrsPageRepository.frsPage(link.forumName, 1, 1, sortType).first()
                val data = requireNotNull(response.data_)
                PreviewInfo(
                    clipBoardLink = link,
                    title = context.getString(R.string.title_forum, link.forumName),
                    subtitle = data.forum?.slogan,
                    icon = data.forum?.avatar?.let { Icon(it) }
                )
            }

    fun getPreviewInfoFlow(context: Context, clipBoardLink: ClipBoardLink): Flow<PreviewInfo?> {
        val detailFlow = when (clipBoardLink) {
            is ClipBoardLink.Forum -> getForumPreviewInfoFlow(context, clipBoardLink)
            is ClipBoardLink.Thread -> getThreadPreviewInfoFlow(context, clipBoardLink)
            else -> throw RuntimeException("Not implement")
        }
        val flow = flowOf(
            PreviewInfo(
                clipBoardLink = clipBoardLink,
                title = clipBoardLink.url,
                subtitle = context.getString(R.string.subtitle_link),
                icon = Icon(R.drawable.ic_link)
            )
        )
        return merge(flow, detailFlow)
    }

    @Immutable
    data class PreviewInfo(
        val clipBoardLink: ClipBoardLink,
        val title: String? = null,
        val subtitle: String? = null,
        val icon: Icon? = null,
    )

    @Immutable
    data class Icon(
        val type: Int,
        val url: String? = null,
        @DrawableRes
        val res: Int = 0,
    ) {

        constructor(url: String?) : this(
            type = TYPE_URL,
            url = url
        )

        constructor(@DrawableRes res: Int) : this(
            type = TYPE_DRAWABLE_RES,
            res = res
        )

        companion object {
            const val TYPE_DRAWABLE_RES = 0
            const val TYPE_URL = 1
        }
    }
}