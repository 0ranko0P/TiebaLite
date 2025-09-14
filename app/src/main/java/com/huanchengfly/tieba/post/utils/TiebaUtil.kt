package com.huanchengfly.tieba.post.utils

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.retrofit.doIfFailure
import com.huanchengfly.tieba.post.api.retrofit.doIfSuccess
import com.huanchengfly.tieba.post.api.urlEncode
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.dialogs.LoadingDialog
import com.huanchengfly.tieba.post.pendingIntentFlagMutable
import com.huanchengfly.tieba.post.receivers.AutoSignAlarm
import com.huanchengfly.tieba.post.services.OKSignService
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.extension.toShareIntent
import java.util.Calendar

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

    fun initAutoSign(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val autoSign = context.appPreferences.autoSign
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AutoSignAlarm::class.java),
            pendingIntentFlagMutable()
        )
        if (autoSign) {
            val autoSignTimeStr = context.appPreferences.autoSignTime!!
            val time = autoSignTimeStr.split(":").toTypedArray()
            val hour = time[0].toInt()
            val minute = time[1].toInt()
            val calendar = Calendar.getInstance()
            calendar[Calendar.HOUR_OF_DAY] = hour
            calendar[Calendar.MINUTE] = minute
            if (calendar.timeInMillis >= System.currentTimeMillis()) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } else {
            alarmManager.cancel(pendingIntent)
        }
    }

    @JvmStatic
    fun startSign(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            context.toastShort(R.string.toast_no_permission_notification)
            return
        }

        context.appPreferences.signDay = Calendar.getInstance()[Calendar.DAY_OF_MONTH]
        ContextCompat.startForegroundService(
            context,
            Intent(context, OKSignService::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(OKSignService.ACTION_START_SIGN)
        )
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
            .doIfFailure {
                dialog.dismiss()
                context.toastShort(R.string.toast_load_failed)
            }
    }
}