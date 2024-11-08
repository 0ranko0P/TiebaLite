package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.NetworkObserver
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity.Companion.EXTRA_PHOTO_VIEW_DATA
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_DARKEN_IMAGE_WHEN_NIGHT_MODE
import com.huanchengfly.tieba.post.utils.GlideUtil
import com.huanchengfly.tieba.post.utils.ImageUtil

fun shouldLoadImage(skipNetworkCheck: Boolean): Boolean {
    val imageLoadSettings = ImageUtil.imageLoadSettings
    return skipNetworkCheck
            || imageLoadSettings == ImageUtil.SETTINGS_SMART_ORIGIN
            || imageLoadSettings == ImageUtil.SETTINGS_ALL_ORIGIN
            || (imageLoadSettings == ImageUtil.SETTINGS_SMART_LOAD && NetworkObserver.isNetworkUnMetered)
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
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .clip(RoundedCornerShape(6.dp)),
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
    photoViewData: PhotoViewData? = null,
    contentScale: ContentScale = ContentScale.Fit,
    skipNetworkCheck: Boolean = false,
    enablePreview: Boolean = false,
) {
    val context = LocalContext.current
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
                        photoViewData?.let { photos ->
                            // bug from caller
                            if (photos.data != null && photos.data.forumName.isEmpty()) {
                                context.toastShort(R.string.title_unknown_error); return@let
                            }
                            context.goToActivity<PhotoViewActivity> { putExtra(EXTRA_PHOTO_VIEW_DATA, photos) }
                        }
                    }
                )
            }
    ) {
        val darkenImage by rememberPreferenceAsState(
            key = booleanPreferencesKey(KEY_DARKEN_IMAGE_WHEN_NIGHT_MODE),
            defaultValue = true
        )
        GlideImage(
            model = imageUri,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            colorFilter = if (darkenImage && ExtendedTheme.colors.isNightMode) GlideUtil.DarkFilter else null,
            transition = CrossFade
        ) {
            if (shouldLoadImage(skipNetworkCheck)) {
                it
            } else {
                it.onlyRetrieveFromCache(true)
            }
        }
    }

    if (enablePreview) {
        val previewAlpha by animateFloatAsState(
            targetValue = if (isLongPressing) 1.0f else 0f,
            animationSpec = SpringSpec(),
            label = "AnimatePreviewAlphaAsState"
        )

        if (previewAlpha != 0f) {
            PreviewImage(
                modifier = Modifier.alpha(previewAlpha),
                imageUri = imageUri,
                originImageUri = photoViewData?.data?.originUrl
            )
        }
    }
}

@Composable
fun NetworkImage(
    imageUriProvider: () -> String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    photoViewDataProvider: (() -> PhotoViewData)? = null,
    contentScale: ContentScale = ContentScale.Fit,
    skipNetworkCheck: Boolean = false,
    enablePreview: Boolean = false,
) {
    val imageUri by rememberUpdatedState(newValue = imageUriProvider())
    val photoViewData by rememberUpdatedState(newValue = photoViewDataProvider?.invoke())

    NetworkImage(
        imageUri = imageUri,
        contentDescription = contentDescription,
        modifier = modifier,
        photoViewData = photoViewData,
        contentScale = contentScale,
        skipNetworkCheck = skipNetworkCheck,
        enablePreview = enablePreview
    )
}