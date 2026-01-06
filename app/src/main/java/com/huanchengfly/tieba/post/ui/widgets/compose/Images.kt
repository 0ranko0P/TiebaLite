package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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