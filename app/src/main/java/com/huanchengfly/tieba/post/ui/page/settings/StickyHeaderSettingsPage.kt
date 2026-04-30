package com.huanchengfly.tieba.post.ui.page.settings

import androidx.annotation.StringRes
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.page.settings.theme.UserPostCardWidget
import com.huanchengfly.tieba.post.ui.page.subposts.PostLikeButton
import com.huanchengfly.tieba.post.ui.page.thread.ThreadHeader
import com.huanchengfly.tieba.post.ui.page.thread.Type
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.fade
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun StickyHeaderSettingsPage(habitSettings: Settings<HabitSettings>, onBack: () -> Unit = {}) {
    val listState = rememberLazyListState()
    val windowSize = LocalWindowInfo.current.containerDpSize
    val isWindowHeightCompact = windowSize.height <= WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND.dp
    val isWindowWidthCompact = windowSize.width <= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp

    val selected = LocalHabitSettings.current.stickyHeader
    val onSelectChanged: (Boolean) -> Unit = { newValue ->
        if (newValue != selected) {
            habitSettings.save { it.copy(stickyHeader = newValue) }
        }
    }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = R.string.title_settings_sticky_header,
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) }
            )
        },
        backgroundColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .padding(
                        horizontal = if (isWindowWidthCompact) Dp.Hairline else 32.dp,
                        vertical = if (isWindowHeightCompact) 16.dp else 32.dp
                    )
                    .fillMaxWidth(fraction = if (isWindowHeightCompact) 0.9f else 0.7f)
            ) {
                val containerColor = MaterialTheme.colorScheme.let {
                    if (it.isTranslucent) it.surfaceContainerHigh else it.surfaceContainer
                }
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        background = containerColor,
                        surface = containerColor
                    )
                ) {
                    ThreadColumnWithStickyHeader(
                        modifier = Modifier
                            .requiredSize(windowSize)
                            .scale(maxWidth / windowSize.width),
                        listState = listState,
                        stickyHeader = selected
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(top = 64.dp, bottom = 8.dp),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    text = stringResource(R.string.tip_sticky_header),
                    style = MaterialTheme.typography.bodyLarge
                )

                RadioPreference(
                    title = R.string.settings_sticky_header_on,
                    summary = R.string.summary_sticky_header_on,
                    selected = selected,
                    onClick = { onSelectChanged(true) }
                )

                RadioPreference(
                    title = R.string.settings_sticky_header_off,
                    summary = R.string.summary_sticky_header_off,
                    selected = !selected,
                    onClick = { onSelectChanged(false) }
                )
            }
        }

        LaunchedEffect(Unit) {
            val scrollAnimationSpec: AnimationSpec<Float> = tween(durationMillis = 1000)
            while (true) {
                launch {
                    withTimeout(1300L) {
                        if (listState.canScrollBackward) {
                            listState.animateScrollBy(-9000f, scrollAnimationSpec)
                        } else {
                            listState.animateScrollBy(1500f, scrollAnimationSpec)
                        }
                    }
                }
                delay(4000)
            }
        }
    }
}

@Composable
private fun RadioPreference(
    selected: Boolean,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    @StringRes summary: Int? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    SegmentedPreference(
        modifier = modifier,
        title = {
            Text(text = stringResource(id = title))
        },
        summary = summary?.let {
            { Text(text = stringResource(id = it)) }
        },
        leadingIcon = {
            RadioButton(
                selected = selected,
                onClick = null,
                interactionSource = interactionSource,
            )
        },
        colors = ListItemDefaults.colors(),
        interactionSource = interactionSource,
        onClick = onClick,
    )
}

@Composable
private fun ThreadColumnWithStickyHeader(
    modifier: Modifier,
    listState: LazyListState = rememberLazyListState(),
    stickyHeader: Boolean
) {
    val headerMovableContent = remember {
        movableContentWithReceiverOf<LazyItemScope> {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                ThreadHeader(replyNum = 999, isSeeLz = false)
            }
        }
    }

    val placeholderHighlight = PlaceholderHighlight.fade(
        animationSpec = infiniteRepeatable(
            animation = tween(delayMillis = 200, durationMillis = 1000),
            repeatMode = RepeatMode.Reverse,
        )
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        shadowElevation = 1.dp,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            item(contentType = Type.FirstPost) {
                UserPostCardWidget(account = null, postText = R.string.tip_empty)
            }

            if (stickyHeader) {
                stickyHeader(contentType = Type.Header, content = { headerMovableContent() })
            } else {
                item(contentType = Type.Header, content = headerMovableContent)
            }

            items(24, contentType = { Type.Post }) {
                ThreadPlaceHolder(highlight = placeholderHighlight)
            }
        }
    }
}

@Composable
private fun ThreadPlaceHolder(
    modifier: Modifier = Modifier,
    highlight: PlaceholderHighlight? = null,
) {
    val userCardShape = MaterialTheme.shapes.extraSmall
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AvatarPlaceholder(size = Sizes.Small)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.25f)
                        .height(16.dp)
                        .placeholder(shape = userCardShape, highlight = highlight)
                )

                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .placeholder(shape = userCardShape, highlight = highlight)
                )
            }

            PostLikeButton(LikeZero, onClick = { })
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, top = 8.dp)
                .height(36.dp)
                .placeholder(highlight = highlight)
        )
    }
}
