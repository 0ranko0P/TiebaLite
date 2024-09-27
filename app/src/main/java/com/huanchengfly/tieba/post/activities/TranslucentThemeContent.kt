package com.huanchengfly.tieba.post.activities

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestListener
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.PaletteBackground
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.settings.AboutPage
import com.huanchengfly.tieba.post.ui.widgets.compose.OneTimeMeasurer
import com.huanchengfly.tieba.post.ui.widgets.compose.RoundedSlider
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.ColorPickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.DisplayUtil
import com.huanchengfly.tieba.post.utils.DisplayUtil.toDpSize

@Composable
fun TranslucentThemeContent(
    modifier: Modifier = Modifier,
    viewModel: TranslucentThemeViewModel,
    onSelectWallpaper: () ->Unit
) {
    val colorPickerState = rememberDialogState()

    val style =
        MaterialTheme.typography.subtitle2.copy(color = LocalContentColor.current, fontSize = 14.sp)

    ProvideTextStyle(style) {
        Content(
            modifier = modifier,
            accent = viewModel.primaryColor,
            onColorPicked = viewModel::onColorPicked,
            colorPalette = viewModel.colorPalette,
            isDarkTheme = viewModel.isDarkTheme,
            onColorModeChanged = viewModel::onColorModeChanged,
            alpha = viewModel.alpha,
            onAlphaChanged = viewModel::onAlphaChanged,
            blur = viewModel.blurRadius,
            onBlurChanged = viewModel::onBlurChanged,
            onValueChangeFinished = viewModel::updateImage,
            onSelectWallpaper = onSelectWallpaper,
            onLaunchColorPicker = colorPickerState::show
        )
    }

    if (colorPickerState.show) {
        ColorPickerDialog(
            state = colorPickerState,
            title = R.string.title_color_picker_primary,
            initial = viewModel.primaryColor,
            onColorChanged = viewModel::onColorPicked
        )
    }
}

