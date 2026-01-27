package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.block

@Composable
fun ChipText(
    modifier: Modifier = Modifier,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = contentColorFor(containerColor),
    onClick: (() -> Unit)? = null,
){
    Box(
        modifier = modifier
            .block {
                if (onClick != null) {
                    clip(CircleShape).background(containerColor).clickable(onClick = onClick)
                } else {
                    background(color = containerColor, shape = CircleShape)
                }
            }
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            color = contentColor,
            text = text,
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    prefixIcon: (@Composable BoxScope.() -> Unit)? = null,
    appendIcon: (@Composable BoxScope.() -> Unit)? = null,
    invertColor: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val color = if (invertColor) colors.onPrimary else colors.onSecondaryContainer
    val containerColor = if (invertColor) colors.primary else colors.secondaryContainer

    if (prefixIcon == null && appendIcon == null) {
        ChipText(modifier, text, containerColor, color, onClick)
        return
    }

    Row(
        modifier = modifier
            .block {
                if (onClick != null) {
                    clip(CircleShape).background(containerColor).clickable(onClick = onClick)
                } else {
                    background(color = containerColor, shape = CircleShape)
                }
            }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            prefixIcon?.let {
                Box(modifier = Modifier.size(16.dp), content = it)
            }

            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )

            appendIcon?.let {
                Box(modifier = Modifier.size(16.dp), content = it)
            }
        }
    }
}