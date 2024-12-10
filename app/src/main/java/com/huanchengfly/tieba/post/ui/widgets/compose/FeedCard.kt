package com.huanchengfly.tieba.post.ui.widgets.compose

import android.content.pm.ActivityInfo
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.ui.widgets.compose.video.DefaultVideoPlayerController
import com.huanchengfly.tieba.post.ui.widgets.compose.video.OnFullScreenModeChangedListener
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayerSource
import com.huanchengfly.tieba.post.ui.widgets.compose.video.rememberVideoPlayerController
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_POST_HIDE_MEDIA
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.max
import kotlin.math.min

private val ImmutableHolder<Media>.url: String
    get() = ImageUtil.getThumbnail(
        // item.srcPic, // Best  quality in [Media]
        item.bigPic,
        item.originPic  // Worst quality in [Media]
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
    val cardModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    val paddingModifier = if (action != null) Modifier.padding(top = 16.dp)
    else Modifier.padding(vertical = 16.dp)

    Column(
        modifier = cardModifier
            .then(modifier)
            .then(paddingModifier)
            .padding(contentPadding)
    ) {
        header()
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            content()
        }
        action?.invoke(this)
    }
}

@Composable
fun Badge(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(100),
    backgroundColor: Color = Color.Black.copy(0.5f),
    contentColor: Color = Color.White,
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
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
        style = MaterialTheme.typography.body1,
        highlightKeywords = highlightKeywords
    )
}

fun buildThreadContent(
    title: String,
    abstractText: String,
    tabName: String = "",
    isGood: Boolean = false
): AnnotatedString = buildAnnotatedString {
    val theme by ThemeUtil.themeState
    val showTitle = title.isNotBlank()
    val showAbstract = abstractText.isNotBlank()

    if (showTitle) {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            if (isGood) {
                withStyle(style = SpanStyle(color = theme.primary)) {
                    append(App.INSTANCE.getString(R.string.tip_good))
                }
                append(" ")
            }

            if (tabName.isNotBlank()) {
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
                style = MaterialTheme.typography.subtitle1,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.placeholder(highlight = PlaceholderHighlight.fade())
            )

            Text(
                text = "Text",
                style = MaterialTheme.typography.body1,
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
 *
 * @param forumName 吧名
 * @param extraKey 额外标识键. 确保推荐页, 搜索页中多个贴子来自同一个吧时过渡动画的唯一性
 * */
data class ForumAvatarSharedBoundsKey(val forumName: String, val extraKey: String?)

/**
 * 贴吧吧名过渡动画唯一标识键
 *
 * @param forumName 吧吧名
 * @param extraKey 额外标识键. 确保推荐页, 搜索页中多个贴子来自同一个吧时过渡动画的唯一性
 * */
data class ForumTitleSharedBoundsKey(val forumName: String, val extraKey: String?)

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
            .clip(RoundedCornerShape(4.dp))
            .background(color = ExtendedTheme.colors.chip)
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
                shape = RoundedCornerShape(4.dp)
            )
        }
        Text(
            text = stringResource(id = R.string.title_forum_name, forumName),
            modifier = Modifier
                .localSharedBounds(key = ForumTitleSharedBoundsKey(forumName, extraKey)),
            style = MaterialTheme.typography.body2,
            color = ExtendedTheme.colors.onChip,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MediaPlaceholder(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ExtendedTheme.colors.chip)
            .clickable(
                enabled = onClick != null
            ) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProvideContentColor(color = ExtendedTheme.colors.onChip) {
            Box(
                modifier = Modifier.size(16.dp),
            ) {
                icon()
            }
            ProvideTextStyle(
                value = MaterialTheme.typography.subtitle2,
                content = text
            )
        }
    }
}

