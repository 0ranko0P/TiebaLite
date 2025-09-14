package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.utils.appPreferences

@Composable
fun BlockTip(
    text: @Composable () -> Unit = { Text(text = stringResource(id = R.string.tip_blocked_content)) },
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceDim, MaterialTheme.shapes.extraSmall)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        ProvideContentColorTextStyle(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            textStyle = MaterialTheme.typography.bodySmall,
            content = text
        )
    }
}

@Composable
fun BlockableContent(
    blocked: Boolean,
    modifier: Modifier = Modifier,
    blockedTip: @Composable BoxScope.() -> Unit = { BlockTip() },
    hideBlockedContent: Boolean = LocalContext.current.appPreferences.hideBlockedContent,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!blocked) {
        Box(modifier = modifier, content = content)
    } else if (!hideBlockedContent) {
        Box(modifier = modifier, content = blockedTip)
    }
}