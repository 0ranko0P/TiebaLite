package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication

/** 视频封面图与播放按钮 */
@Composable
fun VideoCover(
    modifier: Modifier = Modifier,
    url: String?,
    contentScale: ContentScale = ContentScale.FillWidth,
    onClick: (() -> Unit)? = null,
    showPlay: Boolean = true,
) {
    BoxWithConstraints(
        modifier =
            if (onClick != null) {
                modifier.clickableNoIndication(onClick = onClick)
            } else {
                modifier
            },
        contentAlignment = Alignment.Center,
    ) {
        val playSize = (minOf(maxWidth, maxHeight) * 0.28f).coerceIn(24.dp, 72.dp)

        AsyncImage(
            model = url,
            contentDescription = stringResource(R.string.desc_video),
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale,
        )

        if (showPlay) {
            ShadowedIcon(
                painter = painterResource(R.drawable.ic_play_arrow),
                modifier = Modifier.size(playSize),
            )
        }
    }
}
