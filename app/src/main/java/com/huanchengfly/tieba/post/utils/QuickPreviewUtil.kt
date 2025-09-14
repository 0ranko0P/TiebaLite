package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.components.ClipBoardLink
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.PbPageRepository

object QuickPreviewUtil {
    fun getThreadId(uri: Uri): Long? {
        val path = uri.path?: return null
        if (uri.host == null || path.isEmpty()) return null

        if (path.equals("/f", ignoreCase = true) || path.equals("/mo/q/m", ignoreCase = true)) {
            return uri.getQueryParameter("kz")?.toLongOrNull()
        }

        if (path.startsWith("/p/")) {
            return runCatching {
                val paramIndex = path.indexOf('&').takeUnless { it == -1 } ?: path.length
                path.substring(3, paramIndex).toLong()
            }.getOrNull()
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

    private suspend fun loadThreadPreview(context: Context, link: ClipBoardLink.Thread): PreviewInfo {
        val response = PbPageRepository
            .pbPage(link.threadId)
            .firstOrThrow()
        val data = response.data_ ?: throw TiebaException(response.error?.error_msg)
        val forumName = data.forum?.name ?: "?"
        val replies = data.thread?.replyNum ?: 0

        return PreviewInfo(
            clipBoardLink = link,
            title = data.thread?.title,
            subtitle = context.getString(R.string.subtitle_quick_preview_thread, forumName, replies),
            icon = data.thread?.author?.portrait?.let { Icon(StringUtil.getAvatarUrl(it)) }
        )
    }

    private suspend fun loadForumPreview(
        context: Context,
        link: ClipBoardLink.Forum,
        forumRepo: ForumRepository
    ): PreviewInfo {
        val info = forumRepo.loadForumInfo(forumName = link.forumName, forceNew = false)
        return PreviewInfo(
            clipBoardLink = link,
            title = context.getString(R.string.title_forum, link.forumName),
            subtitle = info.slogan.orEmpty(),
            icon = Icon(url = info.avatar)
        )
    }

    @Throws(TiebaException::class, NoConnectivityException::class)
    suspend fun loadPreviewInfo(
        context: Context,
        clipBoardLink: ClipBoardLink,
        forumRepo: ForumRepository
    ): PreviewInfo {
        return when (clipBoardLink) {
            is ClipBoardLink.Forum -> loadForumPreview(context, clipBoardLink, forumRepo)
            is ClipBoardLink.Thread -> loadThreadPreview(context, clipBoardLink)
        }
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