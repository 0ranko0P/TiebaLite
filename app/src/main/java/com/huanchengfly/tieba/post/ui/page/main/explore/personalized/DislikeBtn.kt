package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.models.explore.Dislike
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultHazeStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultInputScale
import com.huanchengfly.tieba.post.ui.widgets.compose.hazeSource
import com.huanchengfly.tieba.post.ui.widgets.compose.items
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import dev.chrisbanes.haze.hazeEffect

@Composable
fun Dislike(
    dislikeResource: List<Dislike>,
    selectedReasons: Set<Dislike>,
    onDismiss: () -> Unit,
    onDislikeSelected: (Dislike) -> Unit,
    onDislikeClicked: () -> Unit
) {
    val hazeState = LocalHazeState.current
    val menuState = rememberMenuState()

    ClickMenu(
        menuContent = {
            val toggleButtonShapes = ToggleButtonShapes(
                shape = MaterialTheme.shapes.small,
                pressedShape = MaterialTheme.shapes.medium,
                checkedShape = CircleShape
            )
            val toggleButtonColors = ToggleButtonDefaults.toggleButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.title_dislike),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )

                    SubmitButton {
                        dismiss()
                        onDislikeClicked()
                    }
                }

                VerticalGrid(
                    column = 2,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(
                        items = dislikeResource,
                        span = { if (it.id == 7) 2 else 1 }
                    ) { dislike ->
                        ToggleButton(
                            modifier = Modifier.fillMaxWidth(),
                            checked = dislike in selectedReasons,
                            onCheckedChange = { onDislikeSelected(dislike) },
                            shapes = toggleButtonShapes,
                            colors = toggleButtonColors,
                        ) {
                            Text(text = dislike.reason, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .onNotNull(hazeState) {
                val hazeInputScale = defaultInputScale()
                hazeSource(state = it, zIndex = 1f)
                    .hazeEffect(state = it, style = defaultHazeStyle()) {
                        inputScale = hazeInputScale
                    }
                    .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.74f))
             },
        menuState = menuState,
        menuShape = MaterialTheme.shapes.small,
        onDismiss = onDismiss
    ) {
        IconButton(
            onClick = { menuState.expanded = true },
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.btn_more)
            )
        }
    }
}

@Composable
private fun SubmitButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = ButtonDefaults.filledTonalButtonColors()

    Text(
        text = stringResource(id = R.string.button_submit_dislike),
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(color = colors.containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 10.dp),
        color = colors.contentColor,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall,
    )
}