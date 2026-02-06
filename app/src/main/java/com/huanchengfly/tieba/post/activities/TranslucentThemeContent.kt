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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.colorscheme.translucentColorScheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.PaletteBackground
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.settings.AboutPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Measurer
import com.huanchengfly.tieba.post.ui.widgets.compose.RoundedSlider
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.ColorPickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.DisplayUtil
import com.huanchengfly.tieba.post.utils.DisplayUtil.toDpSize
import kotlin.math.abs

@Composable
fun TranslucentThemeContent(
    modifier: Modifier = Modifier,
    viewModel: TranslucentThemeViewModel,
    state: UiState,
    onSelectWallpaper: () ->Unit
) {
    val colorPickerState = rememberDialogState()

    TranslucentThemeContent(
        modifier = modifier.fillMaxHeight(),
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
                Spacer(modifier = Modifier.height(4.dp))
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

        Spacer(modifier = Modifier.weight(1.0f))
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
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        // Constraint size by screen aspect ratio
        val wallpaperModifier = Modifier
            .fillMaxHeight()
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
private fun ImageFilterPanel(
    color: Color,
    alpha: Float,
    onAlphaChanged: (Float) -> Unit = {},
    blur: Float,
    onBlurChanged: (Float) -> Unit = {},
) {
    val titleStyle = MaterialTheme.typography.titleSmall
    val accentColorAnim by animateColorAsState(color, spring(stiffness = Spring.StiffnessLow))
    val sliderColors = SliderDefaults.colors()
    val sliderColorsAnimated = sliderColors.copy(
        activeTrackColor = accentColorAnim,
        inactiveTrackColor = accentColorAnim.copy(0.3f),
        thumbColor = Color.White
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = stringResource(R.string.title_translucent_theme_alpha), style = titleStyle)
        RoundedSlider(
            value = alpha,
            onValueChange = onAlphaChanged,
            modifier = Modifier.padding(start = 10.dp),
            valueRange = 0.1f..1.0f,
            colors = sliderColorsAnimated
        )
    }

    // Delay blur progress changes until onValueChangeFinished
    var blurProgress by remember { mutableFloatStateOf(blur) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = stringResource(R.string.title_translucent_theme_blur), style = titleStyle)
        RoundedSlider(
            value = blurProgress,
            onValueChange = { blurProgress = it },
            modifier = Modifier.padding(start = 10.dp),
            onValueChangeFinished = {
                onBlurChanged(blurProgress)
            },
            valueRange = 0f..125.0f,
            steps = 124,
            colors = sliderColorsAnimated
        )

        // Sync after vm initialize saved blur value
        LaunchedEffect(blur) {
            if (abs(blur - blurProgress) > 0.01f) blurProgress = blur
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
    Row(modifier = modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Rounded.Palette,
            contentDescription = stringResource(R.string.title_custom_color),
            modifier = Modifier
                .padding(end = 10.dp)
                .size(size = Sizes.Medium)
                .background(Color.LightGray, MaterialTheme.shapes.medium)
                .clickable(onClick = onColorPickerClicked),
            tint = Color.White
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(list, key = { item: Color -> item.value.toString() }) {
                ColorBox(
                    modifier = Modifier.clickableNoIndication { onSelect(it) },
                    color = it,
                    selected = it.value == selected.value
                )
            }
        }
    }
}

@Composable
private fun ColorBox(modifier: Modifier = Modifier, color: Color, selected: Boolean = false) {
    Box(
        modifier = modifier
            .size(size = Sizes.Medium)
            .background(color, shape = MaterialTheme.shapes.medium)
    ) {
        if (!selected) return
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(12.dp)
                .offset(x = 4.dp, y = 4.dp)
                .background(color = color, shape = CircleShape)
                .border(BorderStroke(1.dp, Color.White), shape = CircleShape),
            tint = Color.White
        )
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
