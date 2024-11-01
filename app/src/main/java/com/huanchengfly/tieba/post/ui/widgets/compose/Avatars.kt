package com.huanchengfly.tieba.post.ui.widgets.compose

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.GlideUtil

object Sizes {
    val Tiny = 24.dp
    val Small = 36.dp
    val Medium = 48.dp
    val Large = 56.dp
}

@Composable
fun AvatarIcon(
    icon: ImageVector,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = backgroundColor)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarIcon(
    @DrawableRes
    resId: Int,
    size: Dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = ImageVector.vectorResource(id = resId),
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = backgroundColor)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarPlaceholder(size: Dp, modifier: Modifier = Modifier)
= Box(
    modifier = modifier
        .size(size)
        .placeholder(highlight = PlaceholderHighlight.fade(), shape = CircleShape)
)

@Composable
fun Avatar(
    data: String?,
    size: Dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    if (!data.isNullOrEmpty()) {
        Avatar(data, contentDescription, modifier.size(size), shape)
    } else {
        Avatar(R.drawable.ic_account, size, null, modifier)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Avatar(
    data: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    GlideImage(
        model = data,
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = ContentScale.Crop,
        failure = placeholder(R.drawable.ic_error),
        transition = CrossFade(TweenSpec())
    ) {
        it.addListener(GlideUtil.getDefaultErrorListener())
    }
}

@Composable
fun Avatar(
    @DrawableRes data: Int,
    size: Dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) = Image(
    painter = painterResource(id = data),
    contentDescription = contentDescription,
    modifier = modifier
        .size(size)
        .clip(CircleShape),
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