package com.huanchengfly.tieba.post.activities

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestListener
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.Grey800
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.colorscheme.translucentColorScheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.PaletteBackground
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.page.settings.AboutPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Measurer
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.ColorPickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.ColorUtils
import com.huanchengfly.tieba.post.utils.DisplayUtil
import com.huanchengfly.tieba.post.utils.DisplayUtil.toDpSize
import kotlin.math.abs

private val ColorThumbSize = DpSize(4.dp, 38.dp)

@Composable
fun TranslucentThemeContent(
    modifier: Modifier = Modifier,
    viewModel: TranslucentThemeViewModel,
    state: UiState,
    onSelectWallpaper: () ->Unit
) {
    val colorPickerState = rememberDialogState()

    TranslucentThemeContent(
        modifier = modifier,
        accent = state.primaryColor,
        onColorPicked = viewModel::onColorPicked,
        colorPalette = state.colorPalette,
        isDarkTheme = state.isDarkTheme,
        onColorModeChanged = viewModel::onColorModeChanged,
        alpha = state.alpha,
        onAlphaChanged = viewModel::onAlphaChanged,
        blur = state.blur,
        onBlurChanged = viewModel::onBlurChanged,
        onSelectWallpaper = onSelectWallpaper,
        onLaunchColorPicker = colorPickerState::show
    )

    if (colorPickerState.show) {
        ColorPickerDialog(
            state = colorPickerState,
            title = R.string.title_color_picker_primary,
            initial = state.primaryColor,
            onColorChanged = viewModel::onColorPicked
        )
    }
}