@Composable
private fun Content(
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
    onValueChangeFinished: () -> Unit = {},
    onSelectWallpaper: () -> Unit = {},
    onLaunchColorPicker: () -> Unit = {}
) {
    val maxWidth = Modifier.fillMaxWidth()
    Column(modifier = modifier) {
        val accentColorAnim by animateColorAsState(
            targetValue = accent,
            animationSpec = TweenSpec(durationMillis = DefaultDurationMillis * 2),
            label = "AccentColorAnimation"
        )
        val buttonColor = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary,
            disabledContentColor = Color.White,
            disabledBackgroundColor = accentColorAnim
        )

        // Wallpaper picker button
        Box(
            modifier = maxWidth
                .height(40.dp)
                .background(MaterialTheme.colors.secondary, RoundedCornerShape(10.dp))
                .clickable(onClick = onSelectWallpaper)
        ) {
            Text(stringResource(R.string.title_select_pic), Modifier.align(Alignment.Center))
        }
        Spacer(maxWidth.height(12.dp))

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

            Spacer(Modifier.width(12.dp))

            SelectableTextButton(
                modifier = Modifier.weight(0.5f),
                text = R.string.light_color,
                colors = buttonColor,
                selected = !isDarkTheme,
                onClick = onColorModeChanged
            )
        }

        AnimatedVisibility(colorPalette.isNotEmpty()) {
            Column {
                Text(stringResource(R.string.title_select_color), maxWidth.padding(vertical = 12.dp))
                ColorPanel(colorPalette, accent, onColorPicked, onLaunchColorPicker)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.title_translucent_theme_alpha)
            )
            RoundedSlider(
                value = alpha,
                onValueChange = onAlphaChanged,
                modifier = Modifier.padding(start = 10.dp),
                onValueChangeFinished = onValueChangeFinished,
                valueRange = 0.1f..1.0f,
                color = accentColorAnim,
                trackColor = MaterialTheme.colors.secondary,
                trackHeight = 12.dp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.title_translucent_theme_blur)
            )
            RoundedSlider(
                value = blur,
                onValueChange = onBlurChanged,
                modifier = Modifier.padding(start = 10.dp),
                onValueChangeFinished = onValueChangeFinished,
                valueRange = 0f..125.0f,
                steps = 124,
                color = accentColorAnim,
                trackColor = MaterialTheme.colors.secondary,
                trackHeight = 12.dp
            )
        }


        Row(
            modifier = maxWidth
                .background(MaterialTheme.colors.secondary, RoundedCornerShape(10.dp))
                .padding(12.dp),
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
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Display picked background wallpaper side by side, but later one having an [AboutPage] as overlay
 *
 * @see TranslucentThemeViewModel.wallpaperTransformation
 * @see WallpaperOverlay
 * */
@Composable
fun SideBySideWallpaper(
    modifier: Modifier = Modifier,
    vm: TranslucentThemeViewModel,
    placeHolder: () -> Drawable?,
    listener: RequestListener<Drawable>?
) {
    val cornerShape = MaterialTheme.shapes.medium
    val density = LocalDensity.current
    val context = LocalContext.current
    val screen: DpSize = remember { DisplayUtil.getScreenPixels(context).toDpSize(density) }

    Row(modifier = modifier) {
        val wallpaper = vm.wallpaper
        val trans = vm.wallpaperTransformation

        // Constraint size by screen aspect ratio
        val wallpaperModifier = Modifier
            .weight(0.5f)
            .aspectRatio(ratio = screen.width / screen.height)
            .background(Color.Black, shape = cornerShape)
            .shadow(6.dp, shape = cornerShape)

        Wallpaper(wallpaperModifier, wallpaper, vm.alpha, trans, placeHolder, listener = listener)

        Spacer(Modifier.width(16.dp))

        OneTimeMeasurer(modifier = wallpaperModifier) { size ->
            Wallpaper(Modifier.fillMaxSize(), wallpaper, vm.alpha, trans, placeHolder, listener = null)

            val sizeInDp = size?.toDpSize(density) ?: return@OneTimeMeasurer
            WallpaperOverlay(screen = screen, targetSize = sizeInDp, isDarkColor = vm.isDarkTheme)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
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
private fun WallpaperOverlay(screen: DpSize, targetSize: DpSize, isDarkColor: Boolean) {
    // Simulate color mode changes
    val colors = MaterialTheme.colors.copy(
        surface = Color.Transparent,
        onSurface = if (isDarkColor) Color.Black else Color.White
    )
    // Scale font size up
    val typography = MaterialTheme.typography.run {  this.copy(
        button = button.copy(fontSize = button.fontSize * 1.25f),
        caption = caption.copy(fontSize = caption.fontSize * 1.25f)
    ) }

    MaterialTheme(colors = colors, typography = typography) {
        val scale = maxOf(targetSize.width / screen.width, targetSize.height / screen.height)
        AboutPage(
            modifier = Modifier
                .size(targetSize)
                .requiredSize(screen)
                .scale(scale)
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
    val sizeAnim by animateFloatAsState(
        targetValue = if (selected) 1.025f else 1f,
        animationSpec = TweenSpec(easing = LinearOutSlowInEasing),
        label = "SelectableTextButtonAnimation"
    )

    TextButton(
        onClick = onClick,
        modifier = modifier.scale(sizeAnim),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorPanel(list: List<Color>, selected: Color, onSelect: (Color) -> Unit, onColorPickerClicked: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        stickyHeader(Icons.Rounded.Palette.name) {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = stringResource(R.string.title_custom_color),
                modifier = Modifier
                    .padding(end = 6.dp, bottom = 4.dp)
                    .size(48.dp)
                    .background(Color.LightGray, RoundedCornerShape(10.dp))
                    .clickable(onClick = onColorPickerClicked),
                tint = Color.White
            )
        }
        items(list, key = { item: Color -> item.value.toString() }) {
            ColorBox(
                modifier = Modifier.clickable { onSelect(it) },
                color = it,
                selected = it.value == selected.value
            )
        }
    }
}

@Composable
private fun ColorBox(modifier: Modifier = Modifier, color: Color, selected: Boolean = false) {
    Box(modifier = modifier
        .padding(end = 6.dp, bottom = 4.dp)
        .size(48.dp)
        .background(color, RoundedCornerShape(10.dp))
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

@Preview("Content")
@Composable
private fun ContentPreview() = TiebaLiteTheme {
    val palette = TranslucentThemeViewModel.DefaultColors.toList()
    Content(accent = palette[1], colorPalette = palette, isDarkTheme = false, alpha = 1.0f, blur = 0f)
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
