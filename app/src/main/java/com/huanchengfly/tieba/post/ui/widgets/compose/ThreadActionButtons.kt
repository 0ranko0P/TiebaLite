package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.icons.CommentNew
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString

@Composable
private fun ActionBtn(
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
    text: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .onNotNull(onClick) { clickable(onClick = it) }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center,
            content = icon
        )

        Spacer(modifier = Modifier.width(8.dp))

        text()
    }
}

@NonRestartableComposable
@Composable
private fun ThreadReplyBtn(
    modifier: Modifier = Modifier,
    replies: String,
    onClick: (() -> Unit)? = null,
) {
    ActionBtn(
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Rounded.CommentNew,
                contentDescription = stringResource(id = R.string.desc_comment),
            )
        },
        text = { Text(replies) },
        onClick = onClick
    )
}

@NonRestartableComposable
@Composable
private fun ThreadLikeBtn(
    modifier: Modifier = Modifier,
    liked: Boolean,
    likes: String,
    onClick: (() -> Unit)? = null,
) {
    val animatedColor by animateColorAsState(
        targetValue = if (liked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        label = "agreeBtnContentColor"
    )

    ActionBtn(
        icon = {
            Icon(
                imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.button_like),
                tint = animatedColor
            )
        },
        text = {
            Text(text = likes, color = animatedColor)
        },
        modifier = modifier,
        onClick = onClick
    )
}

@NonRestartableComposable
@Composable
fun ThreadShareBtn(
    modifier: Modifier = Modifier,
    shares: String,
    onClick: (() -> Unit)? = null
) {
    ActionBtn(
        icon = {
            Icon(
                imageVector = Icons.Rounded.SwapCalls,
                contentDescription = stringResource(id = R.string.title_share),
            )
        },
        text = { Text(text = shares) },
        modifier = modifier,
        onClick = onClick,
    )
}

@Stable
@Composable
private fun rememberShortNumString(number: Long, @StringRes defaultRes: Int): String {
    return if (number <= 0) {
        stringResource(id = defaultRes)
    } else if (number <= 999) {
        number.toString()
    } else {
        remember { number.getShortNumString() }
    }
}

@Composable
fun ThreadActionButtonRow(
    modifier: Modifier = Modifier,
    shares: Long,
    replies: Int,
    likes: Long,
    liked: Boolean,
    onShareClicked: (() -> Unit)? = null,
    onReplyClicked: (() -> Unit)? = null,
    onAgreeClicked: (() -> Unit)? = null
) {
    ProvideContentColorTextStyle(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        textStyle = MaterialTheme.typography.bodySmall
    ) {
        Row(modifier = modifier.fillMaxWidth()) {
            ThreadShareBtn(
                modifier = Modifier.weight(1f),
                shares = rememberShortNumString(shares, R.string.title_share),
                onClick = onShareClicked
            )

            ThreadReplyBtn(
                modifier = Modifier.weight(1f),
                replies = rememberShortNumString(replies.toLong(), R.string.title_reply),
                onClick = onReplyClicked,
            )

            ThreadLikeBtn(
                modifier = Modifier.weight(1f),
                liked = liked,
                likes = rememberShortNumString(likes, R.string.button_like),
                onClick = onAgreeClicked,
            )
        }
    }
}

@Preview
@Composable
private fun ThreadActionButtonRowPreview() = TiebaLiteTheme {
    ThreadActionButtonRow(shares = 9, replies = 999, likes = 99999, liked = true)
}
