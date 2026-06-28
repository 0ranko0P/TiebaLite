package com.huanchengfly.tieba.post.ui.page.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.LocalFullscreenFollowScreenOrientation
import com.huanchengfly.tieba.post.LocalPipAutoEnterEnabled
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.ui.widgets.compose.video.FullscreenArgs
import com.huanchengfly.tieba.post.ui.widgets.compose.video.FullscreenCallbacks
import com.huanchengfly.tieba.post.ui.widgets.compose.video.FullscreenContent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.FullscreenEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.FullscreenInput
import com.huanchengfly.tieba.post.ui.widgets.compose.video.FullscreenStore
import com.huanchengfly.tieba.post.ui.widgets.compose.video.PipAction
import kotlinx.coroutines.flow.MutableSharedFlow

private const val ACTION_PIP_PLAY_PAUSE = "com.huanchengfly.tieba.post.PIP_PLAY_PAUSE"
private const val ACTION_PIP_REPLAY = "com.huanchengfly.tieba.post.PIP_REPLAY"

/** 平台宿主层：只处理方向、系统栏、PiP、屏幕常亮和导航 */
@UnstableApi
@Composable
fun FullscreenPage(
    args: FullscreenArgs,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? ComponentActivity ?: return
    val followScreenOrientation = LocalFullscreenFollowScreenOrientation.current
    val pipAutoEnterEnabled = LocalPipAutoEnterEnabled.current

    val pipSupported =
        remember {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        }

    // 记录进入全屏前的方向，退出时恢复
    val orientationState =
        remember(activity) {
            OrientationState(originalOrientation = activity.requestedOrientation)
        }
    var initialOrientation by rememberSaveable(args.videoUrl, args.syncGroupId) {
        mutableIntStateOf(resolveInitialOrientation(args, followScreenOrientation))
    }
    var requestedOrientation by rememberSaveable(args.videoUrl, args.syncGroupId) {
        mutableStateOf(args.requestedOrientation)
    }
    var lastIsPlaying by remember { mutableStateOf(args.playWhenReady) }
    var lastWasEnded by remember { mutableStateOf(false) }
    val requestedOrientationState = rememberUpdatedState(requestedOrientation)
    if (!orientationState.applied) {
        orientationState.applied = true
        activity.requestedOrientation = initialOrientation
    }

    val pipActions = remember { MutableSharedFlow<PipAction>(extraBufferCapacity = 2) }
    val fullscreenEvents = remember { MutableSharedFlow<FullscreenEvent>(extraBufferCapacity = 4) }

    DisposableEffect(Unit) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    when (intent.action) {
                        ACTION_PIP_PLAY_PAUSE -> pipActions.tryEmit(PipAction.PlayPause)
                        ACTION_PIP_REPLAY -> pipActions.tryEmit(PipAction.Replay)
                    }
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(ACTION_PIP_PLAY_PAUSE)
                addAction(ACTION_PIP_REPLAY)
            }
        ContextCompat.registerReceiver(activity, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            runCatching { activity.unregisterReceiver(receiver) }
        }
    }

    // 平台层观察 Activity 生命周期，通过事件流下发给内容层
    DisposableEffect(activity) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        fullscreenEvents.tryEmit(
                            FullscreenEvent.Pause(
                                isInPip = activity.isInPictureInPictureMode,
                            ),
                        )
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        fullscreenEvents.tryEmit(FullscreenEvent.Resume)
                    }

                    else -> {}
                }
            }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    val playerInput =
        remember(pipActions, FullscreenStore.pipState, fullscreenEvents, pipSupported) {
            FullscreenInput(
                pipActions = pipActions,
                pipState = FullscreenStore.pipState,
                fullscreenEvents = fullscreenEvents,
                pipSupported = pipSupported,
            )
        }

    val playerCallbacks =
        remember(activity, navController, orientationState, args, pipSupported, pipAutoEnterEnabled) {
            FullscreenCallbacks(
                onExit = {
                    navController.navigateUp()
                },
                onOrientation = { orientation ->
                    requestedOrientation = orientation
                    initialOrientation = orientation
                    activity.requestedOrientation = orientation
                },
                onPlaybackState = { isPlaying, wasEnded ->
                    lastIsPlaying = isPlaying
                    lastWasEnded = wasEnded
                    if (isPlaying) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    val shouldAutoEnterPip = pipAutoEnterEnabled && isPlaying && !wasEnded
                    val pipParams =
                        buildFullscreenPictureInPictureParams(
                            activity,
                            args,
                            isPlaying,
                            wasEnded,
                            shouldAutoEnterPip,
                        )
                    (activity as? MainActivityV2)?.syncAutoEnterPictureInPicture(
                        enabled = shouldAutoEnterPip,
                        params = pipParams,
                    )
                    updatePictureInPictureParams(
                        activity,
                        pipSupported,
                        pipParams,
                    )
                },
                onEnterPip = { isPlaying, wasEnded ->
                    val shouldAutoEnterPip = pipAutoEnterEnabled && isPlaying && !wasEnded
                    enterPictureInPicture(
                        activity,
                        pipSupported,
                        args,
                        isPlaying,
                        wasEnded,
                        shouldAutoEnterPip,
                    )
                },
                getRequestedOrientation = { requestedOrientationState.value },
            )
        }

    // 内容层先保存播放快照；宿主层最后恢复 Activity 状态
    DisposableEffect(Unit) {
        onDispose {
            val disabledPipParams =
                buildFullscreenPictureInPictureParams(
                    activity,
                    args,
                    lastIsPlaying,
                    lastWasEnded,
                    pipAutoEnterEnabled = false,
                )
            (activity as? MainActivityV2)?.syncAutoEnterPictureInPicture(
                enabled = false,
                params = disabledPipParams,
            )
            if (orientationState.applied) activity.requestedOrientation = orientationState.originalOrientation
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    FullscreenContent(
        args = args,
        input = playerInput,
        hooks = playerCallbacks,
    )
}

/** 解析全屏页初始方向：优先显式请求，其次跟随系统旋转，再按视频宽高兜底 */
private fun resolveInitialOrientation(
    args: FullscreenArgs,
    followScreenOrientation: Boolean,
): Int {
    args.requestedOrientation?.let {
        return it
    }
    if (followScreenOrientation) return ActivityInfo.SCREEN_ORIENTATION_FULL_USER
    if (args.videoWidth > 0 && args.videoHeight > 0 &&
        args.videoHeight > args.videoWidth
    ) {
        return ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    }
    return ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
}

/** 保存进入全屏前的 Activity 方向，只恢复一次 */
private class OrientationState(
    val originalOrientation: Int,
) {
    var applied: Boolean = false
}

/** 请求进入画中画模式 */
private fun enterPictureInPicture(
    activity: ComponentActivity,
    pipSupported: Boolean,
    args: FullscreenArgs,
    isPlaying: Boolean,
    wasEnded: Boolean,
    pipAutoEnterEnabled: Boolean,
) {
    if (!pipSupported || activity.isInPictureInPictureMode) return
    val pipParams =
        buildFullscreenPictureInPictureParams(
            activity,
            args,
            isPlaying,
            wasEnded,
            pipAutoEnterEnabled,
        )
    try {
        activity.enterPictureInPictureMode(pipParams)
    } catch (_: IllegalStateException) {
    } catch (_: SecurityException) {
    } catch (_: UnsupportedOperationException) {
    }
}

/** 刷新当前画中画窗口的参数 */
private fun updatePictureInPictureParams(
    activity: ComponentActivity,
    pipSupported: Boolean,
    pipParams: PictureInPictureParams,
) {
    if (!pipSupported) return
    if (activity.isInPictureInPictureMode) {
        activity.setPictureInPictureParams(pipParams)
    }
}

private fun buildFullscreenPictureInPictureParams(
    activity: ComponentActivity,
    args: FullscreenArgs,
    isPlaying: Boolean,
    wasEnded: Boolean,
    pipAutoEnterEnabled: Boolean,
): PictureInPictureParams {
    val aspectRatio = coercePipAspectRatio(args.videoWidth, args.videoHeight)
    return buildPictureInPictureParams(activity, isPlaying, wasEnded, aspectRatio, pipAutoEnterEnabled)
}

/** 计算并约束 Android 允许的 PiP 宽高比 */
private fun coercePipAspectRatio(
    videoWidth: Int,
    videoHeight: Int,
): Rational {
    if (videoWidth <= 0 || videoHeight <= 0) return Rational(16, 9)
    val ratio = videoWidth.toFloat() / videoHeight
    return when {
        ratio < 0.419f -> Rational(100, 239)
        ratio > 2.39f -> Rational(239, 100)
        else -> Rational(videoWidth, videoHeight)
    }
}

/** 构建 PiP 参数及播放/暂停/重播操作 */
private fun buildPictureInPictureParams(
    activity: ComponentActivity,
    isPlaying: Boolean,
    wasEnded: Boolean,
    aspectRatio: Rational,
    pipAutoEnterEnabled: Boolean,
): PictureInPictureParams {
    val (iconRes, titleRes, actionIntent) =
        when {
            wasEnded -> Triple(R.drawable.ic_replay, R.string.btn_replay, Intent(ACTION_PIP_REPLAY))
            isPlaying -> Triple(R.drawable.ic_pause, R.string.btn_pause, Intent(ACTION_PIP_PLAY_PAUSE))
            else -> Triple(R.drawable.ic_play_arrow, R.string.btn_play, Intent(ACTION_PIP_PLAY_PAUSE))
        }
    val title = activity.getString(titleRes)
    val action =
        RemoteAction(
            Icon.createWithResource(activity, iconRes),
            title,
            title,
            PendingIntent.getBroadcast(
                activity,
                0,
                actionIntent.setPackage(activity.packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
    return PictureInPictureParams
        .Builder()
        .setAspectRatio(aspectRatio)
        .setActions(listOf(action))
        .setSourceRectHint(activity.currentPipSourceRect())
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAutoEnterEnabled(pipAutoEnterEnabled)
            }
        }.build()
}

private fun ComponentActivity.currentPipSourceRect(): Rect =
    Rect().also { rect ->
        window.decorView.getGlobalVisibleRect(rect)
    }
