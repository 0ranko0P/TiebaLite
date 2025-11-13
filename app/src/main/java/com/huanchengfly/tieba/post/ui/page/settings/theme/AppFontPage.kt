package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.FloatProducer
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import kotlin.math.abs

private const val FONT_SCALE_MIN = 0.8f
private const val FONT_SCALE_MAX = 1.6f // Maximum 2.0f
private const val FONT_SCALE_STEP = FONT_SCALE_MAX - FONT_SCALE_MIN

// Progress to font scale
private fun Float.toFontScale() = FONT_SCALE_MIN + (this * FONT_SCALE_STEP).fastRoundToInt() / 10f // Round to one decimal

// Font scale to progress
private fun Float.toProgress() = ((this - FONT_SCALE_MIN) * 10).fastRoundToInt() / FONT_SCALE_STEP

private fun getSizeTextHint(sliderPosition: Float): Int {
    return when (sliderPosition.fastRoundToInt()) {
        0 -> R.string.text_size_small
        in 1..2 -> R.string.text_size_little_small
        3 -> R.string.text_size_default
        in 4..6 -> R.string.text_size_little_large
        in 7..8 -> R.string.text_size_large
        else -> R.string.text_size_very_large
    }
}

@Composable
fun AppFontPage(onBack: () -> Unit, vm: AppFontViewModel = hiltViewModel()) {
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
            val fontScale by vm.fontScale.collectAsStateWithLifecycle()
            val fontScaleChanged by vm.fontScaleChanged.collectAsStateWithLifecycle()

            AppFontContent(
                modifier = Modifier.padding(paddingValues),
                fontScale = fontScale,
                onFontScaleChanged = vm::onFontScaleChanged,
                isFontScaleChanged = fontScaleChanged,
                onCancel = onBack,
                onSave = {
                    vm.onSave()
                    context.toastShort(R.string.toast_after_change_will_restart)
                    (context.applicationContext as App).removeAllActivity()
                }
            )
        }
    )
}

@Composable
private fun AppFontContent(
    modifier: Modifier = Modifier,
    fontScale: Float,
    onFontScaleChanged: (Float) -> Unit,
    isFontScaleChanged: Boolean,
    onCancel: () -> Unit = {},
    onSave: () -> Unit  = {}
) {
    if (fontScale < 0f) return // Initializing

    val chatTextStyle = MaterialTheme.typography.bodyLarge
    val fontSize = chatTextStyle.fontSize / LocalDensity.current.fontScale * fontScale

    Column(modifier.padding(18.dp)) {
        ProvideTextStyle(chatTextStyle) {
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
        }

        Spacer(modifier = Modifier.weight(1.0f))

        TextHintSlider(defaultProgress = { fontScale.toProgress() }) {
            onFontScaleChanged(it.toFontScale())
        }

        Row(modifier = Modifier.padding(top = 12.dp)) {

            NegativeButton(text = stringResource(id = R.string.button_cancel), onClick = onCancel)

            Spacer(modifier = Modifier.weight(1.0f))

            PositiveButton(
                text = stringResource(id = R.string.button_finish),
                enabled = isFontScaleChanged,
                onClick = onSave
            )
        }
    }
}

@Composable
private fun TextHintSlider(
    modifier: Modifier = Modifier,
    defaultProgress: FloatProducer,
    onProgressChanged: (Float) -> Unit
) {
    var progress by remember { mutableFloatStateOf(defaultProgress()) }
    val hintText by remember { derivedStateOf { getSizeTextHint(progress) } }

    Column(modifier = modifier) {
        Text(
            text = stringResource(id = hintText),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = progress,
            onValueChange = {
                if (abs(it - progress) > 0.01f) {
                    onProgressChanged(it)
                }
                progress = it
            },
            steps = 7,
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
        fontSize = fontSize,
        lineHeight = fontSize * 1.2f
    )
}

@Preview
@Composable
private fun AppFontContentPreview() {
    TiebaLiteTheme {
        AppFontContent(fontScale = 1.2f, onFontScaleChanged = {}, isFontScaleChanged = false)
    }
}
