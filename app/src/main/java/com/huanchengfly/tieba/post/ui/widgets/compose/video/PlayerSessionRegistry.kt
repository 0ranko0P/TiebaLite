package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 通过 [syncGroupId] 共享 [PlayerCore]，并集中仲裁 Surface owner 与全局后台播放所有权 */
@MainThread
internal object PlayerSessionRegistry {
    private val sessions = mutableMapOf<Long, Entry>()
    private val surfaceOwnerStates = mutableMapOf<Long, MutableStateFlow<Long?>>()
    private val evictionHandlers = mutableMapOf<Long, () -> Unit>()
    private var playbackOwnerId: Long? = null

    private class Entry(
        val core: PlayerCore,
        var refs: Int = 0,
    )

    private fun surfaceOwnerState(syncGroupId: Long): MutableStateFlow<Long?> =
        surfaceOwnerStates.getOrPut(syncGroupId) { MutableStateFlow(null) }

    /** 为同一 [syncGroupId] 返回共享核心，并增加引用计数 */
    fun acquire(
        syncGroupId: Long,
        context: Context,
    ): PlayerCore {
        assertMainThread()
        val entry =
            sessions.getOrPut(syncGroupId) {
                Entry(PlayerCore(context))
            }
        entry.refs++
        return entry.core
    }

    /** 引用计数归零且不保活时释放核心和 Surface 记录 */
    fun release(
        syncGroupId: Long,
        keepAlive: Boolean = false,
    ) {
        assertMainThread()
        val entry = sessions[syncGroupId] ?: return
        if (entry.refs > 0) entry.refs--
        if (entry.refs <= 0 && !keepAlive) releaseEntry(syncGroupId, entry)
    }

    fun releaseIdle(syncGroupId: Long) {
        assertMainThread()
        val entry = sessions[syncGroupId] ?: return
        if (entry.refs <= 0) releaseEntry(syncGroupId, entry)
    }

    private fun releaseEntry(
        syncGroupId: Long,
        entry: Entry,
    ) {
        entry.core.release()
        sessions.remove(syncGroupId)
        surfaceOwnerStates.remove(syncGroupId)
    }

    /** 供前台服务绑定当前同步组的播放器 */
    fun player(syncGroupId: Long): Player? {
        assertMainThread()
        return sessions[syncGroupId]?.core?.playerOrNull()
    }

    /** 暴露当前同步组的 Surface owner 状态，供渲染层决定是否组合 [PlayerSurface] */
    fun surfaceOwner(syncGroupId: Long): StateFlow<Long?> {
        assertMainThread()
        return surfaceOwnerState(syncGroupId)
    }

    /** 显式声明当前控制器接管渲染端；通常由全屏/PiP 页面或可见内嵌实例调用 */
    fun claimSurface(
        syncGroupId: Long,
        controllerId: Long,
    ) {
        assertMainThread()
        surfaceOwnerState(syncGroupId).value = controllerId
    }

    /** 仅释放当前 owner 自己的声明，避免误删其他控制器已接管的渲染端 */
    fun releaseSurface(
        syncGroupId: Long,
        controllerId: Long,
    ) {
        assertMainThread()
        val ownerState = surfaceOwnerState(syncGroupId)
        if (ownerState.value == controllerId) {
            ownerState.value = null
        }
    }

    /** 全屏/PiP 退出到内嵌时仅释放当前 owner，由恢复后的内嵌控制器按同步组主动 claim。 */
    fun transferSurfaceToInline(syncGroupId: Long) {
        assertMainThread()
        surfaceOwnerState(syncGroupId).value = null
    }

    fun onPlaybackEvicted(
        controllerId: Long,
        callback: () -> Unit,
    ) {
        assertMainThread()
        evictionHandlers[controllerId] = callback
    }

    fun clearPlaybackEviction(controllerId: Long) {
        assertMainThread()
        evictionHandlers.remove(controllerId)
    }

    /** 全局策略：后台/全屏/PiP 播放同一时间只允许一个控制器保持播放 */
    fun claimPlayback(controllerId: Long) {
        assertMainThread()
        val previous = playbackOwnerId
        playbackOwnerId = controllerId
        if (previous != null && previous != controllerId) evictionHandlers[previous]?.invoke()
    }

    fun releasePlayback(controllerId: Long) {
        assertMainThread()
        if (playbackOwnerId == controllerId) playbackOwnerId = null
        evictionHandlers.remove(controllerId)
    }
}
