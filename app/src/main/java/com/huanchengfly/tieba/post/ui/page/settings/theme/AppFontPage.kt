package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import androidx.datastore.preferences.core.floatPreferencesKey
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.putFloat
import com.huanchengfly.tieba.post.pxToSp
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FONT_SCALE_MIN = 0.8f
private const val FONT_SCALE_STEP = 0.05f
const val DEFAULT_FONT_SCALE = 1f

private fun getSizeTextHint(sliderPosition: Int): Int {
    return when(sliderPosition) {
        in 0..1 -> R.string.text_size_small
        in 2..3 -> R.string.text_size_little_small
        4 -> R.string.text_size_default
        in 5..6 -> R.string.text_size_little_large
        in 7..8 -> R.string.text_size_large
        else -> R.string.text_size_very_large
    }
}

@Composable
fun AppFontPage(onBack: () -> Unit) {
    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title =  stringResource(id = R.string.title_custom_font_size),
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = onBack)
                }
            )
        },
        content = { paddingValues ->
            val context = LocalContext.current
            AppFontContent(
                modifier = Modifier.padding(paddingValues),
                onCancel = onBack,
                onSave = {
                    context.toastShort(R.string.toast_after_change_will_restart)
                    (context.applicationContext as App).removeAllActivity()
                }
            )
        }
    )
}

@Composable
fun AppFontContent(modifier: Modifier = Modifier, onCancel: () -> Unit, onSave: (Float) -> Unit) {
    val context = LocalContext.current
    val fontScale by rememberPreferenceAsState(
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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

        Row(modifier = Modifier.padding(top = 12.dp)) {
            val fontSizeChanged by remember {
                derivedStateOf { abs(fontScale - newFontScale) >= 0.01f }
            }

            NegativeButton(text = stringResource(id = R.string.button_cancel), onClick = onCancel)

            Spacer(modifier = Modifier.weight(1.0f))

            PositiveButton(
                text = stringResource(id = R.string.button_finish),
                enabled = fontSizeChanged,
                onClick = {
                    context.dataStore.putFloat(AppPreferencesUtils.KEY_FONT_SCALE, newFontScale)
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
            text = stringResource(id = getSizeTextHint(progress.fastRoundToInt())),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = progress,
            onValueChange = onProgressChanged,
            steps = 8,
            valueRange = 0f..10f,
        )
    }
}

@Composable
private fun ChatBubbleText(
    modifier: Modifier = Modifier,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = contentColorFor(containerColor),
    fontSize: TextUnit
) {
    Text(
        text = text,
        modifier = modifier
            .background(color = containerColor, shape = MaterialTheme.shapes.medium)
            .padding(8.dp),
        color = contentColor,
        fontSize = fontSize
    )
}

@Preview
@Composable
private fun AppFontContentPreview() {
    TiebaLiteTheme {
        AppFontContent(onCancel = {}, onSave = {})
    }
}
