package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.floatPreferencesKey
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.pxToSp
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FONT_SCALE_MIN = 0.8f
private const val FONT_SCALE_STEP = 0.05f
const val DEFAULT_FONT_SCALE = 1f

private val SIZE_TEXT_MAPPING = mapOf(
    R.string.text_size_small to 0..1,
    R.string.text_size_little_small to 2..3,
    R.string.text_size_default to 4..4,
    R.string.text_size_little_large to 5..6,
    R.string.text_size_large to 7..8,
    R.string.text_size_very_large to 9..10
)

private fun getSizeTextHint(sliderPosition: Int): Int {
    val sizeTexts = SIZE_TEXT_MAPPING.filterValues { sliderPosition in it }
    return sizeTexts.map { it.key }[0]
}

@Destination
@Composable
fun AppFontPage(navigator: DestinationsNavigator) {
    Scaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_custom_font_size),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
        content = { paddingValues ->
            val context = LocalContext.current
            AppFontContent(
                modifier = Modifier.padding(paddingValues),
                onCancel = { navigator.navigateUp() },
                onSave = {
                    context.toastShort(R.string.toast_after_change_will_restart)
                    (context.applicationContext as App).removeAllActivity()
                }
            )
        }
    )
}

@Preview
@Composable
fun AppFontContentPreview() {
    TiebaLiteTheme {
        AppFontContent(onCancel = {}) { }
    }
}

@Composable
fun AppFontContent(modifier: Modifier = Modifier, onCancel: () -> Unit, onSave: (Float) -> Unit) {
    var fontScale by rememberPreferenceAsMutableState(
        key = floatPreferencesKey(AppPreferencesUtils.KEY_FONT_SCALE),
        defaultValue = DEFAULT_FONT_SCALE
    )
    var newFontScale by remember { mutableFloatStateOf(DEFAULT_FONT_SCALE) }
    var fontSize by remember { mutableStateOf(16.sp) }
    var progress by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(key1 = fontScale) {
        newFontScale = fontScale
        progress = ((fontScale * 1000L - FONT_SCALE_MIN * 1000L)) / ((FONT_SCALE_STEP * 1000L))
        fontSize = (15f.dpToPxFloat() * newFontScale).pxToSp().sp
    }

    Column(modifier.padding(18.dp)) {
        ChatBubbleText(
            modifier = Modifier.align(Alignment.End),
            text = stringResource(id = R.string.bubble_want_change_font_size),
            fontSize = fontSize
        )
        Spacer(modifier = Modifier.height(12.dp))
        ChatBubbleText(
            modifier = modifier.padding(end = 32.dp),
            text = stringResource(id = R.string.bubble_change_font_size),
            background = ExtendedTheme.colors.text.copy(0.1f),
            color = ExtendedTheme.colors.text,
            fontSize = fontSize
        )

        Spacer(modifier = Modifier.weight(1.0f))
        TextHintSlider(
            progress = progress,
            onProgressChanged = {
                progress = it
                newFontScale = FONT_SCALE_MIN + it.roundToInt() * FONT_SCALE_STEP
                fontSize = (15f.dpToPxFloat() * newFontScale).pxToSp().sp
            }
        )

        Row(Modifier.padding(top = 18.dp)) {
            NegativeButton(text = stringResource(id = R.string.button_cancel), onClick = onCancel)

            Spacer(modifier = Modifier.weight(1.0f))

            PositiveButton(
                text = stringResource(id = R.string.button_finish),
                enabled = abs(fontScale - newFontScale) >= 0.01f,
                onClick = {
                    fontScale = newFontScale
                    onSave(newFontScale)
                }
            )
        }
    }
}

@Composable
private fun TextHintSlider(
    modifier: Modifier = Modifier,
    progress: Float,
    onProgressChanged: (Float) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(getSizeTextHint(progress.roundToInt())),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = ExtendedTheme.colors.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(100),
            color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
        ) {
            Slider(
                value = progress,
                colors = SliderDefaults.colors(
                    inactiveTrackColor = Color.LightGray,
                ),
                steps = 6,
                valueRange = 0f..10f,
                onValueChange = onProgressChanged
            )
        }
    }
}

@Composable
private fun ChatBubbleText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = ExtendedTheme.colors.onAccent,
    background: Color = ExtendedTheme.colors.accent,
    fontSize: TextUnit
) {
    Text(
        text = text,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .padding(8.dp),
        color = color,
        fontSize = fontSize
    )
}