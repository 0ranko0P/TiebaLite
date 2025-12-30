package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.VideoViewActivity
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.localSharedElements
import com.huanchengfly.tieba.post.ui.models.search.SearchMedia
import com.huanchengfly.tieba.post.ui.models.search.SearchThreadInfo
import com.huanchengfly.tieba.post.ui.page.search.SearchIconSharedElementKey
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoThumbnail
import kotlin.math.min

@Composable
fun QuotePostCard(
    quoteContent: AnnotatedString,
    mainPostTitle: AnnotatedString,
    mainPostContent: AnnotatedString?,
    onMainPostClick: () -> Unit,
    modifier: Modifier = Modifier,
    medias: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PbContentText(
            text = quoteContent,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        MainPostCard(
            mainPostTitle = mainPostTitle,
            mainPostContent = mainPostContent,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onMainPostClick),
            medias = medias,
        )
    }
}

@Composable
fun MainPostCard(
    modifier: Modifier = Modifier,
    mainPostTitle: AnnotatedString,
    mainPostContent: AnnotatedString?,
    medias: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PbContentText(
            text = mainPostTitle,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (!mainPostContent.isNullOrBlank()) {
            PbContentText(
                text = mainPostContent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        medias()
    }
}

@Composable
fun SearchThreadItem(
    item: SearchThreadInfo,
    onClick: (SearchThreadInfo) -> Unit,
    onValidUserClick: () -> Unit,
    onForumClick: ((SearchThreadBean.ForumInfo, transitionKey: String) -> Unit)?, // Null to hide Forum
    modifier: Modifier = Modifier,
    onQuotePostClick: () -> Unit = {},
    onMainPostClick: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        header = {
            SharedTransitionUserHeader(
                user = item.author,
                extraKey = item.lazyListKey,
                desc = item.timeDesc,
                onClick = onValidUserClick.takeUnless { item.author.id == -1L }
            )
        },
        content = {
            PbContentText(
                text = item.content,
                modifier = Modifier.fillMaxWidth(),
                lineSpacing = 0.5.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3,
                style = MaterialTheme.typography.bodyMedium,
            )

            val medias: @Composable () -> Unit = {
                if (!LocalHabitSettings.current.hideMedia) {
                    if (item.pictures != null) {
                        SearchPhoto(pics = item.pictures)
                    } else if (item.video != null) {
                        SearchVideo(video = item.video)
                    }
                }
            }

            if (item.mainPostTitle != null) {
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)

                if (item.postInfoContent != null) {
                    QuotePostCard(
                        quoteContent = item.postInfoContent,
                        mainPostTitle = item.mainPostTitle,
                        mainPostContent = item.mainPostContent,
                        onMainPostClick = onMainPostClick,
                        modifier = cardModifier.clickable(onClick = onQuotePostClick),
                        medias = medias,
                    )
                } else {
                    MainPostCard(
                        mainPostTitle = item.mainPostTitle,
                        mainPostContent = item.mainPostContent,
                        modifier = cardModifier.clickable(onClick = onMainPostClick),
                        medias = medias,
                    )
                }
            } else {
                medias()
            }
            if (onForumClick != null && item.forumInfo.forumName.isNotEmpty()) {
                ForumInfoChip(
                    forumName = item.forumInfo.forumName,
                    avatarUrl = item.forumInfo.avatar,
                    transitionKey = item.pid
                ) {
                    onForumClick(item.forumInfo, item.pid.toString())
                }
            }
        },
        onClick = { onClick(item) },
    )
}

@Composable
private fun SearchVideo(modifier: Modifier = Modifier, video: SearchMedia.Video) {
    val context = LocalContext.current
    VideoThumbnail(
        modifier = modifier
            .fillMaxWidth(singleVideoFraction)
            .aspectRatio(ratio = 2.0f)
            .clip(MaterialTheme.shapes.small),
        thumbnailUrl = video.thumbnail,
        onClick = {
            VideoViewActivity.launch(context, videoUrl = video.url, thumbnailUrl = video.thumbnail)
        }
    )
}

@Composable
private fun SearchPhoto(modifier: Modifier = Modifier, pics: List<SearchMedia.Picture>) {
    if (pics.isEmpty()) {
        return
    }

    val picCount = pics.size
    val isSinglePhoto = picCount == 1
    val mediaWidthFraction = if (isSinglePhoto) singlePhotoFraction else 1f
    val mediaAspectRatio = if (isSinglePhoto) 2f else 3f
    val hasMoreMedia = picCount > MAX_PHOTO_IN_ROW

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth(mediaWidthFraction)
                .aspectRatio(mediaAspectRatio)
                .clip(MaterialTheme.shapes.small),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until min(picCount, MAX_PHOTO_IN_ROW)) {
                NetworkImage(
                    imageUri = pics[i].url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        if (hasMoreMedia) {
            MediaSizeBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                size = picCount,
            )
        }
    }
}

@Composable
fun SearchBox(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onKeywordSubmit: (String) -> Unit = {},
    placeholder: @Composable () -> Unit = {},
    prependIcon: @Composable RowScope.() -> Unit = {},
    appendIcon: (@Composable RowScope.() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    var launchFocused by rememberSaveable { mutableStateOf(false) }
    val isKeywordNotEmpty = remember(keyword) { keyword.isNotEmpty() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            prependIcon.invoke(this)
            BaseTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = placeholder,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        if (isKeywordNotEmpty) onKeywordSubmit(keyword)
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusEvent {
                        isFocused = it.hasFocus
                        if (!isFocused) keyboardController?.hide()
                    },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
            )

            appendIcon?.invoke(this)

            AnimatedVisibility(visible = isKeywordNotEmpty && isFocused) {
                Icon(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onKeywordChange("") },
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = stringResource(id = R.string.button_clear)
                )
            }

            AnimatedVisibility(visible = isKeywordNotEmpty) {
                Icon(
                    modifier = Modifier
                        .localSharedElements(SearchIconSharedElementKey)
                        .clip(CircleShape)
                        .clickable { onKeywordSubmit(keyword) },
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(id = R.string.button_search)
                )
            }
        }
    }

    if (!launchFocused) { // Focus & Show keyboard at fist launch
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            launchFocused = true
        }
    }

    val isKeyboardOpen by with(LocalDensity.current) {
         rememberUpdatedState(WindowInsets.ime.getBottom(this) > 0)
    }

    BackHandler(enabled = isKeyboardOpen) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
}