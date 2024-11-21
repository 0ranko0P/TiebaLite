package com.huanchengfly.tieba.post.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.urlDecode
import com.huanchengfly.tieba.post.arch.ControlledRunner
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
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

            else -> throw RuntimeException("Not implemented ${this::class.simpleName}!")
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

    private val mutablePreviewInfoStateFlow = MutableStateFlow<QuickPreviewUtil.PreviewInfo?>(null)
    val previewInfoStateFlow
        get() = mutablePreviewInfoStateFlow.asStateFlow()

    private val previewTaskRunner = ControlledRunner<Unit>()

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

    fun checkClipBoard(owner: LifecycleOwner, context: Context) {
        val clipBoardText = getClipBoardText()
        if (clipBoardText == null || lastClipBoardHash == clipBoardText.hashCode()) return

        owner.lifecycleScope.launch(Dispatchers.IO) {
            previewTaskRunner.cancelPreviousThenRun {
                val matcher = pattern.matcher(clipBoardText)
                if (matcher.find()) {
                    val link = parseLink(url = matcher.group()) ?: return@cancelPreviousThenRun
                    lastClipBoardHash = clipBoardText.hashCode()
                    QuickPreviewUtil.getPreviewInfoFlow(context, link)
                        .catch { it.printStackTrace() }
                        .retry(retries = 2) { err -> err !is NoConnectivityException }
                        .collect {
                            mutablePreviewInfoStateFlow.value = it
                        }
                } else {
                    clear()
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

    fun Uri.isBaidu(): Boolean = this.isHttp() && host?.endsWith("baidu.com") == true

    fun Uri.isHttp(): Boolean = scheme?.startsWith("http", ignoreCase = true) == true

    fun Uri.isTieba(): Boolean {
        return host != null && (host.equals("wapp.baidu.com", ignoreCase = true) ||
                host.equals("tieba.baidu.com", ignoreCase = true) ||
                host.equals("tiebac.baidu.com", ignoreCase = true))
    }

    fun Uri.isLogin(): Boolean = path?.let { p ->
        host.equals("wappass.baidu.com") && p.startsWith("/passport") && queryParameterNames.contains("login")
    } == true

    fun clear() {
        previewTaskRunner.cancelCurrent()
        mutablePreviewInfoStateFlow.value = null
    }
}