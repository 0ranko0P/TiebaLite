package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.VideoViewActivity
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.aspectRatio
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.models.ThreadInfoItem
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoThumbnail
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.max
import kotlin.math.min

private val Media.url: String
    get() = ImageUtil.getThumbnail(
        // srcPic, // Best  quality in [Media]
        bigPic,
        originPic  // Worst quality in [Media]
    )

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
fun Badge(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black.copy(0.5f),
    contentColor: Color = Color.White,
) {
    Row(
        modifier = modifier
            .background(color = backgroundColor, shape = CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Text(text = text, fontSize = 12.sp, color = contentColor)
    }
}

@NonRestartableComposable
@Composable
fun ThreadContent(
    modifier: Modifier = Modifier,
    content: AnnotatedString,
    maxLines: Int = 5,
    highlightKeywords: ImmutableList<String> = persistentListOf(),
) {
    HighlightText(
        text = content,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        fontSize = 15.sp,
        lineSpacing = 0.8.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        style = MaterialTheme.typography.bodyLarge,
        highlightKeywords = highlightKeywords
    )
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
                withStyle(style = SpanStyle(color = colorScheme.tertiaryContainer)) {
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

@OptIn(ExperimentalSharedTransitionApi::class)
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
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
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

val singleMediaFraction: Float
    @Composable @ReadOnlyComposable get() = if (isWindowWidthCompact()) 1f else 0.5f

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ThreadMedia(
    forumId: Long,
    forumName: String,
    threadId: Long,
    modifier: Modifier = Modifier,
    medias: List<Media> = persistentListOf(),
    videoInfo: ImmutableHolder<VideoInfo>? = null,
) {
    val context = LocalContext.current

    val mediaCount = medias.size
    val hasPhoto = mediaCount > 0
    val isSinglePhoto = mediaCount == 1

    val hasMedia = hasPhoto || videoInfo != null

    if (hasMedia) {
        val hideMedia = context.appPreferences.hideMedia

        Box(modifier = modifier) {
            if (videoInfo != null) {
                if (hideMedia) {
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
                            .fillMaxWidth(singleMediaFraction)
                            .aspectRatio(ratio = max(videoInfo.item.aspectRatio(), 16f / 9))
                            .clip(MaterialTheme.shapes.small),
                        thumbnailUrl = videoInfo.item.thumbnailUrl,
                        onClick = {
                            VideoViewActivity.launch(context, videoInfo.item)
                        }
                    )
                }
            } else {
                val mediaWidthFraction = if (isSinglePhoto) singleMediaFraction else 1f
                val mediaAspectRatio = if (isSinglePhoto) 2f else 3f

                if (hideMedia) {
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
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(mediaWidthFraction)
                                .aspectRatio(mediaAspectRatio)
                                .clip(MaterialTheme.shapes.small),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (index in 0 until min(medias.size, MAX_PHOTO_IN_ROW)) {
                                NetworkImage(
                                    imageUri = medias[index].url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
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
                            }
                        }
                        if (hasMoreMedia) {
                            Badge(
                                icon = Icons.Rounded.PhotoSizeSelectActual,
                                text = medias.size.toString(),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OriginThreadCard(
    originThreadInfo: ImmutableHolder<OriginThreadInfo>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val contentRenders = remember(originThreadInfo.item.tid) {
        originThreadInfo.get { content.renders }
    }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .onNotNull(onClick) {
                clickable(onClick = it)
            }
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

enum class FeedType {
    Top, PlainText, SingleMedia, MultiMedia, Video
}

@Composable
fun FeedCard(
    item: ThreadInfoItem,
    onClick: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (ThreadInfo) -> Unit = {},
    onClickUser: (User) -> Unit = {},
    onClickForum: ((SimpleForum) -> Unit)? = null, // Parse Null to Hide ForumInfo
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
    dislikeAction: (@Composable RowScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val thread = item.info
    Card(
        header = {
            val author = thread.author?: return@Card
            UserHeader(
                name = author.name,
                nameShow = author.nameShow,
                portrait = author.portrait,
                onClick = { onClickUser(author) },
                desc = remember {
                    DateTimeUtils.getRelativeTimeString(context, thread.lastTimeInt.toString())
                },
                content = dislikeAction
            )
        },
        content = {
            ThreadContent(content = item.content)

            with(thread) {
                ThreadMedia(
                    forumId = forumId,
                    forumName = forumInfo?.name ?: forumName, // Might be Empty
                    threadId = threadId,
                    medias = media,
                    videoInfo = videoInfo?.wrapImmutable(),
                    modifier = modifier,
                )
            }

            thread.origin_thread_info
                .takeIf { thread.is_share_thread == 1 }?.let {
                    OriginThreadCard(
                        originThreadInfo = it.wrapImmutable(),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable {
                                onClickOriginThread(it)
                            }
                            .padding(16.dp)
                    )
                }

            if (onClickForum != null) {
                thread.forumInfo?.let { forumInfo ->
                    ForumInfoChip(
                        forumName = forumInfo.name,
                        avatarUrl = forumInfo.avatar,
                        transitionKey = thread.threadId.toString(),
                        onClick = { onClickForum(forumInfo) }
                    )
                }
            }
        },
        action = {
            ThreadActionButtonRow(
                modifier = Modifier.fillMaxWidth(),
                shareNum = thread.shareNum,
                replyNum = thread.replyNum,
                agreeNum = item.agreeNum,
                agreed = item.hasAgree,
                onShareClicked = {
                    TiebaUtil.shareThread(context, thread.title, thread.threadId)
                },
                onReplyClicked = { onClickReply(thread) },
                onAgreeClicked = { onAgree(thread) }
            )
        },
        onClick = { onClick(thread) },
        modifier = modifier,
    )
}

@Composable
fun FeedCard(
    item: ImmutableHolder<PostInfoList>,
    onClick: () -> Unit,
    onAgree: (PostInfoList) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (PostInfoList) -> Unit = {},
    onClickUser: (id: Long) -> Unit = {},
    onClickForum: (name: String) -> Unit = {},
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
) {
    val context = LocalContext.current
    val info = item.get()

    Card(
        header = {
            UserHeader(
                name = info.user_name,
                nameShow = info.name_show,
                portrait = info.user_portrait,
                desc = remember {
                    DateTimeUtils.getRelativeTimeString(context, info.create_time.toString())
                },
                onClick = { onClickUser(info.user_id) },
            )
        },
        content = {
            ThreadContent(
                content = remember {
                    buildThreadContent(title = info.title, abstractText = info.abstractText)
                }
            )

            ThreadMedia(
                forumId = info.forum_id,
                forumName = info.forum_name,
                threadId = info.thread_id,
                medias = item.get { media },
                videoInfo = item.getNullableImmutable { video_info }
            )

            item.getNullableImmutable { origin_thread_info }
                .takeIf { info.is_share_thread == 1 }?.let { info ->
                    OriginThreadCard(
                        originThreadInfo = info,
                        onClick = { onClickOriginThread(info.get()) }
                    )
                }

            ForumInfoChip(
                forumName = info.forum_name,
                onClick = { onClickForum(info.forum_name) }
            )
        },
        action = {
            ThreadActionButtonRow(
                modifier = Modifier.fillMaxWidth(),
                shareNum = info.share_num.toLong(),
                replyNum = info.reply_num,
                agreeNum = info.agree_num.toLong(),
                agreed = info.agree?.hasAgree == 1,
                onShareClicked = {
                    TiebaUtil.shareThread(context, title = info.title, threadId = info.thread_id)
                },
                onReplyClicked = { onClickReply(info) },
                onAgreeClicked = { onAgree(info) }
            )
        },
        onClick = onClick,
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
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .placeholder(
                    visible = true,
                    highlight = PlaceholderHighlight.fade(),
                ),
        )
    }
}

@Preview("FeedCardPreview")
@Composable
fun FeedCardPreview() = TiebaLiteTheme {
    Surface {
        FeedCard(
            item = ThreadInfoItem(
                ThreadInfo(
                    title = "预览",
                    author = User(),
                    lastTimeInt = (System.currentTimeMillis() / 1000).toInt()
                )
            ),
            onClick = {},
            onAgree = {},
        )
    }
}