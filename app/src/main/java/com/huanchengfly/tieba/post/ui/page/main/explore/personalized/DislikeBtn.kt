package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.models.explore.Dislike
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.items
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState

@Composable
fun Dislike(
    dislikeResource: List<Dislike>,
    selectedReasons: Set<Dislike>,
    onDismiss: () -> Unit,
    onDislikeSelected: (Dislike) -> Unit,
    onDislikeClicked: () -> Unit
) {
    val menuState = rememberMenuState()
    ClickMenu(
        menuContent = {
            val colorScheme = MaterialTheme.colorScheme

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.title_dislike),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    Text(
                        text = stringResource(id = R.string.button_submit),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(color = colorScheme.tertiary)
                            .clickable {
                                onDislikeClicked()
                                dismiss()
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        color = colorScheme.onTertiary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
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
                    ) {
                        val selected = it in selectedReasons
                        val backgroundColor by animateColorAsState(
                            targetValue = if (selected) colorScheme.primary else colorScheme.outlineVariant
                        )

                        Text(
                            text = it.reason,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor, shape = MaterialTheme.shapes.small)
                                .clickable {
                                    onDislikeSelected(it)
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            color = colorScheme.contentColorFor(backgroundColor),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        menuState = menuState,
        onDismiss = onDismiss
    ) {
        IconButton(
            onClick = { menuState.expanded = true },
            modifier = Modifier.size(Sizes.Tiny)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.btn_more)
            )
        }
    }
}