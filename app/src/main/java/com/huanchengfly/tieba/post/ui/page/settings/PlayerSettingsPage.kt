package com.huanchengfly.tieba.post.ui.page.settings

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.models.settings.PlayerSettings
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.numberPref
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.preference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.toggleablePreference
import kotlinx.collections.immutable.persistentMapOf
import kotlin.math.roundToInt

/** 播放器设置页面 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerSettingsPage(
    settings: Settings<PlayerSettings>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val playerSettingsState = settings.collectAsStateWithLifecycle(initialValue = PlayerSettings())
    val playerSettings by playerSettingsState
    var cacheSettingsBaseline by rememberSaveable { mutableStateOf<PlayerSettings?>(null) }
    val cacheSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cacheIcon = ImageVector.vectorResource(R.drawable.ic_sliders)
    val saveIcon = ImageVector.vectorResource(R.drawable.ic_save)
    val doubleTapIcon = ImageVector.vectorResource(R.drawable.ic_touch_double)
    val orientationIcon = ImageVector.vectorResource(R.drawable.ic_screen_rotation_alt)
    val backgroundIcon = ImageVector.vectorResource(R.drawable.ic_headphones)
    val pipIcon = ImageVector.vectorResource(R.drawable.ic_picture_in_picture)
    val swipeIcon = ImageVector.vectorResource(R.drawable.ic_swipe)
    val swipeVerticalIcon = ImageVector.vectorResource(R.drawable.ic_swipe_vertical)
    val longPressIcon = ImageVector.vectorResource(R.drawable.ic_touch_long)
    val cacheSettingIcon = ImageVector.vectorResource(R.drawable.ic_database)

    SettingsScaffold(
        titleRes = R.string.title_player_settings,
        onBack = onBack,
        settings = settings,
        initialValue = PlayerSettings(),
        state = playerSettingsState,
    ) {
        group(title = R.string.settings_group_player_feature) {
            // 后台播放
            toggleablePreference(
                property = PlayerSettings::backgroundPlayEnabled,
                title = R.string.background_play,
                leadingIcon = backgroundIcon,
            )

            // PIP
            toggleablePreference(
                property = PlayerSettings::pipAutoEnterEnabled,
                title = R.string.pip_auto_enter,
                leadingIcon = pipIcon,
            )

            // 全屏方向
            toggleablePreference(
                property = PlayerSettings::fullscreenFollowScreenOrientation,
                title = R.string.fullscreen_follow_screen_orientation,
                leadingIcon = orientationIcon,
            )

            // 保存进度
            toggleablePreference(
                property = PlayerSettings::saveVideoProgress,
                title = R.string.save_video_progress,
                leadingIcon = saveIcon,
            )

            // 分段缓存
            toggleablePreference(
                property = PlayerSettings::drawSegmentedCache,
                title = R.string.draw_segmented_cache,
                summary =
                    if (currentPreference.diskCacheEnabled) {
                        null
                    } else {
                        R.string.summary_depends_on_disk_cache
                    },
                leadingIcon = cacheIcon,
                enabled = currentPreference.diskCacheEnabled,
            )

            // 缓存
            preference(
                title = R.string.title_player_cache,
                leadingIcon = cacheSettingIcon,
                onClick = {
                    cacheSettingsBaseline = playerSettings
                },
            )
        }

        group(title = R.string.settings_group_player_gesture) {
            // 双击暂停
            toggleablePreference(
                property = PlayerSettings::doubleTapPauseEnabled,
                title = R.string.gesture_double_tap_pause,
                leadingIcon = doubleTapIcon,
            )

            // 双击快进/快退
            toggleablePreference(
                property = PlayerSettings::doubleTapSeekEnabled,
                title = R.string.gesture_double_tap_seek,
                leadingIcon = doubleTapIcon,
            )

            // 滑动快进/快退
            toggleablePreference(
                property = PlayerSettings::horizontalSwipeSeekEnabled,
                title = R.string.gesture_horizontal_swipe_seek,
                leadingIcon = swipeIcon,
            )

            // 长按倍速
            listPref(
                value = currentPreference.longPressSpeed,
                onValueChange = { value -> settings.save { it.copy(longPressSpeed = value) } },
                options =
                    persistentMapOf(
                        "disabled" to context.getString(R.string.gesture_long_press_speed_disabled),
                        "auto_double" to context.getString(R.string.gesture_long_press_speed_auto_double),
                        "1.5" to "1.5X",
                        "2" to "2X",
                        "3" to "3X",
                        "4" to "4X",
                    ),
                title = context.getString(R.string.gesture_long_press_speed),
                leadingIcon = longPressIcon,
            )

            // 滑动调节音量
            toggleablePreference(
                property = PlayerSettings::volumeControlEnabled,
                title = R.string.gesture_volume_control,
                leadingIcon = swipeVerticalIcon,
            )

            // 滑动调节亮度
            toggleablePreference(
                property = PlayerSettings::brightnessControlEnabled,
                title = R.string.gesture_brightness_control,
                leadingIcon = swipeVerticalIcon,
            )
        }
    }

    if (cacheSettingsBaseline != null) {
        CacheSettingsBottomSheet(
            state = playerSettingsState,
            settings = settings,
            sheetState = cacheSheetState,
            onDismiss = {
                val baseline = cacheSettingsBaseline
                cacheSettingsBaseline = null
                if (baseline != null && baseline != playerSettings) {
                    context.toastShort(context.getString(R.string.summary_reopen_player_to_apply))
                }
            },
        )
    }
}

private const val VIDEO_CACHE_MIN_LIMIT_MB = 100
private const val VIDEO_CACHE_MAX_LIMIT_MB = 10240
private const val FORWARD_BUFFER_MIN_SIZE_MB = 64
private const val FORWARD_BUFFER_MAX_SIZE_MB = 512
private const val BACK_BUFFER_MIN_DURATION_SEC = 0
private const val BACK_BUFFER_MAX_DURATION_SEC = 300
private const val MIN_BUFFER_MIN_SEC = 10
private const val MIN_BUFFER_MAX_SEC = 300
private const val MAX_BUFFER_MIN_SEC = 30
private const val MAX_BUFFER_MAX_SEC = 600
private const val PLAYBACK_BUFFER_MIN_SEC = 2
private const val PLAYBACK_BUFFER_MAX_SEC = 30
private const val MILLIS_PER_SECOND = 1_000

private fun millisToRoundedSeconds(millis: Int): Int = (millis.toFloat() / MILLIS_PER_SECOND).roundToInt()

/** 缓存设置底部弹窗 */
@Composable
private fun CacheSettingsBottomSheet(
    state: State<PlayerSettings>,
    settings: Settings<PlayerSettings>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        SegmentedPrefsScreen(
            modifier = Modifier.navigationBarsPadding(),
            initialValue = PlayerSettings(),
            state = state,
            settings = settings,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            fluid = true,
        ) {
            group(title = R.string.settings_group_player_disk_cache) {
                toggleablePreference(
                    property = PlayerSettings::diskCacheEnabled,
                    title = R.string.title_disk_cache_enabled,
                )

                if (currentPreference.diskCacheEnabled) {
                    // 硬盘缓存
                    numberPref(
                        value = currentPreference.videoCacheLimitMb,
                        onValueChange = { value -> settings.save { it.copy(videoCacheLimitMb = value) } },
                        range = VIDEO_CACHE_MIN_LIMIT_MB..VIDEO_CACHE_MAX_LIMIT_MB,
                        title = R.string.title_video_cache_limit,
                        unit = R.string.unit_mb,
                    )
                }
            }

            group(title = R.string.settings_group_player_buffer_cache) {
                // 缓冲区
                numberPref(
                    value = currentPreference.forwardBufferSizeMb,
                    onValueChange = { value -> settings.save { it.copy(forwardBufferSizeMb = value) } },
                    range = FORWARD_BUFFER_MIN_SIZE_MB..FORWARD_BUFFER_MAX_SIZE_MB,
                    title = R.string.title_forward_buffer_size,
                    unit = R.string.unit_mb,
                )

                toggleablePreference(
                    property = PlayerSettings::advancedBufferSettingsEnabled,
                    title = R.string.title_advanced_buffer,
                )

                if (currentPreference.advancedBufferSettingsEnabled) {
                    numberPref(
                        value = currentPreference.backBufferDurationSec,
                        onValueChange = { value -> settings.save { it.copy(backBufferDurationSec = value) } },
                        range = BACK_BUFFER_MIN_DURATION_SEC..BACK_BUFFER_MAX_DURATION_SEC,
                        title = R.string.title_back_buffer_duration,
                        unit = R.string.unit_second,
                    )

                    numberPref(
                        value = millisToRoundedSeconds(currentPreference.maxBufferMs),
                        onValueChange = { value ->
                            val valueMs = value * MILLIS_PER_SECOND
                            settings.save { current ->
                                current.copy(
                                    minBufferMs = current.minBufferMs.coerceAtMost(valueMs),
                                    maxBufferMs = valueMs,
                                )
                            }
                        },
                        range = MAX_BUFFER_MIN_SEC..MAX_BUFFER_MAX_SEC,
                        title = R.string.title_max_buffer,
                        unit = R.string.unit_second,
                    )

                    numberPref(
                        value = millisToRoundedSeconds(currentPreference.minBufferMs),
                        onValueChange = { value ->
                            val valueMs = value * MILLIS_PER_SECOND
                            settings.save { current ->
                                current.copy(
                                    minBufferMs = valueMs,
                                    maxBufferMs = current.maxBufferMs.coerceAtLeast(valueMs),
                                )
                            }
                        },
                        range = MIN_BUFFER_MIN_SEC..MIN_BUFFER_MAX_SEC,
                        title = R.string.title_min_buffer,
                        unit = R.string.unit_second,
                    )

                    numberPref(
                        value = millisToRoundedSeconds(currentPreference.bufferForPlaybackMs),
                        onValueChange = { value ->
                            val valueMs = value * MILLIS_PER_SECOND
                            settings.save { it.copy(bufferForPlaybackMs = valueMs) }
                        },
                        range = PLAYBACK_BUFFER_MIN_SEC..PLAYBACK_BUFFER_MAX_SEC,
                        title = R.string.title_buffer_for_playback,
                        unit = R.string.unit_second,
                    )

                    numberPref(
                        value = millisToRoundedSeconds(currentPreference.bufferForPlaybackAfterRebufferMs),
                        onValueChange = { value ->
                            val valueMs = value * MILLIS_PER_SECOND
                            settings.save { it.copy(bufferForPlaybackAfterRebufferMs = valueMs) }
                        },
                        range = PLAYBACK_BUFFER_MIN_SEC..PLAYBACK_BUFFER_MAX_SEC,
                        title = R.string.title_buffer_for_playback_after_rebuffer,
                        unit = R.string.unit_second,
                    )

                    toggleablePreference(
                        property = PlayerSettings::prioritizeCacheSize,
                        title = R.string.title_prioritize_cache_size,
                    )
                }
            }
        }
    }
}
