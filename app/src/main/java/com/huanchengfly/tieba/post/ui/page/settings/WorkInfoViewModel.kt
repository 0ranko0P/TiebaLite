package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.huanchengfly.tieba.post.utils.workManager
import com.huanchengfly.tieba.post.workers.NewMessageWorker
import com.huanchengfly.tieba.post.workers.OKSignWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WorkInfoViewModel @Inject constructor(@ApplicationContext context: Context) : ViewModel() {

    private val workManager = context.workManager()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    val oKSignPeriodic: StateFlow<String> = workManager.getWorkInfosByTagFlow(OKSignWorker.TAG)
        .map(transform = ::format)
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), WORKER_NOT_ENQUEUE)

    val oKSignExpedited: StateFlow<String> = workManager.getWorkInfosByTagFlow(OKSignWorker.TAG_EXPEDITED)
        .map(transform = ::format)
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), WORKER_NOT_ENQUEUE)

    val newMessagePeriodic: StateFlow<String> = workManager.getWorkInfosByTagFlow(NewMessageWorker.TAG)
        .map(transform = ::format)
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), WORKER_NOT_ENQUEUE)

    val newMessageOneShot: StateFlow<String> = workManager.getWorkInfosByTagFlow(NewMessageWorker.TAG_ONE_SHOT)
        .map(transform = ::format)
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), WORKER_NOT_ENQUEUE)

    private suspend fun format(workInfos: List<WorkInfo>): String {
        if (workInfos.isEmpty()) return WORKER_NOT_ENQUEUE

        return withContext(Dispatchers.Default) {
            val stringBuilder = StringBuilder()
            workInfos[0].run {
                stringBuilder.appendLine("ID: $id")
                stringBuilder.appendLine("Tags: {")
                for (tag in tags) {
                    stringBuilder.appendLine("    $tag")
                }
                stringBuilder.appendLine('}')
                if (nextScheduleTimeMillis != Long.MAX_VALUE) {
                    val nextRun = synchronized(TAG) {
                        dateFormat.format(Date(nextScheduleTimeMillis))
                    }
                    stringBuilder.appendLine("Next Run: $nextRun")
                }
                if (initialDelayMillis >= 1000) {
                    stringBuilder.appendLine("InitialDelay: ${initialDelayMillis / 1000}s")
                }
                stringBuilder.appendLine("State: $state")
                if (runAttemptCount > 0) {
                    stringBuilder.appendLine("Attempts: $runAttemptCount")
                }
                stringBuilder.appendLine("StopReason: $stopReasonString")
                val outputData = outputData.keyValueMap
                if (outputData.isNotEmpty()) {
                    stringBuilder.appendLine("Output: {")
                    outputData.forEach { k, v ->
                        stringBuilder.appendLine("    $k: $v")
                    }
                    stringBuilder.appendLine('}')
                }
                stringBuilder.toString()
            }
        }
    }

    companion object {

        private const val TAG = "WorkInfoViewModel"

        private const val WORKER_NOT_ENQUEUE = "No Worker"

        private val WorkInfo.stopReasonString: String
            get() = when (stopReason) {
                WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT -> "FOREGROUND_SERVICE_TIMEOUT"
                WorkInfo.STOP_REASON_NOT_STOPPED -> "NOT_STOPPED"
                WorkInfo.STOP_REASON_UNKNOWN -> "UNKNOWN"
                WorkInfo.STOP_REASON_CANCELLED_BY_APP -> "CANCELLED_BY_APP"
                WorkInfo.STOP_REASON_PREEMPT -> "PREEMPT"
                WorkInfo.STOP_REASON_TIMEOUT -> "TIMEOUT"
                WorkInfo.STOP_REASON_DEVICE_STATE -> "DEVICE_STATE"
                WorkInfo.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW -> "CONSTRAINT_BATTERY_NOT_LOW"
                WorkInfo.STOP_REASON_CONSTRAINT_CHARGING -> "CONSTRAINT_CHARGING"
                WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "CONSTRAINT_CONNECTIVITY"
                WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE -> "CONSTRAINT_DEVICE_IDLE"
                WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW -> "STORAGE_NOT_LOW"
                WorkInfo.STOP_REASON_QUOTA -> "QUOTA"
                WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION -> "BACKGROUND_RESTRICTION"
                WorkInfo.STOP_REASON_APP_STANDBY -> "APP_STANDBY"
                WorkInfo.STOP_REASON_USER -> "USER"
                WorkInfo.STOP_REASON_SYSTEM_PROCESSING -> "SYSTEM_PROCESSING"
                WorkInfo.STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED -> "ESTIMATED_APP_LAUNCH_TIME_CHANGED"
                else -> "CODE $stopReason"
            }
    }
}