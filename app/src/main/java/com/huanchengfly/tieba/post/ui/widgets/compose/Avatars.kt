package com.huanchengfly.tieba.post.ui.widgets.compose

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.GlideUtil

object Sizes {
    val Tiny = 24.dp
    val Small = 36.dp
    val Medium = 48.dp
    val Large = 56.dp
}

@NonRestartableComposable
@Composable
fun AvatarPlaceholder(size: Dp, modifier: Modifier = Modifier)  {
    Box(
        modifier = modifier
            .size(size)
            .placeholder(shape = CircleShape)
    )
}

@Composable
fun Avatar(
    data: String?,
    size: Dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    if (!data.isNullOrEmpty()) {
        Avatar(modifier.size(size), data, contentDescription, shape)
    } else {
        Avatar(R.drawable.ic_account, size, null, modifier)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    data: Any?,
    contentDescription: String? = null,
    shape: Shape = CircleShape
) =
    // TODO: Unbox when glide-compose stable released
    Box(
        modifier = modifier.clip(shape)
    ) {
        GlideImage(
            model = data,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            failure = GlideUtil.DefaultErrorPlaceholder,
        )
    }

@Composable
fun Avatar(
    @DrawableRes data: Int,
    size: Dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape
) = Image(
    painter = painterResource(id = data),
    contentDescription = contentDescription,
    modifier = modifier
        .size(size)
        .clip(shape = shape),
    contentScale = ContentScale.Crop
)

@Composable
fun Avatar(
    data: Drawable,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberDrawablePainter(drawable = data),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}