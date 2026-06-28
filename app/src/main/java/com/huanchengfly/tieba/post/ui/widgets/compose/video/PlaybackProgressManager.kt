package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.huanchengfly.tieba.post.components.MediaCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val PROGRESS_SAVE_INTERVAL_MS = 10_000L

/** 管理单个视频源的进度恢复与持久化；handoff 期间暂停写入，避免覆盖接手播放器的状态 */
internal class PlaybackProgressManager(
    context: Context,
    private val scope: CoroutineScope,
    private val policy: VideoProgressPolicy = VideoProgressPolicy(),
    enabled: Boolean = true,
) {
    private val context = context.applicationContext
    private var player: ExoPlayer? = null
    private var sourceUrl: String? = null
    private var trackingJob: Job? = null
    private var isTransitioning: Boolean = false
    private var enabled: Boolean = enabled

    fun resolveInitialPosition(source: PlayerSource): Long =
        when (source.initialPositionKind) {
            InitialPositionKind.Explicit, InitialPositionKind.Handoff -> {
                source.initialPositionMs.coerceAtLeast(0L)
            }

            InitialPositionKind.PersistedResume -> {
                if (!enabled) {
                    0L
                } else {
                    source.initialPositionMs.takeIf { it > 0L }
                        ?: VideoProgressStore.progress(context, source.url, policy = policy)?.positionMs
                        ?: 0L
                }
            }
        }

    fun attach(
        player: ExoPlayer,
        source: PlayerSource,
    ) {
        this.player = player
        sourceUrl = source.url
        startTrackingIfNeeded()
    }

    fun detach() {
        stopTracking()
        player = null
        sourceUrl = null
    }

    fun setTransitioning(value: Boolean) {
        if (isTransitioning == value) return
        isTransitioning = value
        if (value) {
            stopTracking()
        } else {
            startTrackingIfNeeded()
        }
    }

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (!value) {
            stopTracking()
            clear(commit = true)
        } else {
            startTrackingIfNeeded()
        }
    }

    fun onIsPlayingChanged(isPlaying: Boolean) {
        val activePlayer = player ?: return
        if (isPlaying) {
            startTrackingIfNeeded()
        } else {
            stopTracking()
            if (!activePlayer.playWhenReady && !isTransitioning) saveNow()
        }
    }

    fun onPositionDiscontinuity() {
        if (!isTransitioning) saveNow()
    }

    /** 仅在实际播放中启动低频兜底轮询；主要保存仍由暂停、seek、切源、释放等事件触发 */
    fun startTrackingIfNeeded() {
        val activePlayer = player ?: return
        if (!enabled) return
        if (sourceUrl == null) return
        if (isTransitioning) return
        if (!activePlayer.isPlaying) return
        if (trackingJob?.isActive == true) return
        trackingJob =
            scope.launch {
                while (isActive) {
                    delay(PROGRESS_SAVE_INTERVAL_MS)
                    saveNow()
                }
            }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }

    fun saveNow(commit: Boolean = false) {
        if (!enabled) return
        val url = sourceUrl ?: return
        val activePlayer = player ?: return
        if (activePlayer.playbackState == Player.STATE_ENDED) {
            clear(commit = commit)
            return
        }
        val duration = activePlayer.duration.takeIf { it > 0L } ?: return
        when (
            val decision =
                policy.classifySave(
                    positionMs = activePlayer.currentPosition,
                    durationMs = duration,
                    isTransitioning = isTransitioning,
                )
        ) {
            ProgressSaveDecision.Ignore -> {
                Unit
            }

            ProgressSaveDecision.Clear -> {
                VideoProgressStore.clear(context, url, commit = commit)
            }

            is ProgressSaveDecision.Save -> {
                VideoProgressStore.save(
                    context = context,
                    url = url,
                    positionMs = decision.positionMs,
                    durationMs = decision.durationMs,
                    commit = commit,
                    scope = scope,
                )
            }
        }
    }

    fun clear(commit: Boolean = false) {
        val url = sourceUrl ?: return
        VideoProgressStore.clear(context, url, commit = commit)
    }
}

/** 视频进度存储，按缓存键归档并定期清理过期或超量记录 */
internal object VideoProgressStore {
    private const val PREFS_NAME = "video_progress"
    private const val PROGRESS_PREFIX = "progress_"
    private const val MAX_ENTRIES = 500
    private const val LAST_PRUNE_AT = "last_prune_at"
    private const val PRUNE_INTERVAL_MS = 6 * 60 * 60 * 1000L

    fun progress(
        context: Context,
        url: String,
        nowMs: Long = System.currentTimeMillis(),
        policy: VideoProgressPolicy = VideoProgressPolicy(),
    ): SavedVideoProgress? {
        val prefs = prefs(context)
        val id = url.cacheId()
        val value = prefs.getString(progressKey(id), null) ?: return null
        val saved =
            SavedVideoProgress.deserialize(value) ?: run {
                clear(context, url)
                return null
            }
        if (!policy.shouldRestore(saved, nowMs)) {
            clear(context, url)
            return null
        }
        return saved
    }

