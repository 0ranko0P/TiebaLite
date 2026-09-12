package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.aspectRatio
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.theme.LocalExtendedColorScheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.utils.CoilUtil
import com.huanchengfly.tieba.post.utils.EmoticonManager

@DrawableRes
val DefaultErrorResource: Int = R.drawable.ic_error

@Composable
fun CircularLoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun PlaceholderRetry(onRetry: () -> Unit) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clickableNoIndication(onClick = onRetry),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_error),
            contentDescription = stringResource(R.string.desc_image_failed),
            modifier = Modifier.weight(1.0f).aspectRatio(1.0f)
        )

        Chip(
            text = stringResource(R.string.button_retry),
            prefixIcon = {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                )
            }
        )
    }
}

@NonRestartableComposable
@Composable
fun ErrorImage(modifier: Modifier = Modifier, tip: String) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painter = painterResource(R.drawable.ic_error), contentDescription = null)

        Text(
            text = tip,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                )
                .padding(4.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
@NonRestartableComposable
fun EmoticonInlineImage(
    id: String,
    size: Dp,
    modifier: Modifier = Modifier,
    description: String = id,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    Box(modifier = modifier.size(size)) {
        AsyncImage(
            model = EmoticonManager.getEmoticonUri(id),
            contentDescription = description,
            modifier = Modifier.matchParentSize(),
            error = painterResource(R.drawable.ic_error),
            onError = onError
        )
    }
}

@Composable
private fun PreviewImage(
    modifier: Modifier = Modifier,
    model: String,
    placeholder: MemoryCache.Key?,
    dimensions: IntSize,
) {
    val sizeResolver = rememberConstraintsSizeResolver()
    FullScreen {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .size(sizeResolver)
                    .placeholderMemoryCacheKey(placeholder)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build(),
            )

            val state by painter.state.collectAsState()
            when (state) {
                is AsyncImagePainter.State.Empty,
                is AsyncImagePainter.State.Loading, // Show memory cache as placeholder
                is AsyncImagePainter.State.Success -> {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .block {
                                // Fill width/height based on current orientation
                                if (maxHeight > maxWidth) fillMaxWidth() else fillMaxHeight()
                            }
                            .aspectRatio(ratio = dimensions.aspectRatio ?: 1f)
                            .then(sizeResolver),
                        contentScale = if (maxHeight > maxWidth) ContentScale.FillWidth else ContentScale.FillHeight,
                    )

                    if (state !is AsyncImagePainter.State.Success) {
                        CircularLoadingPlaceholder()
                    }
                }

                is AsyncImagePainter.State.Error -> {
                    ErrorImage(
                        tip = (state as AsyncImagePainter.State.Error).result.throwable.getErrorMessage()
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    dimensions: IntSize? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    photoViewDataProvider: (() -> PhotoViewData?)? = null,
) {
    if (imageUrl.isEmpty()) {
        ErrorImage(modifier, tip = stringResource(R.string.desc_image_empty_url))
        return
    }

    val context = LocalContext.current
    val darkenImage = LocalUISettings.current.darkenImage && LocalExtendedColorScheme.current.darkTheme
    var isLongPressing by remember { mutableStateOf(false) }

    val sizeResolver = rememberConstraintsSizeResolver()
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(sizeResolver)
            .build(),
        contentScale = contentScale,
    )

    Box(
        modifier = modifier
            .then(sizeResolver)
            .onNotNull(photoViewDataProvider) { dataProvider ->
                pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if ((dimensions?.aspectRatio ?: 1f) <= 0.1f) {
                                context.toastShort(R.string.toast_preview_image_too_large)
                            } else {
                                isLongPressing = true
                            }
                        },
                        onPress = {
                            tryAwaitRelease()
                            isLongPressing = false
                        },
                        onTap = {
                            val photos = dataProvider() ?: return@detectTapGestures
                            // bug from caller
                            if (photos.data != null && photos.data.forumName.isEmpty()) {
                                context.toastShort(R.string.title_unknown_error)
                            } else {
                                PhotoViewActivity.launch(context, photos)
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val state by painter.state.collectAsState()
        when (state) {
            is AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                )
            }

            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier.matchParentSize(),
                    contentScale = contentScale,
                    colorFilter = if (darkenImage) CoilUtil.DarkFilter else null,
                )

                if (dimensions != null) {
                    val previewAlpha by animateFloatAsState(
                        targetValue = if (isLongPressing) 1.0f else 0f,
                        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                    )
                    val previewVisible by remember { derivedStateOf { previewAlpha > 0.01f } }
                    if (previewVisible) {
                        PreviewImage(
                            modifier = Modifier.graphicsLayer {
                                alpha = previewAlpha
                            },
                            model = remember(photoViewDataProvider) {
                                photoViewDataProvider?.invoke()?.data?.originUrl ?: imageUrl
                            },
                            placeholder = (state as AsyncImagePainter.State.Success).result.memoryCacheKey,
                            dimensions = dimensions,
                        )
                    }
                }
            }

            is AsyncImagePainter.State.Error -> {
                PlaceholderRetry(onRetry = painter::restart)
            }
        }
    }
}