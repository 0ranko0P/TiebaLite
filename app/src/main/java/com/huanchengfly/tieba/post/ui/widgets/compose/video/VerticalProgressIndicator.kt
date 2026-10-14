package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.core.ui.FloatProducer

@Composable
fun VerticalProgressIndicator(
    value: FloatProducer,
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    icon: Painter,
    colors: SliderColors = SliderDefaults.colors(),
) {
    Box(
        modifier = modifier
            .size(width = width, height = 250.dp)
            .clip(MaterialTheme.shapes.large)
            .drawWithContent {
                with(drawContext.canvas.nativeCanvas) {
                    drawRect(colors.activeTrackColor)
                    val checkpoint = saveLayer(null, null)
                    drawContent()
                    drawRect(
                        color = colors.inactiveTrackColor,
                        size = Size(
                            width = size.width,
                            height = size.height - size.height * value().coerceIn(0f, 1f)
                        ),
                        blendMode = BlendMode.SrcOut
                    )
                    restoreToCount(checkpoint)
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Preview
@Composable
private fun VerticalProgressIndicatorPreview() {
    MaterialTheme {
        VerticalProgressIndicator(
            value = { 0.1f },
            icon = rememberVectorPainter(Icons.Rounded.BrightnessLow)
        )
    }
}