@Composable
private fun TranslucentThemeContent(
    modifier: Modifier = Modifier,
    accent: Color,
    onColorPicked: (Color) -> Unit = {},
    colorPalette: List<Color>,
    isDarkTheme: Boolean,
    onColorModeChanged: () -> Unit = {},
    alpha: Float,
    onAlphaChanged: (Float) -> Unit = {},
    blur: Float,
    onBlurChanged: (Float) -> Unit = {},
    onSelectWallpaper: () -> Unit = {},
    onLaunchColorPicker: () -> Unit = {}
) {
    val maxWidth = Modifier.fillMaxWidth()
    Column(modifier = modifier) {
        val buttonColor = ButtonDefaults.elevatedButtonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        )

        TextButton(
            onClick = onSelectWallpaper,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = buttonColor
        ) {
            Text(stringResource(R.string.title_select_pic), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectableTextButton(
                modifier = Modifier.weight(0.5f),
                text = R.string.dark_color,
                colors = buttonColor,
                selected = isDarkTheme,
                onClick = onColorModeChanged
            )

            Spacer(modifier = Modifier.width(12.dp))

            SelectableTextButton(
                modifier = Modifier.weight(0.5f),
                text = R.string.light_color,
                colors = buttonColor,
                selected = !isDarkTheme,
                onClick = onColorModeChanged
            )
        }

        AnimatedVisibility(visible = colorPalette.isNotEmpty()) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.title_select_color),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                ColorPanel(colorPalette, accent, onColorPicked, onLaunchColorPicker)
            }
        }

        ImageFilterPanel(
            color = accent,
            alpha = alpha,
            onAlphaChanged = onAlphaChanged,
            blur = blur,
            onBlurChanged = onBlurChanged,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = maxWidth.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Red
                )
                Text(
                    text = stringResource(R.string.title_translucent_theme_experimental_feature),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Display picked background wallpaper side by side, but later one having an [AboutPage] as overlay
 *
 * @see WallpaperOverlay
 * */
@Composable
fun SideBySideWallpaper(
    modifier: Modifier = Modifier,
    sideBySide: Boolean = !DisplayUtil.isLandscape,
    wallpaper: Uri?,
    alpha: Float,
    primary: Color,
    isDarkTheme: Boolean,
    transformation: BitmapTransformation?,
    placeHolder: () -> Drawable?,
    listener: RequestListener<Drawable>?
) {
    val cornerShape = MaterialTheme.shapes.medium
    val density = LocalDensity.current
    val context = LocalContext.current
    val screen: DpSize = remember { DisplayUtil.getScreenPixels(context).toDpSize(density) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
    ) {

        // Constraint size by screen aspect ratio
        val wallpaperModifier = Modifier
            .block {
                if (sideBySide) weight(1f, false) else fillMaxHeight()
            }
            .aspectRatio(ratio = screen.width / screen.height)
            .background(Color.Black, shape = cornerShape)
            .shadow(6.dp, shape = cornerShape)

        if (sideBySide) {
            Wallpaper(wallpaperModifier, wallpaper, alpha, transformation, placeHolder, listener = listener)
        }

        Measurer(modifier = wallpaperModifier) { size ->
            Wallpaper(Modifier.matchParentSize(), wallpaper, alpha, transformation, placeHolder, listener = null)

            val sizeInDp = size?.toDpSize(density) ?: return@Measurer
            WallpaperOverlay(screen, sizeInDp, primary, isDarkTheme)
        }
    }
}

@Composable
private fun Wallpaper(
    modifier: Modifier = Modifier,
    wallpaper: Uri?,
    alpha: Float,
    trans: BitmapTransformation?,
    placeHolder: () -> Drawable?,
    listener: RequestListener<Drawable>?
) {
    if (wallpaper != null) {
        GlideImage(
            model = wallpaper,
            contentDescription = null,
            modifier = modifier,
            alpha = alpha
        ) {
            val loadingPlaceHolder = placeHolder()
            var builder = it
            if (trans != null) builder = builder.transform(trans)
            if (listener != null) builder = builder.addListener(listener)
            if (loadingPlaceHolder != null) builder = builder.placeholder(loadingPlaceHolder)
            return@GlideImage builder.diskCacheStrategy(DiskCacheStrategy.NONE)
        }
    } else {
        PaletteBackground(modifier, shape = MaterialTheme.shapes.medium, blurRadius = 100.dp)
    }
}

/**
 * Compose [AboutPage] as overlay and scale to the [targetSize]
 *
 * @param isDarkColor Dark/Light text to simulate
 * */
@Composable
private fun WallpaperOverlay(
    screen: DpSize,
    targetSize: DpSize,
    primary: Color,
    isDarkColor: Boolean
) {
    val colors = remember(primary, isDarkColor) {
        translucentColorScheme(primary, colorMode = isDarkColor).lightColor
    }
    val typography = MaterialTheme.typography.run { // Scale font size up
        copy(
            labelLarge = labelLarge.copy(fontSize = labelLarge.fontSize * 1.3f),
            bodySmall = bodySmall.copy(fontSize = bodySmall.fontSize * 1.3f)
        )
    }

    MaterialTheme(colorScheme = colors, typography = typography) {
        val scale = ScaleFactor(targetSize.width / screen.width, targetSize.height / screen.height)
        AboutPage(
            modifier = Modifier
                .size(targetSize)
                .requiredSize(screen)
                .graphicsLayer {
                    scaleX = scale.scaleX
                    scaleY = scale.scaleY
                }
        )
    }
}

@Composable
private fun SelectableTextButton(
    modifier: Modifier = Modifier,
    @StringRes text: Int,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (selected) 1.025f else 1f,
        animationSpec = TweenSpec(easing = LinearOutSlowInEasing),
        label = "SelectableTextButtonAnimation"
    )

    TextButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scaleAnim
            scaleY = scaleAnim
        },
        enabled = !selected,
        shape = MaterialTheme.shapes.medium,
        colors = colors
    ) {
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null
        )
        Text(
            text = stringResource(text),
            color = LocalContentColor.current,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColorSliderResetButton(modifier: Modifier = Modifier, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = modifier.size(ColorThumbSize.height).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = tint)
    }
}

