package com.huanchengfly.tieba.post.workers

import android.Manifest
import android.app.NotificationChannelGroup
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.ui.page.TB_LITE_DOMAIN
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.utils.NotificationUtils
import com.huanchengfly.tieba.post.utils.NotificationUtils.notificationManager
import com.huanchengfly.tieba.post.utils.ThemeUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class NewMessageWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    private val homeRepository: HomeRepository
) : CoroutineWorker(context, params) {

    init {
        setupNotification()
    }

    override suspend fun doWork(): Result = try {
        val newMessage = homeRepository.fetchNewMessage()
        if (NotificationUtils.checkPermission(context)) {
            if (newMessage.replyMe > 0) {
                updateNotification(NotificationsType.ReplyMe, newMsgCount = newMessage.replyMe)
            }
            if (newMessage.atMe > 0) {
                updateNotification(NotificationsType.AtMe, newMsgCount = newMessage.atMe)
            }
        }
        Result.success(
            workDataOf(KEY_NEW_MESSAGE_COUNT to newMessage.replyMe + newMessage.atMe)
        )
    } catch (e: CancellationException) {
        Log.e(TAG, "onDoWork: $id canceled ${e.message}")
        throw e
    } catch (e: Throwable) {
        Log.e(TAG, "onDoWork: Error: ${e.getErrorMessage()}, code: ${e.getErrorCode()}", e)
        Result.failure()
    }

    private fun setupNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channelGroupName = context.getString(R.string.channel_group_msg)
        NotificationChannelGroup(CHANNEL_GROUP_ID, channelGroupName).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                it.description = context.getString(R.string.channel_group_msg_description)
            }
            notificationManager.createNotificationChannelGroup(it)
            NotificationUtils.createChannel(CHANNEL_REPLY, getChannelName(CHANNEL_REPLY), groupId = it.id)
            NotificationUtils.createChannel(CHANNEL_AT, getChannelName(CHANNEL_AT), groupId = it.id)
        }
    }

    /**
     * Post or replace a new message notification.
     *
     * @param type notification type
     * @param newMsgCount number of new messages
     * */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotification(type: NotificationsType, newMsgCount: Int) {
        val uri = "$TB_LITE_DOMAIN://notifications?type=${type.ordinal}".toUri()
        val intent = Intent(ACTION_VIEW, uri, context, MainActivityV2::class.java)
        when (type) {
            NotificationsType.AtMe -> {
                val title = context.getString(R.string.tips_message_at, newMsgCount)
                updateNotification(title, id = ID_AT, CHANNEL_AT, intent)
            }

            NotificationsType.ReplyMe -> {
                val title = context.getString(R.string.tips_message_reply, newMsgCount)
                updateNotification(title, id = ID_REPLY, CHANNEL_REPLY, intent)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotification(title: String, id: Int, channelId: String, intent: Intent) {
        val currentColorScheme = ThemeUtil.currentColorScheme()
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.tip_touch_to_view))
            .setSubText(getChannelName(channelId))
            .setSmallIcon(R.drawable.ic_round_drafts)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
            .setColor(currentColorScheme.primary.toArgb())
            .build()
        notificationManager.notify(id, notification)
    }

    private fun getChannelName(channelId: String): String = when (channelId) {
        CHANNEL_AT -> context.getString(R.string.channel_at)

        CHANNEL_REPLY -> context.getString(R.string.channel_reply)

        else -> throw RuntimeException("Unknow channel ID: $channelId")
    }

    companion object {
        const val TAG = "NewMessageWorker"
        const val TAG_ONE_SHOT = "NewMessageWorker:OneShot"
        const val KEY_NEW_MESSAGE_COUNT = "new_msg_count"

        private const val CHANNEL_GROUP_ID = "20"
        private const val CHANNEL_AT = "3"
        private const val CHANNEL_REPLY = "2"

        private const val ID_REPLY = 20
        private const val ID_AT = 21

        fun schedulePeriodically(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<NewMessageWorker>(
                repeatInterval = 40, TimeUnit.MINUTES,
                flexTimeInterval = 5, TimeUnit.MINUTES
            )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.CONNECTED
                    )
                )
                .addTag(TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun startNow(workManager: WorkManager): UUID {
            val request = OneTimeWorkRequestBuilder<NewMessageWorker>()
                .addTag(TAG_ONE_SHOT)
                .build()
            workManager.enqueueUniqueWork(TAG_ONE_SHOT, ExistingWorkPolicy.REPLACE, request)
            return request.id
        }
    }
}