package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.Placeholder
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.model.GlideUrl
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.NetworkObserver
import com.huanchengfly.tieba.post.components.glide.TbGlideUrl
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.theme.LocalExtendedColorScheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity.Companion.EXTRA_PHOTO_VIEW_DATA
import com.huanchengfly.tieba.post.utils.GlideUtil
import com.huanchengfly.tieba.post.utils.ImageUtil

@OptIn(ExperimentalGlideComposeApi::class)
val CircularLoadingPlaceholder: Placeholder = placeholder {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
fun placeholderRetry(onRetry: () -> Unit): Placeholder = placeholder {
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

@Composable
private fun shouldLoadImage(): Boolean {
    return when (val loadType = LocalHabitSettings.current.imageLoadType) {
        ImageUtil.SETTINGS_SMART_LOAD -> {
            NetworkObserver.isNetworkUnmetered.collectAsStateWithLifecycle().value
        }

        ImageUtil.SETTINGS_SMART_ORIGIN, ImageUtil.SETTINGS_ALL_ORIGIN -> true

        else -> throw IllegalArgumentException("Unknow image load type: $loadType")
    }
}

@NonRestartableComposable
@Composable
fun ErrorImage(modifier: Modifier = Modifier, tip: String) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun PreviewImage(
    modifier: Modifier = Modifier,
    model: GlideUrl,
    originModelProvider: () -> GlideUrl?,
    dimensions: IntSize,
) {
    val context = LocalContext.current
    val originModel = remember {
        originModelProvider()?.takeIf { it != model } ?: model
    }

    val aspectRatio = if (dimensions != IntSize.Zero && dimensions.width > 0) {
        dimensions.width / dimensions.height.toFloat()
    } else {
        1f
    }

    FullScreen {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            GlideImage(
                model = originModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio = aspectRatio),
                contentScale = ContentScale.Crop,
                failure = GlideUtil.DefaultErrorPlaceholder
            ) {
                if (originModel === model) {
                    it
                } else {
                    it.thumbnail(Glide.with(context).load(model))
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    dimensions: IntSize? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    photoViewDataProvider: (() -> PhotoViewData?)? = null,
) {
    val context = LocalContext.current
    val shouldLoadImage = shouldLoadImage()
    val darkenImage = LocalUISettings.current.darkenImage && LocalExtendedColorScheme.current.darkTheme
    var isLongPressing by remember { mutableStateOf(false) }
    val model = TbGlideUrl(imageUrl)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { isLongPressing = true },
                    onPress = {
                        tryAwaitRelease()
                        isLongPressing = false
                    },
                    onTap = {
                        // Launch PhotoViewActivity now, ignore image load settings
                        val photos = photoViewDataProvider?.invoke() ?: return@detectTapGestures
                        // bug from caller
                        if (photos.data != null && photos.data.forumName.isEmpty()) {
                            context.toastShort(R.string.title_unknown_error)
                        } else {
                            context.goToActivity<PhotoViewActivity> {
                                putExtra(EXTRA_PHOTO_VIEW_DATA, photos)
                            }
                        }
                    }
                )
            }
    ) {
        // Note: 用户报告 'Glide 网络恢复时自动重试' 会在一加系统中失效.
        // 添加 placeholderRetry 让用户手动重试加载图片
        var retryCount by remember { mutableIntStateOf(0) }
        key(model, retryCount) {
            GlideImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                colorFilter = if (darkenImage) GlideUtil.DarkFilter else null,
                loading = CircularLoadingPlaceholder,
                failure = placeholderRetry { retryCount++ },
                // transition = CrossFade
            ) {
                if (shouldLoadImage || retryCount > 0) it else it.onlyRetrieveFromCache(true)
            }
        }
    }

    if (dimensions != null) {
        val previewAlpha by animateFloatAsState(targetValue = if (isLongPressing) 1.0f else 0f)
        val previewVisible by remember { derivedStateOf { isLongPressing || previewAlpha > 0.01f } }

        if (previewVisible) {
            PreviewImage(
                modifier = Modifier.graphicsLayer {
                    alpha = previewAlpha
                },
                model = model,
                originModelProvider = {
                    photoViewDataProvider?.invoke()?.data?.originUrl?.let { TbGlideUrl(url = it) }
                },
                dimensions = dimensions,
            )
        }
    }
}