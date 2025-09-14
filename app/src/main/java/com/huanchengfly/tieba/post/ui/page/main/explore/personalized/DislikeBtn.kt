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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.personalized.DislikeReason
import com.huanchengfly.tieba.post.api.models.protos.personalized.ThreadPersonalized
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.items
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun Dislike(
    personalized: ImmutableHolder<ThreadPersonalized>,
    onDislike: (clickTime: Long, reasons: ImmutableList<ImmutableHolder<DislikeReason>>) -> Unit,
) {
    var clickTime by remember { mutableLongStateOf(0L) }
    val selectedReasons = remember { mutableStateSetOf<ImmutableHolder<DislikeReason>>() }
    val menuState = rememberMenuState()
    val dislikeResource = personalized.getImmutableList { dislikeResource }
    ClickMenu(
        menuContent = {
            val colorScheme = MaterialTheme.colorScheme

            DisposableEffect(personalized) {
                clickTime = System.currentTimeMillis()
                onDispose {
                    selectedReasons.clear()
                }
            }

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
                        text = stringResource(id = R.string.button_submit_dislike),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(color = colorScheme.tertiary)
                            .clickable {
                                dismiss()
                                onDislike(clickTime, selectedReasons.toImmutableList())
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
                        span = { if (it.get { dislikeId } == 7) 2 else 1 }
                    ) {
                        val selected by remember { derivedStateOf { selectedReasons.contains(it) } }
                        val backgroundColor by animateColorAsState(
                            targetValue = if (selected) colorScheme.primary else colorScheme.outlineVariant
                        )

                        Text(
                            text = it.get { dislikeReason },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor, shape = MaterialTheme.shapes.small)
                                .clickable {
                                    if (selectedReasons.contains(it)) {
                                        selectedReasons.remove(it)
                                    } else {
                                        selectedReasons.add(it)
                                    }
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
    ) {
        IconButton(
            onClick = { menuState.expanded = true },
            modifier = Modifier.size(Sizes.Tiny)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.button_submit_dislike)
            )
        }
    }
}