    fun save(
        context: Context,
        url: String,
        positionMs: Long,
        durationMs: Long,
        commit: Boolean = false,
        scope: CoroutineScope,
        pruneDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        val prefs = prefs(context)
        val id = url.cacheId()
        val now = System.currentTimeMillis()
        val key = progressKey(id)
        val isNewEntry = !prefs.contains(key)
        val progress =
            SavedVideoProgress(
                positionMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(0L),
                updatedAtMs = now,
            )
        val edit = prefs.edit().putString(key, progress.serialize())
        edit.save(commit)
        scope.launch(pruneDispatcher) {
            maybePrune(context, now, isNewEntry)
        }
    }

    fun clear(
        context: Context,
        url: String,
        commit: Boolean = false,
    ) {
        val prefs = prefs(context)
        val id = url.cacheId()
        val edit = prefs.edit().remove(progressKey(id))
        edit.save(commit)
    }

    private fun maybePrune(
        context: Context,
        now: Long,
        isNewEntry: Boolean,
    ) {
        val prefs = prefs(context)
        // 新增条目或达到巡检间隔时再扫描，避免每次写入都遍历 SharedPreferences 全量键
        val shouldCheckCount = isNewEntry || now - prefs.getLong(LAST_PRUNE_AT, 0L) >= PRUNE_INTERVAL_MS
        if (!shouldCheckCount) return
        val entries = prefs.all.filterKeys { it.startsWith(PROGRESS_PREFIX) }
        val count = entries.size
        if (count <= MAX_ENTRIES && now - prefs.getLong(LAST_PRUNE_AT, 0L) < PRUNE_INTERVAL_MS) return
        prune(prefs, entries, now)
    }

    private fun prune(
        prefs: SharedPreferences,
        entries: Map<String, *>,
        now: Long,
    ) {
        val invalidKeys = mutableSetOf<String>()
        val parsed =
            entries
                .mapNotNull { (key, value) ->
                    val serialized =
                        (value as? String) ?: run {
                            invalidKeys += key
                            return@mapNotNull null
                        }
                    val progress =
                        SavedVideoProgress.deserialize(serialized) ?: run {
                            invalidKeys += key
                            return@mapNotNull null
                        }
                    key to progress
                }.sortedByDescending { it.second.updatedAtMs }

        val expired = parsed.filter { now - it.second.updatedAtMs > TimeUnit.DAYS.toMillis(30) }.map { it.first }
        val overflow = parsed.drop(MAX_ENTRIES).map { it.first }
        val keysToRemove = (invalidKeys + expired + overflow).distinct()

        val edit = prefs.edit().putLong(LAST_PRUNE_AT, now)
        keysToRemove.forEach { key ->
            edit.remove(key)
        }
        edit.apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun SharedPreferences.Editor.save(commit: Boolean) {
        if (commit) this.commit() else apply()
    }

    private fun progressKey(id: String): String = "$PROGRESS_PREFIX$id"

    @OptIn(UnstableApi::class)
    private fun String.cacheId(): String = MediaCache.key(this)
}

/** 单条持久化的视频进度记录 */
internal data class SavedVideoProgress(
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long,
) {
    fun serialize(): String = "$positionMs,$durationMs,$updatedAtMs"

    companion object {
        fun deserialize(serialized: String): SavedVideoProgress? {
            val parts = serialized.split(',')
            if (parts.size != 3) return null
            val positionMs = parts[0].toLongOrNull() ?: return null
            val durationMs = parts[1].toLongOrNull() ?: return null
            val updatedAtMs = parts[2].toLongOrNull() ?: return null
            if (positionMs < 0L || durationMs <= 0L || updatedAtMs <= 0L) return null
            return SavedVideoProgress(positionMs, durationMs, updatedAtMs)
        }
    }
}

/** 保存决策：忽略、清除或写入当前位置 */
internal sealed interface ProgressSaveDecision {
    data object Ignore : ProgressSaveDecision

    data object Clear : ProgressSaveDecision

    data class Save(
        val positionMs: Long,
        val durationMs: Long,
    ) : ProgressSaveDecision
}

/** 进度保存与恢复策略：只约束有效性和过期时间，不额外引入业务条件 */
internal class VideoProgressPolicy(
    private val expireMs: Long = TimeUnit.DAYS.toMillis(30),
) {
    /** 仅过滤无效状态；合法进度一律保存，恢复阶段只受过期时间约束 */
    fun classifySave(
        positionMs: Long,
        durationMs: Long,
        isTransitioning: Boolean = false,
    ): ProgressSaveDecision {
        if (isTransitioning) return ProgressSaveDecision.Ignore
        if (positionMs < 0L || durationMs <= 0L) return ProgressSaveDecision.Ignore
        return ProgressSaveDecision.Save(
            positionMs = positionMs.coerceIn(0L, durationMs),
            durationMs = durationMs,
        )
    }

    fun shouldRestore(
        saved: SavedVideoProgress,
        nowMs: Long,
    ): Boolean = saved.updatedAtMs > 0L && nowMs - saved.updatedAtMs <= expireMs
}
