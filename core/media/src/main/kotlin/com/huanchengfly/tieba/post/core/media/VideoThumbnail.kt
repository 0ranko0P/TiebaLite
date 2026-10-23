package com.huanchengfly.tieba.post.core.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import com.huanchengfly.tieba.post.core.designsystem.theme.Grey100
import com.huanchengfly.tieba.post.core.ui.R as CoreUiR
import com.huanchengfly.tieba.post.core.ui.modifier.clickableNoIndication
import com.huanchengfly.tieba.post.core.ui.modifier.onNotNull
import androidx.media3.ui.compose.material3.R as media3UiR

@Composable
fun VideoThumbnail(
    modifier: Modifier = Modifier,
    thumbnailUrl: String?,
    contentScale: ContentScale = ContentScale.FillWidth,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.onNotNull(onClick) { clickableNoIndication(onClick = it) },
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(false)
                    .error(CoreUiR.drawable.ic_error)
                    .build(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale
            )
        }

        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(id = media3UiR.string.playpause_button_play),
            modifier = Modifier.size(48.dp),
            tint = Grey100
        )
    }
}
