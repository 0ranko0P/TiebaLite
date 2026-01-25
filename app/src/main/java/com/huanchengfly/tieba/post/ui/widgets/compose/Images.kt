package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.NetworkObserver
import com.huanchengfly.tieba.post.components.glide.TbGlideUrl
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.theme.LocalExtendedColorScheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity.Companion.EXTRA_PHOTO_VIEW_DATA
import com.huanchengfly.tieba.post.utils.GlideUtil
import com.huanchengfly.tieba.post.utils.ImageUtil

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
private fun PreviewImage(modifier: Modifier = Modifier, imageUri: String, originImageUri: String?) {
    val context = LocalContext.current
    val originRequest = remember {
        if (originImageUri.isNullOrEmpty() || originImageUri == imageUri) imageUri else originImageUri
    }

    FullScreen {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .clip(MaterialTheme.shapes.small),
        ) {
            GlideImage(
                model = originRequest,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.ic_error)
            ) {
                if (originImageUri == imageUri) return@GlideImage it
                it.thumbnail(
                    Glide.with(context).load(imageUri)
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun NetworkImage(
    imageUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    enablePreview: Boolean = false,
    photoViewDataProvider: (() -> PhotoViewData?)? = null,
) {
    val context = LocalContext.current
    val shouldLoadImage = shouldLoadImage()
    val darkenImage = LocalUISettings.current.darkenImage && LocalExtendedColorScheme.current.darkTheme
    var isLongPressing by remember { mutableStateOf(false) }

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
        GlideImage(
            model = TbGlideUrl(imageUri),
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale,
            colorFilter = if (darkenImage) GlideUtil.DarkFilter else null,
            failure = GlideUtil.DefaultErrorPlaceholder,
            // transition = CrossFade
        ) {
            if (shouldLoadImage) it else it.onlyRetrieveFromCache(true)
        }
    }

    if (enablePreview) {
        val previewAlpha by animateFloatAsState(targetValue = if (isLongPressing) 1.0f else 0f)

        if (isLongPressing) {
            PreviewImage(
                modifier = Modifier.graphicsLayer {
                    alpha = previewAlpha
                },
                imageUri = imageUri,
                originImageUri = photoViewDataProvider?.invoke()?.data?.originUrl
            )
        }
    }
}