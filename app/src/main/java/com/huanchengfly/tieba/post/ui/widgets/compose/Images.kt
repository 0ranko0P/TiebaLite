package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
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

private fun shouldLoadImage(imageLoadSettings: Int): Boolean {
    return imageLoadSettings == ImageUtil.SETTINGS_SMART_ORIGIN
            || imageLoadSettings == ImageUtil.SETTINGS_ALL_ORIGIN
            || (imageLoadSettings == ImageUtil.SETTINGS_SMART_LOAD && NetworkObserver.isNetworkUnmetered)
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun NetworkImage(
    imageUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    photoViewDataProvider: (() -> PhotoViewData?)? = null,
) {
    val context = LocalContext.current
    val imageLoadSettings = LocalHabitSettings.current.imageLoadType
    val darkenImage = LocalUISettings.current.darkenImage && LocalExtendedColorScheme.current.darkTheme

    Box(
        modifier = modifier
            .clickableNoIndication {
                // Launch PhotoViewActivity now, ignore image load settings
                val photos = photoViewDataProvider?.invoke() ?: return@clickableNoIndication

                if (photos.data != null && photos.data.forumName.isEmpty()) { // bug from caller
                    context.toastShort(R.string.title_unknown_error)
                } else {
                    context.goToActivity<PhotoViewActivity> {
                        putExtra(EXTRA_PHOTO_VIEW_DATA, photos)
                    }
                }
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
            if (NetworkObserver.isNetworkConnected && shouldLoadImage(imageLoadSettings)) {
                it
            } else {
                it.onlyRetrieveFromCache(true)
            }
        }
    }
}