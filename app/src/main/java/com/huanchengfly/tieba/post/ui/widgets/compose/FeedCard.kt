package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.VideoViewActivity
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.api.models.protos.aspectRatio
import com.huanchengfly.tieba.post.api.models.protos.buildRenders
import com.huanchengfly.tieba.post.api.models.protos.getPicUrl
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoThumbnail
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.max
import kotlin.math.min

enum class FeedType {
    Top, PlainText, SingleMedia, MultiMedia, Video
}

val ThreadContentType: (index: Int, item: ThreadItem) -> FeedType by unsafeLazy {
    { _, item ->
        when {
            item.isTop -> FeedType.Top
            item.video != null -> FeedType.Video
            item.medias?.size == 1 -> FeedType.SingleMedia
            (item.medias?.size ?: 0) > 1 -> FeedType.MultiMedia
            else -> FeedType.PlainText
        }
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
    action: @Composable (ColumnScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    Column(
        modifier = modifier
            .block {
                onClick?.let { clickable(onClick = it) }
            }
            .block {
                if (action != null) padding(top = 16.dp) else padding(vertical = 16.dp)
            }
            .padding(contentPadding)
    ) {
        header()

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
            content = content
        )

        action?.invoke(this)
    }
}

@Composable
fun MediaSizeBadge(
    modifier: Modifier = Modifier,
    size: Int,
    backgroundColor: Color = Color.Black.copy(0.5f),
    contentColor: Color = Color.White,
) {
    Row(
        modifier = modifier
            .background(color = backgroundColor, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.PhotoSizeSelectActual,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Text(text = size.toString(), fontSize = 12.sp, color = contentColor)
    }
}

fun buildThreadContent(
    title: String?,
    abstractText: String,
    tabName: String? = null,
    isGood: Boolean = false
): AnnotatedString = buildAnnotatedString {
    val colorScheme = ThemeUtil.currentColorScheme()
    val showTitle = !title.isNullOrBlank()
    val showAbstract = abstractText.isNotBlank()

    if (showTitle) {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            if (isGood) {
                withStyle(style = SpanStyle(color = colorScheme.tertiary)) {
                    append(App.INSTANCE.getString(R.string.tip_good))
                }
                append(" ")
            }

            if (!tabName.isNullOrBlank()) {
                append(tabName)
                append(" | ")
            }

            append(title)
        }
    }
    if (showTitle && showAbstract) {
        append('\n')
    }
    if (showAbstract) {
        append(abstractText.emoticonString)
    }
}

@Composable
fun FeedCardPlaceholder() {
    Card(
        header = { UserHeaderPlaceholder(avatarSize = Sizes.Small) },
        content = {
            Text(
                text = "TitlePlaceholder",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.placeholder(highlight = PlaceholderHighlight.fade())
            )

            Text(
                text = "Text",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(highlight = PlaceholderHighlight.fade())
            )
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) {
                    ActionBtnPlaceholder(
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

/**
 * 贴吧头像过渡动画唯一标识键
 * */
@Immutable
@JvmInline
value class ForumAvatarSharedBoundsKey(private val value: String) {

    /**
     * @param forumName 吧名
     * @param extraKey 额外标识键. 确保推荐页, 搜索页中多个贴子来自同一个吧时过渡动画的唯一性
     * */
    constructor(forumName: String, extraKey: Any?): this(
        if (extraKey != null) forumName + extraKey else forumName
    )
}

/**
 * 贴吧吧名过渡动画唯一标识键
 * */
@Immutable
@JvmInline
value class ForumTitleSharedBoundsKey(private val value: String) {

    /**
     * @param forumName 吧吧名
     * @param extraKey 额外标识键. 确保推荐页, 搜索页中多个贴子来自同一个吧时过渡动画的唯一性
     * */
    constructor(forumName: String, extraKey: Any?): this(
        if (extraKey != null) forumName + extraKey else forumName
    )
}

@Composable
fun ForumInfoChip(
    modifier: Modifier = Modifier,
    forumName: String,
    avatarUrl: String? = null,
    transitionKey: Any? = null,
    onClick: () -> Unit
) {
    val extraKey = transitionKey?.toString()
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        avatarUrl?.let {
            Avatar(
                data = avatarUrl,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .localSharedBounds(key = ForumAvatarSharedBoundsKey(forumName, extraKey)),
                shape = MaterialTheme.shapes.extraSmall
            )
        }
        Text(
            text = stringResource(id = R.string.title_forum_name, forumName),
            modifier = Modifier
                .localSharedBounds(key = ForumTitleSharedBoundsKey(forumName, extraKey)),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MediaPlaceholder(
    icon: @Composable BoxScope.() -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    ProvideContentColorTextStyle(
        contentColor = MaterialTheme.colorScheme.onSurface,
        textStyle = MaterialTheme.typography.labelMedium
    ) {
        Row(
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .onNotNull(onClick) {
                    clickable(onClick = it)
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(16.dp), content = icon)
            text()
        }
    }
}

const val MAX_PHOTO_IN_ROW = 3

val singleVideoFraction: Float
    @Composable @ReadOnlyComposable get() = if (isWindowWidthCompact()) 1.0f else 0.5f

val singlePhotoFraction: Float
    @Composable @ReadOnlyComposable get() = if (isWindowWidthCompact()) 0.75f else 0.5f

private val Media.singlePhotoAspectRatio: Float
    @Stable get() = if (width <= 0 || height <= 0) {
        1f
    } else {
        (width / height.toFloat()).coerceAtLeast(0.6f) // Limit long pic height
    }

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ThreadMedia(
    modifier: Modifier = Modifier,
    forumId: Long,
    forumName: String,
    threadId: Long,
    medias: List<Media> = persistentListOf(),
    videoInfo: ImmutableHolder<VideoInfo>? = null,
) {
    if (medias.isEmpty() && videoInfo == null) return

    val context = LocalContext.current
    val habitSettings = LocalHabitSettings.current
    val mediaCount = medias.size
    val isSinglePhoto = mediaCount == 1

    Box(modifier = modifier) {
        if (videoInfo != null) {
            if (habitSettings.hideMedia) {
                MediaPlaceholder(
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.OndemandVideo,
                            contentDescription = stringResource(id = R.string.desc_video)
                        )
                    },
                    text = {
                        Text(text = stringResource(id = R.string.desc_video))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                VideoThumbnail(
                    modifier = Modifier
                        .fillMaxWidth(singleVideoFraction)
                        .aspectRatio(ratio = max(videoInfo.item.aspectRatio(), 16f / 9))
                        .clip(MaterialTheme.shapes.small),
                    thumbnailUrl = videoInfo.item.thumbnailUrl,
                    onClick = {
                        VideoViewActivity.launch(context, videoInfo.item)
                    }
                )
            }
        } else {
            if (habitSettings.hideMedia) {
                MediaPlaceholder(
                    icon = {
                        Icon(
                            imageVector = if (isSinglePhoto) Icons.Rounded.Photo else Icons.Rounded.PhotoLibrary,
                            contentDescription = stringResource(id = R.string.desc_photo)
                        )
                    },
                    text = {
                        Text(text = stringResource(id = R.string.btn_open_photos, mediaCount))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        context.goToActivity<PhotoViewActivity> {
                            putExtra(
                                PhotoViewActivity.EXTRA_PHOTO_VIEW_DATA,
                                getPhotoViewData(
                                    medias = medias,
                                    forumId = forumId,
                                    forumName = forumName,
                                    threadId = threadId,
                                    index = 0
                                )
                            )
                        }
                    }
                )
            } else {
                val hasMoreMedia = medias.size > MAX_PHOTO_IN_ROW
                val mediaWidthFraction = if (isSinglePhoto) singlePhotoFraction else 1f

                Box(
                    modifier = Modifier
                        .fillMaxWidth(mediaWidthFraction)
                        .aspectRatio(if (isSinglePhoto) medias[0].singlePhotoAspectRatio else 3f)
                ) {
                    Row(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(MaterialTheme.shapes.small),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (index in 0 until min(medias.size, MAX_PHOTO_IN_ROW)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                NetworkImage(
                                    imageUri = medias[index].getPicUrl(habitSettings.imageLoadType),
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop,
                                    photoViewDataProvider = {
                                        getPhotoViewData(
                                            medias = medias.toImmutableList(),
                                            forumId = forumId,
                                            forumName = forumName,
                                            threadId = threadId,
                                            index = index
                                        )
                                    },
                                )
                                if (medias[index].isLongPic == 1) {
                                    LongPicChip(modifier = Modifier.padding(6.dp))
                                }
                            }
                        }
                    }
                    if (hasMoreMedia) {
                        MediaSizeBadge(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            size = medias.size,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LongPicChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(4.dp)
    ) {
        Text(
            text = stringResource(R.string.tip_long_pic),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun OriginThreadCard(
    originThreadInfo: ImmutableHolder<OriginThreadInfo>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val imageLoadType = LocalHabitSettings.current.imageLoadType
    val contentRenders = remember(originThreadInfo.item.tid) {
        originThreadInfo.get { content.buildRenders(imageLoadType) }
    }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .onNotNull(onClick) { clickable(onClick = it) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            contentRenders.fastForEach {
                it.Render()
            }
        }
        ThreadMedia(
            forumId = originThreadInfo.get { fid },
            forumName = originThreadInfo.get { fname },
            threadId = originThreadInfo.get { tid.toLong() },
            medias = originThreadInfo.item.media,
            videoInfo = originThreadInfo.get { video_info }?.wrapImmutable()
        )
    }
}

@Composable
fun FeedCard(
    thread: ThreadItem,
    onClick: (ThreadItem) -> Unit,
    onLike: (ThreadItem) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (ThreadItem) -> Unit = onClick,
    onClickUser: (Author) -> Unit = {},
    onClickForum: ((ThreadItem) -> Unit)? = null, // Parse Null to Hide ForumInfo
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
    dislikeAction: (@Composable RowScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val (forumId, forumName, forumAvatar) = thread.simpleForum

    Card(
        header = {
            UserHeader(
                name = thread.author.name,
                avatar = thread.author.avatarUrl,
                onClick = { onClickUser(thread.author) },
                desc = remember { DateTimeUtils.getRelativeTimeString(context, thread.lastTimeMill) },
                content = dislikeAction
            )
        },
        content = {
            if (!thread.content.isNullOrEmpty()) {
                PbContentText(
                    text = thread.content,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 15.sp,
                    lineSpacing = 0.8.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            ThreadMedia(
                forumId = forumId,
                forumName = forumName,
                threadId = thread.id,
                medias = thread.medias ?: emptyList(),
                videoInfo = thread.video,
            )

            thread.originThreadInfo?.let {
                OriginThreadCard(
                    originThreadInfo = it,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onClickOriginThread(it.item) }
                        .padding(16.dp)
                )
            }

            if (onClickForum != null) {
                ForumInfoChip(
                    forumName = forumName,
                    avatarUrl = forumAvatar,
                    transitionKey = thread.id.toString(),
                    onClick = { onClickForum(thread) }
                )
            }
        },
        action = {
            ThreadActionButtonRow(
                modifier = Modifier.fillMaxWidth(),
                shares = thread.shareNum,
                replies = thread.replyNum,
                likes = thread.like.count,
                liked = thread.like.liked,
                onShareClicked = {
                    TiebaUtil.shareThread(context, thread.title, thread.id)
                },
                onReplyClicked = { onClickReply(thread) },
                onAgreeClicked = { onLike(thread) }
            )
        },
        onClick = { onClick(thread) },
        modifier = modifier,
    )
}

@Composable
private fun ActionBtnPlaceholder(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Button",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.placeholder(highlight = PlaceholderHighlight.fade())
        )
    }
}

@Preview("FeedCardPreview")
@Composable
private fun FeedCardPreview() = TiebaLiteTheme {
    Surface {
        FeedCard(
            thread = ThreadItem(
                author = Author(0, name = "FeedCardPreview", avatarUrl = ""),
                title = "预览",
                lastTimeMill = System.currentTimeMillis(),
                replyNum = 99999,
                shareNum = 20,
                simpleForum = com.huanchengfly.tieba.post.ui.models.SimpleForum(-1, "Test", "")
            ),
            onClick = {},
            onLike = {},
        )
    }
}