package com.huanchengfly.tieba.post.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.retrofit.doIfFailure
import com.huanchengfly.tieba.post.api.retrofit.doIfSuccess
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.urlEncode
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.dialogs.LoadingDialog
import com.huanchengfly.tieba.post.di.RepositoryEntryPoint
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.extension.toShareIntent
import com.huanchengfly.tieba.post.workers.OKSignWorker
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object TiebaUtil {
    private fun ClipData.setIsSensitive(isSensitive: Boolean): ClipData = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, isSensitive)
            }
        }
    }

    fun copyText(context: Context, text: String?, isSensitive: Boolean = false) {
        val cm: ClipboardManager = ClipBoardLinkDetector.clipBoardManager
        val clipData = ClipData.newPlainText("Tieba Lite", text).setIsSensitive(isSensitive)
        cm.setPrimaryClip(clipData)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            context.toastShort(R.string.toast_copy_success)
        }
    }

    @JvmStatic
    fun startSign(context: Context) {
        val okSignRepo = EntryPointAccessors.fromApplication<RepositoryEntryPoint>(context).okSignRepository()
        val isOKSignWorkerRunning = runBlocking { okSignRepo.isOKSignWorkerRunning.first() }
        if (isOKSignWorkerRunning) {
            context.toastShort(R.string.toast_oksign_start)
        } else if (NotificationUtils.checkPermission(context)) {
            OKSignWorker.startExpedited(context.workManager())
        } else {
            context.toastShort(R.string.toast_no_permission_notification)
        }
    }

    fun shareForum(context: Context, forum: String) {
        val forumName = forum.urlEncode()
        val link = "https://tieba.baidu.com/f?kw=$forumName"
        link.toUri()
            .toShareIntent(context, "text/plain", context.getString(R.string.title_forum, forum))
            .let { intent -> runCatching { context.startActivity(intent) } }

        ClipBoardLinkDetector.onCopyTiebaLink(link)
    }

    fun shareThread(context: Context, title: String, threadId: Long) {
        val link = "https://tieba.baidu.com/p/$threadId"
        link.toUri()
            .toShareIntent(context, "text/plain", title)
            .let { runCatching { context.startActivity(it) } }

        ClipBoardLinkDetector.onCopyTiebaLink(link)
    }

    suspend fun reportPost(
        context: Context,
        navigator: NavController,
        postId: String,
    ) {
        val dialog = LoadingDialog(context).apply { show() }
        TiebaApi.getInstance()
            .checkReportPostAsync(postId)
            .doIfSuccess {
                dialog.dismiss()
                navigator.navigate(Destination.WebView(it.data.url))
            }
            .doIfFailure { e ->
                dialog.dismiss()
                if (e is TiebaNotLoggedInException) {
                    context.toastShort(R.string.title_not_logged_in)
                } else {
                    context.toastShort(R.string.toast_load_failed)
                }
            }
    }
}