package com.huanchengfly.tieba.post.ui.models.settings

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/** 播放器设置快照 */
@Immutable
@Parcelize
data class PlayerSettings(
    val longPressSpeed: String = "disabled",
    val doubleTapPauseEnabled: Boolean = true,
    val doubleTapSeekEnabled: Boolean = true,
    val horizontalSwipeSeekEnabled: Boolean = true,
    val volumeControlEnabled: Boolean = true,
    val brightnessControlEnabled: Boolean = true,
    val backgroundPlayEnabled: Boolean = false,
    val pipAutoEnterEnabled: Boolean = true,
    val fullscreenFollowScreenOrientation: Boolean = false,
    val saveVideoProgress: Boolean = true,
    val drawSegmentedCache: Boolean = true,
    val diskCacheEnabled: Boolean = true,
    val videoCacheLimitMb: Int = 500,
    val forwardBufferSizeMb: Int = 50,
    val advancedBufferSettingsEnabled: Boolean = false,
    val minBufferMs: Int = 50_000,
    val maxBufferMs: Int = 50_000,
    val bufferForPlaybackMs: Int = 3_000,
    val bufferForPlaybackAfterRebufferMs: Int = 5_000,
    val backBufferDurationSec: Int = 30,
    val prioritizeCacheSize: Boolean = true,
) : Parcelable
