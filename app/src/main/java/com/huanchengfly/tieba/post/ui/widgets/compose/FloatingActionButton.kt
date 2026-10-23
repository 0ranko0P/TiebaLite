package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.huanchengfly.tieba.post.R

@Composable
fun DefaultBackToTopFAB(
    modifier: Modifier = Modifier,
    visible: Boolean,
    size: Dp = ExtendedFabHeight,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
            spacingBetweenTooltipAndAnchor = 8.dp
        ),
        tooltip = {
            PlainTooltip { Text(text = stringResource(R.string.btn_back_to_top)) }
        },
        state = rememberTooltipState(),
        modifier = modifier,
        hasAction = true,
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.animateFloatingActionButton(visible, alignment = Alignment.Center).size(size),
        ) {
            Icon(
                imageVector = Icons.Rounded.VerticalAlignTop,
                contentDescription = stringResource(R.string.btn_back_to_top)
            )
        }
    }
}

@NonRestartableComposable
@Composable
fun DefaultToggleFloatingActionButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    ToggleFloatingActionButton(
        modifier = modifier
            .semantics {
                traversalIndex = -1f
            }
            .focusRequester(focusRequester)
            .focusable(),
        checked = checked,
        onCheckedChange = onCheckedChange,
    ) {
        Box(
            Modifier.animateIcon({ checkedProgress })
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Add),
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    rotationZ = checkedProgress * 45
                    scaleX = lerp(1f, 1.25f, checkedProgress)
                    scaleY = scaleX
                },
            )
        }
    }
}
