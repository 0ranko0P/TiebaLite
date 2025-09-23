package com.huanchengfly.tieba.post.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import com.google.common.base.Preconditions.checkArgument
import com.google.common.net.InternetDomainName
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.api.urlDecode
import com.huanchengfly.tieba.post.arch.ControlledRunner
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector.checkClipBoard
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil.Icon
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil.PreviewInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.intellij.lang.annotations.RegExp
import java.util.regex.Pattern

sealed class ClipBoardLink(val url: String) {

    class Forum(url: String, val forumName: String) : ClipBoardLink(url)

    class Thread(url: String, val threadId: Long) : ClipBoardLink(url)

    // Convert ClipBoardLink to Navigation Route
    fun toRoute(avatarUrl: String? = null): Destination {
        return when (this) {
            is Forum -> Destination.Forum(forumName = forumName, avatar = avatarUrl)

            is Thread -> Destination.Thread(threadId = threadId)
        }
    }
}

/**
 * 检测剪贴板中的贴吧链接.
 *
 * 由于Android Q 引入的隐私限制, 仅当界面拥有焦点时读取剪贴板.
 *
 * @see checkClipBoard
 * @see [android.app.Activity.onWindowFocusChanged]
 * */
object ClipBoardLinkDetector {

    private val pattern by lazy {
        Pattern.compile(
            @RegExp
            "((http|https)://)(([a-zA-Z0-9._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9&%_./-~-]*)?"
        )
    }

    val clipBoardManager by lazy {
        App.INSTANCE.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    private val _previewInfoStateFlow = MutableStateFlow<PreviewInfo?>(null)
    val previewInfoStateFlow: StateFlow<PreviewInfo?> = _previewInfoStateFlow.asStateFlow()

    private val previewTaskRunner = ControlledRunner<Unit>()

    @Volatile
    private var lastClipBoardHash: Int = -1

    private fun parseLink(url: String): ClipBoardLink? {
        val uri = Uri.parse(url)
        if (uri.isTieba()) {
            QuickPreviewUtil.getForumName(uri)?.let {
                return ClipBoardLink.Forum(url, forumName = it)
            }

            QuickPreviewUtil.getThreadId(uri)?.let {
                return ClipBoardLink.Thread(url, threadId = it)
            }
        }

        return null
    }

    fun parseDeepLink(uri: Uri): ClipBoardLink? {
        return if (uri.scheme == "com.baidu.tieba" && uri.host == "unidispatch") {
            when (uri.path.orEmpty().lowercase()) {
                "/frs" -> {
                    val forumName = uri.getQueryParameter("kw") ?: return null
                    return ClipBoardLink.Forum("https://tieba.baidu.com/f?kw=$forumName", forumName)
                }

                "/pb" -> {
                    val threadId = uri.getQueryParameter("tid") ?: return null
                    return ClipBoardLink.Forum("https://tieba.baidu.com/p/$threadId", threadId)
                }

                else -> null
            }
        } else {
            parseLink(uri.toString().urlDecode())
        }
    }

    suspend fun checkClipBoard(context: Context, forumRepo: ForumRepository, threadRepo: PbPageRepository) {
        val clipBoardText = getClipBoardText()
        if (clipBoardText == null || lastClipBoardHash == clipBoardText.hashCode()) return

        previewTaskRunner.cancelPreviousThenRun {
            val clipBoardLink = withContext(Dispatchers.Default) {
                val matcher = pattern.matcher(clipBoardText)
                if (matcher.find()) parseLink(url = matcher.group()) else null
            }
            if (clipBoardLink == null) {
                clear()
                return@cancelPreviousThenRun
            }

            lastClipBoardHash = clipBoardText.hashCode()
            val title = when(clipBoardLink) {
                is ClipBoardLink.Forum -> clipBoardLink.forumName
                is ClipBoardLink.Thread -> clipBoardLink.url
            }
            _previewInfoStateFlow.update {
                PreviewInfo(
                    clipBoardLink = clipBoardLink,
                    title = title,
                    subtitle = context.getString(R.string.subtitle_link),
                    icon = Icon(R.drawable.ic_link)
                )
            }
            runCatching {
                QuickPreviewUtil.loadPreviewInfo(context, clipBoardLink, forumRepo, threadRepo)
            }
            .onSuccess { preview ->
                yield()
                _previewInfoStateFlow.update { preview }
            }
            .onFailure { e ->
                yield()
                _previewInfoStateFlow.update {
                    PreviewInfo(clipBoardLink, title, e.getErrorMessage(), Icon(R.drawable.ic_error))
                }
            }
        }
    }

    fun onCopyTiebaLink(link: String) {
        clear()
        lastClipBoardHash = link.hashCode()
    }

    fun getClipBoardText(): String? {
        val data = clipBoardManager.primaryClip ?: return null
        val item = data.getItemAt(0)
        return item?.text?.toString()
    }

    // Return ture: 百度短链接
    private fun InternetDomainName.isBaiduShortLink(): Boolean = parts().first().let {
        it == "mr" || it == "mbd" || it == "t" || it == "rh" || (it.first() == 'm' && it.getOrNull(1)?.isDigit() == true)
    }

    fun Uri.isBaidu(): Boolean = try {
        checkArgument(this.isHttp())
        with(InternetDomainName.from(host!!)) {
            when {
                isTopPrivateDomain -> "baidu.com" == host

                isUnderPublicSuffix -> {
                    "baidu.com" == parent().toString() && !isBaiduShortLink() && path?.endsWith("checkurl") != true
                }

                else -> false
            }
        }
    } catch (_: Exception) {
        false
    }

    fun Uri.isHttp(): Boolean = scheme?.startsWith("http", ignoreCase = true) == true

    fun Uri.isTieba(): Boolean {
        return host != null && (host.equals("wapp.baidu.com", ignoreCase = true) ||
                host.equals("tieba.baidu.com", ignoreCase = true) ||
                host.equals("tiebac.baidu.com", ignoreCase = true))
                && queryParameterNames?.contains("tbjump") != true // Exclude Tieba redirect
    }

    fun Uri.isLogin(): Boolean = path?.let { p ->
        host.equals("wappass.baidu.com") && p.startsWith("/passport") && queryParameterNames.contains("login")
    } == true

    fun clear() {
        previewTaskRunner.cancelCurrent()
        _previewInfoStateFlow.update { null }
    }
}