@Composable
private fun ImageFilterPanel(
    color: Color,
    alpha: Float,
    onAlphaChanged: (Float) -> Unit = {},
    blur: Float,
    onBlurChanged: (Float) -> Unit = {},
) {
    val titleStyle = MaterialTheme.typography.titleSmall
    val accentColorAnim by animateColorAsState(color, spring(stiffness = Spring.StiffnessLow))
    val sliderColors = SliderDefaults.colors(
        activeTrackColor = accentColorAnim,
        inactiveTrackColor = accentColorAnim.copy(0.3f),
        thumbColor = remember(color) { Color(ColorUtils.getDarkerColor(color.toArgb())) }
    )

    // Delay blur progress changes until onValueChangeFinished
    val blurState = rememberSliderState(value = blur, steps = 124, valueRange = 0f..125.0f)
    LaunchedEffect(onBlurChanged, blurState) {
        blurState.onValueChangeFinished = { onBlurChanged(blurState.value) }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        val interactionSource = remember { MutableInteractionSource() }

        Text(text = stringResource(R.string.title_translucent_theme_alpha), style = titleStyle)
        Slider(
            value = alpha,
            onValueChange = onAlphaChanged,
            modifier = Modifier.weight(1.0f).padding(start = 8.dp),
            colors = sliderColors,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(interactionSource, colors = sliderColors, thumbSize = ColorThumbSize)
            },
            track = { sliderState ->
                SliderDefaults.Track(sliderState, colors = sliderColors)
            },
        )
        ColorSliderResetButton(tint = sliderColors.thumbColor) {
            onAlphaChanged(1.0f)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        val interactionSource = remember { MutableInteractionSource() }

        Text(text = stringResource(R.string.title_translucent_theme_blur), style = titleStyle)
        Slider(
            state = blurState,
            modifier = Modifier.weight(1.0f).padding(start = 8.dp),
            colors = sliderColors,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(interactionSource, colors = sliderColors, thumbSize = ColorThumbSize)
            },
            track = {
                SliderDefaults.Track(it, colors = sliderColors, drawTick = { _, _ -> /* NoTick */ })
            }
        )
        ColorSliderResetButton(tint = sliderColors.thumbColor) {
            onBlurChanged(0f)
        }

        // Sync after vm initialize saved blur value
        LaunchedEffect(blur) {
            if (abs(blur - blurState.value) > 0.01f) blurState.value = blur
        }
    }
}

@Composable
private fun ColorPanel(
    list: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit,
    onColorPickerClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes = IconButtonDefaults.toggleableShapes(
        shape = MaterialTheme.shapes.small,
        pressedShape = MaterialTheme.shapes.medium,
        checkedShape = CircleShape
    )
    val colorsList = remember(list) {
        list.map {
            val contentColor = if (ColorUtils.isColorLight(it.toArgb())) Grey800 else Color.White
            IconToggleButtonColors(it, contentColor, Color.Transparent, Color.Transparent, it, contentColor)
        }
    }

    Row(modifier = modifier.fillMaxWidth()) {
        IconButton(
            onClick = onColorPickerClicked,
            shapes = IconButtonShapes(shape = shapes.shape, pressedShape = shapes.pressedShape),
            modifier = Modifier.padding(end = 4.dp),
            colors = IconButtonDefaults.filledIconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = stringResource(R.string.title_custom_color),
                modifier = Modifier.size(size = Sizes.Tiny)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(list, key = { _, item -> item.value.toString() }) { i, item ->
                IconToggleButton(
                    checked = item == selected,
                    onCheckedChange = {
                        if (it) onSelect(item)
                    },
                    shapes = shapes,
                    colors = colorsList.getOrNull(i) ?: IconButtonDefaults.iconToggleButtonVibrantColors(),
                ) {
                    if (item == selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(Sizes.Tiny))
                    }
                }
            }
        }
    }
}

@Preview("TranslucentThemeContent")
@Composable
private fun TranslucentThemeContentPreview() = TiebaLiteTheme {
    val palette = TranslucentThemeViewModel.DefaultColors.toList()
    TranslucentThemeContent(
        accent = palette[1],
        colorPalette = palette,
        isDarkTheme = false,
        alpha = 1.0f,
        blur = 0f
    )
}

@Preview("ColorPanel", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AccentColorPanelPreview() = TiebaLiteTheme {
    val colors = TranslucentThemeViewModel.DefaultColors.toList()
    ColorPanel(colors, colors[2], onSelect = {}, onColorPickerClicked = {})
}

@Preview("SelectableTextButton")
@Composable
private fun SelectableTextButtonPreview() = TiebaLiteTheme {
    SelectableTextButton(text = R.string.light_color, selected = true) { }
}
