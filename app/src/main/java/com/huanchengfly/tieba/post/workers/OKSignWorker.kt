package com.huanchengfly.tieba.post.workers

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaMSignException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.repository.user.OKSignRepository
import com.huanchengfly.tieba.post.services.OKSignTileService
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.NotificationUtils
import com.huanchengfly.tieba.post.utils.NotificationUtils.notificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

@HiltWorker
class OKSignWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val okSignRepository: OKSignRepository,
): CoroutineWorker(context, params) {

    private var notificationUpdater = OKSignNotificationUpdater(
        context = applicationContext,
        cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
    ) { progress, total ->
        OKSignTileService.requestListening(applicationContext)
        setProgressAsync(workDataOf(KEY_PROGRESS to progress, KEY_TOTAL to total))
    }

    override suspend fun doWork(): Result = try {
        OKSignTileService.requestListening(applicationContext)
        notificationUpdater.setupNotification()
        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) { // Android S: ForegroundServiceStartNotAllowedException
            Log.e(TAG, "onDoWork: Set foreground failed: ${e.message}")
        }

        okSignRepository.sign(listener = notificationUpdater)
        Result.success()
    } catch (e: CancellationException) {
        Log.e(TAG, "onDoWork: $id is canceled: ${e.message}.")
        throw e
    } catch (e: Throwable) {
        Log.e(TAG, "onDoWork: ${e.getErrorMessage()}.", e)
        notificationUpdater.onError(ConnectivityInterceptor.wrapException(e))
        Result.failure()
    } finally {
        notificationManager.cancel(NOTIFICATION_ID)
        okSignRepository.scheduleWorker()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = applicationContext.getString(R.string.title_loading_data)
        return ForegroundInfo(
            NOTIFICATION_ID,
            notificationUpdater.buildProgressNotification(title).build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
    }

    companion object {
        const val TAG = "OKSignWorker"
        const val TAG_EXPEDITED = "OKSignWorker:Expedited"

        const val KEY_PROGRESS = "sign_progress"
        const val KEY_TOTAL = "sign_total"

        /**
         * Channel ID of signing notifications.
         *
         * Note: Inherited from OKSignService, do not modify.
         *
         * @since 1.3.0
         * */
        private const val NOTIFICATION_CHANNEL_ID = "1"

        /**
         * ID of signing progress notification.
         *
         * Note: Inherited from OKSignService, do not modify.
         *
         * @since 1.3.0
         * */
        private const val NOTIFICATION_ID = 1

        /**
         * ID of signing result notification.
         *
         * @see OKSignNotificationUpdater.onFinish
         * */
        private const val NOTIFICATION_ID_SIGN_RESULT = 15

        /**
         * ID of official signing failed notification.
         *
         * @see OKSignNotificationUpdater.onMSignFailed
         * */
        private const val NOTIFICATION_ID_M_SIGN_FAILED = 16

        fun startDelayed(workManager: WorkManager, hourOfDay: Int, minute: Int) {
            val duration = DateTimeUtils.calculateNextDayDurationMills(hourOfDay, minute) / 1000
            val request = OneTimeWorkRequestBuilder<OKSignWorker>()
                .addTag(TAG)
                .setInitialDelay(duration = duration.coerceAtLeast(60), TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun startExpedited(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<OKSignWorker>()
                .addTag(TAG_EXPEDITED)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            workManager.enqueueUniqueWork(TAG_EXPEDITED, ExistingWorkPolicy.REPLACE, request)
        }

        fun observeOKSignWorkerInfo(workManager: WorkManager, expedited: Boolean = false): Flow<WorkInfo?> {
            val tag = if(expedited) TAG_EXPEDITED else TAG
            return workManager.getWorkInfosByTagFlow(tag).map { it.lastOrNull() }
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.title_oksign))
                .setLightsEnabled(false)
                .setShowBadge(false)
                .build()
                .let { channel -> notificationManager.createNotificationChannel(channel) }
        }

        /**
         * Helper class that updates the signing progress notification.
         *
         * @param context application context
         * @param cancelIntent [PendingIntent] can be used to cancel the worker
         * @param onProgress simple progress listener
         * */
        private class OKSignNotificationUpdater(
            private val context: Context,
            private val cancelIntent: PendingIntent,
            private val onProgress: (progress: Int, total: Int) -> Unit
        ): OKSignRepository.ProgressListener {

            private val notificationManager = NotificationUtils.notificationManager

            private var total = 0
            private var name: String  = ""

            fun buildProgressNotification(title: String, content: String? = null): NotificationCompat.Builder {
                return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(title)
                    .setTicker(title)
                    .setContentText(content)
                    .setSubText(context.getString(R.string.title_oksign))
                    .setSmallIcon(R.drawable.ic_oksign)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .addAction(0, context.getString(android.R.string.cancel), cancelIntent)
                    .setStyle(NotificationCompat.BigTextStyle())
            }

            fun setupNotification() {
                createNotificationChannel(context)
                notificationManager.cancel(NOTIFICATION_ID_SIGN_RESULT)
                notificationManager.cancel(NOTIFICATION_ID_M_SIGN_FAILED)
            }

            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            private fun updateProgressNotification(progress: Int, content: String?) {
                val title = context.getString(R.string.title_signing_progress, name, progress, total)
                buildProgressNotification(title, content)
                    .build()
                    .let { notificationManager.notify(NOTIFICATION_ID, it) }
                onProgress(progress, total)
            }

            override fun onInit(total: Int, userName: String) {
                this.total = total
                this.name = userName
                if (NotificationUtils.checkPermission(context)) {
                    buildProgressNotification(context.getString(R.string.title_start_sign))
                        .build()
                        .let { notificationManager.notify(NOTIFICATION_ID, it) }
                }
                onProgress(0, total)
            }

            override fun onSigned(progress: Int, forum: String, signBonusPoint: Int?) {
                if (NotificationUtils.checkPermission(context)) {
                    updateProgressNotification(
                        progress = progress + 1,
                        content = if (signBonusPoint == null) {
                            context.getString(R.string.text_singing_progress, forum)
                        } else {
                            context.getString(R.string.text_singing_progress_exp, forum, signBonusPoint)
                        }
                    )
                }
            }

            override fun onFailed(progress: Int, forum: String, error: String) {
                if (NotificationUtils.checkPermission(context)) {
                    val content = context.getString(R.string.text_singing_progress_fail, forum, error)
                    updateProgressNotification(progress = progress + 1, content = content)
                }
            }

            override fun onMSignFailed(e: Throwable) {
                if (NotificationUtils.checkPermission(context)) {
                    val content = if (e is TiebaMSignException) {
                        e.signNotice // Use message from server
                    } else {
                        context.getString(R.string.text_oksign_fallback)
                    }
                    NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(context.getString(R.string.text_oksign_failed, e.getErrorCode(), e.getErrorMessage()))
                        .setContentText(content)
                        .setSmallIcon(R.drawable.ic_round_warning)
                        .build()
                        .let { notificationManager.notify(NOTIFICATION_ID_M_SIGN_FAILED, it) }
                }
            }

            override fun onFinish(succeed: Int) {
                if (NotificationUtils.checkPermission(context)) {
                    notificationManager.cancel(NOTIFICATION_ID)
                    val title = context.getString(R.string.title_oksign_finish)
                    val content = if (total > 0) {
                        context.getString(R.string.text_oksign_done, succeed)
                    } else {
                        context.getString(R.string.text_oksign_no_signable)
                    }
                    buildProgressNotification(title, content)
                        .clearActions()
                        .setOngoing(false)
                        .build()
                        .let { notificationManager.notify(NOTIFICATION_ID_SIGN_RESULT, it) }
                }
                onProgress(succeed, total)
            }

            fun onError(e: Throwable) {
                if (NotificationUtils.checkPermission(context)) {
                    notificationManager.cancel(NOTIFICATION_ID)
                    val title = context.getString(R.string.title_oksign_fail)
                    buildProgressNotification(title, e.getErrorMessage())
                        .clearActions()
                        .setOngoing(false)
                        .build()
                        .let { notificationManager.notify(NOTIFICATION_ID_SIGN_RESULT, it) }
                }
                onProgress(total, total)
            }
        }
    }
}
