package com.huanchengfly.tieba.post.ui.widgets.compose.video

sealed class FullscreenCommand {
    data object Toggle : FullscreenCommand()

    data class Rotate(
        val orientation: Int,
    ) : FullscreenCommand()
}

/** 宿主页面负责将全屏意图转换为导航、Activity 方向和 Window 操作 */
interface FullscreenListener {
    fun onFullscreenCommand(event: FullscreenCommand)
}

/** 内嵌播放器进入全屏前先切换到 handoff 状态，再通知宿主启动全屏页 */
class FullscreenStarter(
    private val player: PlayerHandle,
    private val onFullscreen: (controllerId: Long, wasPlaying: Boolean) -> Unit,
) : FullscreenListener {
    override fun onFullscreenCommand(event: FullscreenCommand) {
        if (event !is FullscreenCommand.Toggle) return
        val wasPlaying = player.state.value.isPlaying
        player.prepareFullscreenHandoff()
        onFullscreen(player.controllerId, wasPlaying)
    }
}
