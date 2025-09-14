package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.NetworkObserver
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity.Companion.EXTRA_PHOTO_VIEW_DATA
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_DARKEN_IMAGE_WHEN_NIGHT_MODE
import com.huanchengfly.tieba.post.utils.GlideUtil
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil

fun shouldLoadImage(skipNetworkCheck: Boolean): Boolean {
    val imageLoadSettings = ImageUtil.imageLoadSettings
    return skipNetworkCheck
            || imageLoadSettings == ImageUtil.SETTINGS_SMART_ORIGIN
            || imageLoadSettings == ImageUtil.SETTINGS_ALL_ORIGIN
            || (imageLoadSettings == ImageUtil.SETTINGS_SMART_LOAD && NetworkObserver.isNetworkUnMetered)
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun NetworkImage(
    imageUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    skipNetworkCheck: Boolean = false,
    photoViewDataProvider: (() -> PhotoViewData?)? = null,
) {
    val context = LocalContext.current

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
        val darkenImage by rememberPreferenceAsState(
            key = booleanPreferencesKey(KEY_DARKEN_IMAGE_WHEN_NIGHT_MODE),
            defaultValue = true
        )
        val darkMode by ThemeUtil.darkModeState.collectAsStateWithLifecycle()

        GlideImage(
            model = imageUri,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale,
            colorFilter = if (darkenImage && darkMode) GlideUtil.DarkFilter else null,
            // transition = CrossFade
        ) {
            if (shouldLoadImage(skipNetworkCheck)) {
                it
            } else {
                it.onlyRetrieveFromCache(true)
            }
        }
    }
}