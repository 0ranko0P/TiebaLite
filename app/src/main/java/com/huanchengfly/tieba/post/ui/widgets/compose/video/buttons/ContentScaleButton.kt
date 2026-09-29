package com.huanchengfly.tieba.post.ui.widgets.compose.video.buttons

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ZoomInMap
import androidx.compose.material.icons.sharp.AspectRatio
import androidx.compose.material.icons.sharp.Crop
import androidx.compose.material.icons.sharp.FitScreen
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.designsystem.icon.TbIcons
import com.huanchengfly.tieba.post.ui.widgets.compose.video.LocalPlayerGestureState

/**
 * A Material3 [IconButton] that, when clicked, displays a [ModalBottomSheet] for selecting the
 * video [ContentScale] type.
 *
 * The button's icon displays the current [ContentScale]. When the button is clicked, a
 * [ContentScaleBottomSheet] is shown.
 *
 * @param contentScale The selected [ContentScale].
 * @param onContentScaleSelected Called when new [ContentScale] is selected.
 * @param modifier The [Modifier] to be applied to the button.
 * @param presetsContentScale A list of all available [ContentScale].
 * @param colors [ButtonColors] to be used for the button.
 * @param interactionSource The [MutableInteractionSource] for the button.
 */
@Composable
@NonRestartableComposable
fun ContentScaleButton(
    contentScale: ContentScale,
    onContentScaleSelected: (ContentScale) -> Unit,
    modifier: Modifier = Modifier,
    presetsContentScale: List<ContentScale> =
        listOf(ContentScale.Fit, ContentScale.Crop, ContentScale.FillHeight, ContentScale.FillWidth, ContentScale.FillBounds),
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val gestureState = LocalPlayerGestureState.current

    IconButton(
        onClick = {
            gestureState?.showControls(autoHide = false)
            showBottomSheet = true
        },
        modifier = modifier,
        enabled = gestureState?.isEnabled ?: true,
        colors = colors,
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = contentScale.icon,
            contentDescription = stringResource(contentScale.nameRes),
        )
    }

    if (showBottomSheet) {
        val closeBottomSheet: () -> Unit = {
            gestureState?.showControls(autoHide = true)
            showBottomSheet = false
        }

        ContentScaleBottomSheet(
            contentScale = contentScale,
            onContentScaleSelected = {
                closeBottomSheet()
                onContentScaleSelected(it)
            },
            onDismissRequest = closeBottomSheet,
            presetsContentScale = presetsContentScale,
        )
    }
}

/**
 * The content displayed inside a bottom sheet for video [ContentScale] type selection.
 *
 * The default header displays the current type using `R.string.title_content_scale`. The
 * default presets are displayed as a row of [OutlinedButton] instances.
 *
 * @param contentScale The selected [ContentScale].
 * @param onContentScaleSelected Called when new [ContentScale] is selected.
 * @param onDismissRequest A lambda to be executed to dismiss the sheet.
 * @param modifier The [Modifier] to be applied to the sheet.
 * @param sheetState The state of the bottom sheet.
 * @param presetsContentScale A list of all available [ContentScale].
 */
@Composable
private fun ContentScaleBottomSheet(
    contentScale: ContentScale,
    onContentScaleSelected: (ContentScale) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    presetsContentScale: List<ContentScale>,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.title_content_scale),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                presetsContentScale.fastForEach {
                    val selected = contentScale == it
                    OutlinedButton(
                        onClick = {
                            onContentScaleSelected(it)
                        },
                        colors = if (selected) {
                            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        shape = MaterialTheme.shapes.small,
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        if (selected) {
                            Icon(imageVector = it.icon, null)
                            Spacer(modifier = Modifier.width(ButtonDefaults.ExtraSmallIconSpacing))
                        }
                        Text(
                            text = stringResource(it.nameRes),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

private val ContentScale.nameRes: Int
    @StringRes get() = when (this) {
        ContentScale.Crop -> R.string.text_scale_crop
        ContentScale.Fit -> R.string.text_scale_fit
        ContentScale.FillHeight -> R.string.text_scale_fill_height
        ContentScale.FillWidth -> R.string.text_scale_fill_width
        ContentScale.FillBounds -> R.string.text_scale_fill
        ContentScale.Inside -> R.string.text_scale_inside
        else -> throw IllegalArgumentException()
    }

private val ContentScale.icon: ImageVector
    get() = when (this) {
        ContentScale.Crop -> Icons.Sharp.Crop
        ContentScale.Fit -> Icons.Sharp.AspectRatio
        ContentScale.FillHeight -> TbIcons.FitPageHeight
        ContentScale.FillWidth -> TbIcons.FitPageWidth
        ContentScale.FillBounds -> Icons.Sharp.FitScreen
        ContentScale.Inside -> Icons.Outlined.ZoomInMap
        else -> throw IllegalArgumentException()
    }
