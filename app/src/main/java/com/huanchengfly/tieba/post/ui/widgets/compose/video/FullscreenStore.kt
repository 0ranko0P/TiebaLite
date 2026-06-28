package com.huanchengfly.tieba.post.ui.widgets.compose.video

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 保存 Activity PiP 状态，并在全屏退出时向内嵌播放器传递一次性播放快照 */
object FullscreenStore {
    data class PipState(
        val isInPip: Boolean = false,
        val sessionId: Long? = null,
    )

    private val _pipState = MutableStateFlow(PipState())
    val pipState: StateFlow<PipState> = _pipState.asStateFlow()
    private var activePipSessionId: Long? = null
    private var nextPipSessionId = 1L

    fun nextPipSessionId(): Long = nextPipSessionId++

    fun bindPipSession(sessionId: Long) {
        activePipSessionId = sessionId
        val current = _pipState.value
        if (current.isInPip && current.sessionId != sessionId) {
            _pipState.value = PipState()
        }
    }

    fun clearPipSession(sessionId: Long) {
        if (activePipSessionId == sessionId) activePipSessionId = null
        val current = _pipState.value
        if (current.sessionId == sessionId) {
            _pipState.value = PipState()
        }
    }

    fun setPip(isInPip: Boolean) {
        _pipState.value =
            if (isInPip) {
                PipState(isInPip = true, sessionId = activePipSessionId)
            } else {
                PipState()
            }
    }

    /** 按 URL 保存的一次性播放快照，仅供全屏返回后的内嵌播放器消费 */
    private val restoreStates = mutableMapOf<String, RestoreState>()

    /** 按 URL 保存的全屏方向偏好，避免播放快照承载页面状态 */
    private val orientationStates = mutableMapOf<String, Int?>()

    data class RestoreState(
        val position: Long,
        val play: Boolean,
        val speed: Float,
        val syncGroupId: Long,
    )

    fun saveRestoreState(
        url: String,
        position: Long,
        play: Boolean,
        speed: Float,
        syncGroupId: Long,
    ) {
        restoreStates[url] =
            RestoreState(
                position = position,
                play = play,
                speed = speed,
                syncGroupId = syncGroupId,
            )
    }

    fun saveRequestedOrientation(
        url: String,
        orientation: Int?,
    ) {
        orientationStates[url] = orientation
    }

    fun consumeRestoreState(url: String): RestoreState? = restoreStates.remove(url)

    fun consumeRequestedOrientation(url: String): Int? = orientationStates.remove(url)
}
