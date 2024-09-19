package com.huanchengfly.tieba.post.ui.common.theme.compose

import android.os.Build
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf

// TiebaBlue + Purple
val TiebaBackgorundColors by lazy {
    persistentListOf(Color(0xFF4477E0), Color.White, Color(0xff64029e))
}

@Composable
fun genPaletteColors(): List<Color> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicTonalPalette(LocalContext.current).run {
            remember { persistentListOf(primary80, secondary80, Color.LightGray, tertiary80, primary80) }
        }
    } else {
        TiebaBackgorundColors
    }
}

@NonRestartableComposable
@Composable
fun rememberAnimatedGradientBrush(colors: List<Color> = genPaletteColors()): State<Brush> {
    val brushSize = 600.0f
    val transition = rememberInfiniteTransition(label = "BrushOffsetTransition")
    val targetOffset = with(LocalDensity.current) { 1200.dp.toPx() }
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DefaultDurationMillis * 100, easing = LinearEasing),
        ),
        label = "OffsetTransition"
    )

    return remember { mutableStateOf(Brush.linearGradient(emptyList())) }.apply {
        value = Brush.linearGradient(
            colors = colors,
            start = Offset(offset, offset),
            end = Offset(offset + brushSize, offset + brushSize),
            tileMode = TileMode.Mirror
        )
    }
}

private fun Modifier.background(brush: Brush, shape: Shape = RectangleShape): Modifier =
    this then drawWithCache {
        onDrawBehind {
            if (shape === RectangleShape) {
                drawRect(brush)
            } else {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline, brush)
            }
        }
    }

/**
 * Draw the given palette colors as a background with animated gradient effect.
 *
 * @see genPaletteColors
 * @see Modifier.blur
 *
 * @param colors Palette colors
 * @param shape Shape of this background
 * @param blurRadius Blur Radius of the gradient background, only works on Android 12 and above
 * */
@Composable
fun PaletteBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = genPaletteColors(),
    shape: Shape = RectangleShape,
    blurRadius: Dp = 48.dp,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > Dp.Hairline) {
        Modifier.blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        null
    }

    val brush by rememberAnimatedGradientBrush(colors)
    val backgroundMod = (blurMod ?: Modifier).background(brush)
    val clipModifier = if (shape != RectangleShape) Modifier.clip(shape) else Modifier

    Box(modifier = modifier then clipModifier) {
        // Avoid blurring the content
        Box(modifier = Modifier.fillMaxSize() then backgroundMod)

        if (content != null) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}