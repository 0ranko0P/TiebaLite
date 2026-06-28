package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.huanchengfly.tieba.post.components.MediaCache
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.models.settings.PlayerSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [initialize] 完成前的更新会暂存，初始化时再与持久化快照合并，避免早期 UI 操作丢失 */
object PlayerSettingsStore {
    private val _state = MutableStateFlow(PlayerSettings())
    val state: StateFlow<PlayerSettings> = _state.asStateFlow()

    private var repo: Settings<PlayerSettings>? = null
    private var ready = false
    private val syncScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val pendingEdits = mutableListOf<(PlayerSettings) -> PlayerSettings>()

    /** 从持久化存储加载设置，应用初始化前暂存的更新，并设置缓存大小上限 */
    @OptIn(UnstableApi::class)
    @MainThread
    suspend fun initialize(
        store: Settings<PlayerSettings>,
        context: Context,
    ) {
        if (ready) return
        this.repo = store
        val pending = pendingEdits.toList()
        pendingEdits.clear()
        val saved =
            try {
                store.snapshot()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        val next = pending.fold(saved ?: _state.value) { state, edit -> edit(state) }
        _state.value = next
        MediaCache.setMaxBytes(next.videoCacheLimitMb * 1024L * 1024L)
        ready = true
        if (pending.isNotEmpty()) store.set(next)
        syncScope.launch {
            store.collect {
                _state.value = it
                MediaCache.setMaxBytes(it.videoCacheLimitMb * 1024L * 1024L)
            }
        }
    }

    /** 如果尚未初始化完成，更新会暂存到 [pendingEdits]，待初始化时一并应用 */
    @MainThread
    fun update(edit: (PlayerSettings) -> PlayerSettings) {
        _state.update { state -> edit(state) }
        val repo = this.repo
        if (repo != null) {
            repo.save(edit)
        } else {
            pendingEdits.add(edit)
        }
    }
}