@Composable
private fun ThreadMedia(
    forumId: Long,
    forumName: String,
    threadId: Long,
    modifier: Modifier = Modifier,
    medias: ImmutableList<ImmutableHolder<Media>> = persistentListOf(),
    videoInfo: ImmutableHolder<VideoInfo>? = null,
) {
    val context = LocalContext.current

    val mediaCount = remember(medias) {
        medias.size
    }
    val hasPhoto = remember(mediaCount) { mediaCount > 0 }
    val isSinglePhoto = remember(mediaCount) { mediaCount == 1 }

    val hideMedia by context.dataStore.collectPreferenceAsState(
        key = booleanPreferencesKey(KEY_POST_HIDE_MEDIA),
        defaultValue = false
    )

    val windowWidthSizeClass = LocalWindowSizeClass.current.widthSizeClass
    val singleMediaFraction = remember(windowWidthSizeClass) {
        if (windowWidthSizeClass == WindowWidthSizeClass.Compact)
            1f
        else 0.5f
    }

    val hasMedia = remember(hasPhoto, videoInfo) {
        hasPhoto || videoInfo != null
    }

    if (hasMedia) {
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
                    val aspectRatio = remember(videoInfo) {
                        max(
                            videoInfo
                                .get { thumbnailWidth }
                                .toFloat() / videoInfo.get { thumbnailHeight },
                            16f / 9
                        )
                    }
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                    ) {
                        VideoPlayer(
                            videoUrl = videoInfo.get { videoUrl },
                            thumbnailUrl = videoInfo.get { thumbnailUrl },
                            modifier = Modifier
                                .fillMaxWidth(singleMediaFraction)
                                .aspectRatio(aspectRatio)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            } else if (hasPhoto) {
                val mediaWidthFraction = remember(isSinglePhoto, singleMediaFraction) {
                    if (isSinglePhoto) singleMediaFraction else 1f
                }
                val mediaAspectRatio = remember(isSinglePhoto) {
                    if (isSinglePhoto) 2f else 3f
                }
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
                                        medias = medias.map { it.get() },
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
                    val showMediaCount = remember(medias) { min(medias.size, 3) }
                    val hasMoreMedia = remember(medias) { medias.size > 3 }
                    val showMedias = remember(medias) { medias.subList(0, showMediaCount) }
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(mediaWidthFraction)
                                .aspectRatio(mediaAspectRatio)
                                .clip(RoundedCornerShape(8.dp)),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            showMedias.fastForEachIndexed { index, media ->
                                NetworkImage(
                                    imageUri = remember(media) { media.url },
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    contentScale = ContentScale.Crop,
                                    photoViewDataProvider = {
                                        getPhotoViewData(
                                            medias = medias.map { it.get() },
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
                                text = "${medias.size}",
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
private fun ThreadMedia(
    item: ImmutableHolder<ThreadInfo>,
    modifier: Modifier = Modifier,
) {
    ThreadMedia(
        forumId = item.get { forumId },
        forumName = item.get { forumInfo?.name ?: forumName/* Might be Empty */ },
        threadId = item.get { threadId },
        medias = item.getImmutableList { media },
        videoInfo = item.get { videoInfo }?.wrapImmutable(),
        modifier = modifier,
    )
}

@Composable
fun OriginThreadCard(
    originThreadInfo: ImmutableHolder<OriginThreadInfo>,
    modifier: Modifier = Modifier,
) {
    val contentRenders = remember(originThreadInfo) { originThreadInfo.get { content.renders } }
    Column(
        modifier = modifier,
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
            medias = originThreadInfo.getImmutableList { media },
            videoInfo = originThreadInfo.get { video_info }?.wrapImmutable()
        )
    }
}

@NonRestartableComposable
@Composable
private fun ThreadForumInfo(
    item: ImmutableHolder<ThreadInfo>,
    onClick: (SimpleForum) -> Unit,
) {
    item.get { forumInfo }?.let { forumInfo ->
        ForumInfoChip(
            forumName = forumInfo.name,
            avatarUrl = forumInfo.avatar,
            transitionKey = item.get { threadId.toString() },
            onClick = {
                onClick(forumInfo)
            }
        )
    }
}

@Composable
fun ThreadReplyBtn(
    replyNum: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ActionBtn(
        icon = {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_comment_new),
                contentDescription = stringResource(id = R.string.desc_comment),
            )
        },
        text = {
            Text(
                text = if (replyNum == 0)
                    stringResource(id = R.string.title_reply)
                else replyNum.getShortNumString()
            )
        },
        modifier = modifier,
        onClick = onClick,
        color = ExtendedTheme.colors.textSecondary,
    )
}

@Composable
fun ThreadAgreeBtn(
    hasAgree: Boolean,
    agreeNum: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        if (hasAgree) ExtendedTheme.colors.primary else ExtendedTheme.colors.textSecondary
    val animatedColor by animateColorAsState(contentColor, label = "agreeBtnContentColor")

    ActionBtn(
        icon = {
            Icon(
                imageVector = if (hasAgree) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.desc_like),
            )
        },
        text = {
            Text(
                text = if (agreeNum == 0)
                    stringResource(id = R.string.title_agree)
                else agreeNum.getShortNumString()
            )
        },
        modifier = modifier,
        color = animatedColor,
        onClick = onClick
    )
}

@Composable
fun ThreadShareBtn(
    shareNum: Long,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ActionBtn(
        icon = {
            Icon(
                imageVector = Icons.Rounded.SwapCalls,
                contentDescription = stringResource(id = R.string.desc_share),
            )
        },
        text = {
            Text(
                text = if (shareNum == 0L)
                    stringResource(id = R.string.title_share)
                else shareNum.getShortNumString()
            )
        },
        modifier = modifier,
        onClick = onClick,
        color = ExtendedTheme.colors.textSecondary,
    )
}

enum class FeedType {
    Top, PlainText, SingleMedia, MultiMedia, Video
}

@Composable
fun FeedCard(
    item: ImmutableHolder<ThreadInfo>,
    onClick: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (ThreadInfo) -> Unit = {},
    onClickUser: (User) -> Unit = {},
    onClickForum: ((SimpleForum) -> Unit)? = null, // Parse Null to Hide ForumInfo
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
    dislikeAction: (@Composable RowScope.() -> Unit)? = null,
) {
    Card(
        header = {
            val author = item.item.author?: return@Card
            UserHeader(
                name = author.name,
                nameShow = author.nameShow,
                portrait = author.portrait,
                onClick = { onClickUser(author) },
                desc = remember {
                    DateTimeUtils.getRelativeTimeString(App.INSTANCE, item.get { lastTimeInt }.toString())
                },
                content = dislikeAction
            )
        },
        content = {
            ThreadContent(
                content = remember { with(item.get()) {
                    buildThreadContent(title, abstractText, tabName, isGood = this.isGood == 1)
                } }
            )

            ThreadMedia(
                item = item,
            )

            item.getNullableImmutable { origin_thread_info }
                .takeIf { item.get { is_share_thread } == 1 }?.let {
                    OriginThreadCard(
                        originThreadInfo = it,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .clickable {
                                onClickOriginThread(it.get())
                            }
                            .padding(16.dp)
                    )
                }

            if (onClickForum != null) {
                ThreadForumInfo(item = item, onClick = onClickForum)
            }
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadShareBtn(
                    shareNum = item.get { shareNum },
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                ThreadReplyBtn(
                    replyNum = item.get { replyNum },
                    onClick = { onClickReply(item.get()) },
                    modifier = Modifier.weight(1f)
                )

                ThreadAgreeBtn(
                    hasAgree = item.get { agree?.hasAgree == 1 },
                    agreeNum = item.get { agreeNum },
                    onClick = { onAgree(item.get()) },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        onClick = { onClick(item.get()) },
        modifier = modifier,
    )
}

@Composable
fun FeedCard(
    item: ImmutableHolder<PostInfoList>,
    onClick: (PostInfoList) -> Unit,
    onAgree: (PostInfoList) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (PostInfoList) -> Unit = {},
    onClickUser: (id: Long) -> Unit = {},
    onClickForum: (name: String) -> Unit = {},
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
) {
    Card(
        header = {
            UserHeader(
                name = item.get { user_name },
                nameShow = item.get { name_show },
                portrait = item.get { user_portrait },
                desc = remember {
                    DateTimeUtils.getRelativeTimeString(App.INSTANCE, item.get { create_time }.toString())
                },
                onClick = {
                    onClickUser(item.get { user_id })
                },
            )
        },
        content = {
            ThreadContent(
                content = remember { with(item.item) {
                    buildThreadContent(title = title, abstractText = abstractText)
                } }
            )

            ThreadMedia(
                forumId = item.get { forum_id },
                forumName = item.get { forum_name },
                threadId = item.get { thread_id },
                medias = item.getImmutableList { media },
                videoInfo = item.getNullableImmutable { video_info }
            )

            item.getNullableImmutable { origin_thread_info }
                .takeIf { item.get { is_share_thread } == 1 }?.let {
                    OriginThreadCard(
                        originThreadInfo = it,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .clickable {
                                onClickOriginThread(it.get())
                            }
                            .padding(16.dp)
                    )
                }

            ForumInfoChip(
                forumName = item.get { forum_name },
                onClick = { onClickForum(item.get { forum_name }) }
            )
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadShareBtn(
                    shareNum = item.get { share_num }.toLong(),
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                ThreadReplyBtn(
                    replyNum = item.get { reply_num },
                    onClick = { onClickReply(item.get()) },
                    modifier = Modifier.weight(1f)
                )

                ThreadAgreeBtn(
                    hasAgree = item.get { agree?.hasAgree == 1 },
                    agreeNum = item.get { agree_num },
                    onClick = { onAgree(item.get()) },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        onClick = { onClick(item.get()) },
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
            style = MaterialTheme.typography.caption,
            modifier = Modifier
                .placeholder(
                    visible = true,
                    highlight = PlaceholderHighlight.fade(),
                ),
        )
    }
}

@Composable
private fun ActionBtn(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = clickableModifier
            .padding(vertical = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        ProvideContentColor(color = color) {
            Box(modifier = Modifier.size(18.dp)) {
                icon()
            }
            Spacer(modifier = Modifier.width(8.dp))
            ProvideTextStyle(value = MaterialTheme.typography.caption) {
                text()
            }
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    thumbnailUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val videoPlayerController = rememberVideoPlayerController(
        source = VideoPlayerSource.Network(videoUrl),
        thumbnailUrl = thumbnailUrl,
        fullScreenModeChangedListener = object : OnFullScreenModeChangedListener {
            override fun onFullScreenModeChanged(isFullScreen: Boolean) {
                context.findActivity()?.run {
                    val insetsCtl = WindowCompat.getInsetsController(window, window.decorView)
                    if (isFullScreen) {
                        insetsCtl.hide(WindowInsetsCompat.Type.systemBars())
                        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        insetsCtl.show(WindowInsetsCompat.Type.systemBars())
                        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            }
        }
    )

    val fullScreen by (videoPlayerController as DefaultVideoPlayerController).collect { isFullScreen }
    val videoPlayerContent =
        movableContentOf { isFullScreen: Boolean, playerModifier: Modifier ->
            com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayer(
                videoPlayerController = videoPlayerController,
                modifier = playerModifier,
                backgroundColor = if (isFullScreen) Color.Black else Color.Transparent
            )
        }

    if (fullScreen) {
        Spacer(
            modifier = modifier
        )
        FullScreen {
            videoPlayerContent(
                true,
                Modifier.fillMaxSize()
            )
        }
    } else {
        videoPlayerContent(
            false,
            modifier
        )
    }
}

@Preview("FeedCardPreview")
@Composable
fun FeedCardPreview() {
    FeedCard(
        item = wrapImmutable(
            ThreadInfo(
                title = "预览",
                author = User(),
                lastTimeInt = (System.currentTimeMillis() / 1000).toInt()
            )
        ),
        onClick = {},
        onAgree = {},
        modifier = Modifier.background(ExtendedTheme.colors.card)
    )
}