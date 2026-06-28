package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.os.Parcelable
import androidx.media3.common.Player
import com.huanchengfly.tieba.post.ui.models.settings.PlayerSettings
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

enum class PlayerDisplayMode {
    Inline,
    Fullscreen,
    Pip,
}

enum class InitialPositionKind {
    Explicit,
    Handoff,
    PersistedResume,
}

/** 视频数据源 */
data class PlayerSource(
    val url: String,
    val initialPositionMs: Long = 0L,
    val initialPositionKind: InitialPositionKind = InitialPositionKind.PersistedResume,
    val initialSpeed: Float = 1f,
    val title: String? = null,
    val artist: String? = null,
    val artworkUrl: String? = null,
)

/** 全屏播放路由与会话参数 */
@Serializable
data class FullscreenArgs(
    val videoUrl: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val syncGroupId: Long? = null,
    val inlineControllerId: Long = 0L,
    val requestedOrientation: Int? = null,
    val playWhenReady: Boolean = true,
)

enum class LevelType { Brightness, Volume }

@Parcelize
data class LevelChange(
    val type: LevelType,
    val level: Float,
) : Parcelable

@Parcelize
data class SeekPreview(
    val targetMs: Float,
    val deltaMs: Float,
) : Parcelable

enum class QuickSeek {
    None,
    Rewind,
    Forward,
}

/** 只保存 UI 层需要观察和跨重组保留的状态；不可序列化资源由控制器和核心层管理 */
@Parcelize
data class PlayerUiState(
    val thumbnail: String? = null,
    val isPlaying: Boolean = false,
    val controlsShown: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val wasEnded: Boolean = false,
    val hasPlaybackError: Boolean = false,
    val quickSeek: QuickSeek = QuickSeek.None,
    val playbackSpeed: Float = 1f,
    val isLocked: Boolean = false,
    val lockOverlayShown: Boolean = false,
    val settings: PlayerSettings = PlayerSettings(),
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
) : Parcelable
