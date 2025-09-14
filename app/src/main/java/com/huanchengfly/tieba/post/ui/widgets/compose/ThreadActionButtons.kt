package com.huanchengfly.tieba.post.ui.widgets.compose

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
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

@Composable
private fun ThreadReplyBtn(
    modifier: Modifier = Modifier,
    replyNum: String,
    onClick: (() -> Unit)? = null,
) {
    ActionBtn(
        modifier = modifier,
        icon = {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_comment_new),
                contentDescription = stringResource(id = R.string.desc_comment),
            )
        },
        text = { Text(replyNum) },
        onClick = onClick
    )
}

@Composable
private fun ThreadAgreeBtn(
    modifier: Modifier = Modifier,
    hasAgree: Boolean,
    agreeNum: String,
    onClick: (() -> Unit)? = null,
) {
    val animatedColor by animateColorAsState(
        targetValue = if (hasAgree) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        label = "agreeBtnContentColor"
    )

    ActionBtn(
        icon = {
            Icon(
                imageVector = if (hasAgree) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.desc_like),
                tint = animatedColor
            )
        },
        text = {
            Text(text = agreeNum, color = animatedColor)
        },
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun ThreadShareBtn(
    modifier: Modifier = Modifier,
    shareNum: String,
    onClick: (() -> Unit)? = null
) {
    ActionBtn(
        icon = {
            Icon(
                imageVector = Icons.Rounded.SwapCalls,
                contentDescription = stringResource(id = R.string.desc_share),
            )
        },
        text = {
            Text(text = shareNum)
        },
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun ThreadActionButtonRow(
    modifier: Modifier = Modifier,
    shareText: String,
    replyText: String,
    agreeText: String,
    agreed: Boolean,
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
                shareNum = shareText,
                onClick = onShareClicked
            )

            ThreadReplyBtn(
                modifier = Modifier.weight(1f),
                replyNum = replyText,
                onClick = onReplyClicked,
            )

            ThreadAgreeBtn(
                modifier = Modifier.weight(1f),
                hasAgree = agreed,
                agreeNum = agreeText,
                onClick = onAgreeClicked,
            )
        }
    }
}

@Composable
fun ThreadActionButtonRow(
    modifier: Modifier = Modifier,
    shareNum: Long,
    replyNum: Int,
    agreeNum: Long,
    agreed: Boolean,
    onShareClicked: (() -> Unit)? = null,
    onReplyClicked: (() -> Unit)? = null,
    onAgreeClicked: (() -> Unit)? = null
) {
    ThreadActionButtonRow(
        modifier = modifier,
        shareText = if (shareNum == 0L) {
            stringResource(id = R.string.title_share)
        } else {
            remember { shareNum.getShortNumString() }
        },
        replyText = if (replyNum == 0) {
            stringResource(id = R.string.title_reply)
        } else {
            remember { replyNum.getShortNumString() }
        },
        agreeText = if (agreeNum == 0L) {
            stringResource(id = R.string.title_agree)
        } else {
            remember(agreeNum) { agreeNum.getShortNumString() }
        },
        agreed = agreed,
        onShareClicked = onShareClicked,
        onReplyClicked = onReplyClicked,
        onAgreeClicked = onAgreeClicked
    )
}

@Preview
@Composable
private fun ThreadActionButtonRowPreview() = TiebaLiteTheme {
    ThreadActionButtonRow(shareNum = 9, replyNum = 999, agreeNum = 99999,  agreed = true)
}
