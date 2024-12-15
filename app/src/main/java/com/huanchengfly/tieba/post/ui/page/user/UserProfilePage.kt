package com.huanchengfly.tieba.post.ui.page.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.BazhuSign
import com.huanchengfly.tieba.post.api.models.protos.NewGodInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.components.glide.BlurTransformation
import com.huanchengfly.tieba.post.components.imageProcessor.ImageProcessor
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.user.edit.EditProfileActivity
import com.huanchengfly.tieba.post.ui.page.user.likeforum.UserLikeForumPage
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.CollapseScrollConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberCollapseConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

private sealed class Tab(val text: String) {
    class POSTS(text: String): Tab(text)

    class THREADS(text: String): Tab(text)

    class FORUMS(text: String): Tab(text)
}

// Blurry avatar background
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AvatarBackground(
    modifier: Modifier = Modifier,
    imgProcessor: ImageProcessor,
    avatar: String,
    collapsed: Boolean
) {
    val backgroundColor = ExtendedTheme.colors.windowBackground
    val topFadeBrush = remember(ExtendedTheme.colors.theme) {
        Brush.verticalGradient(0.35f to Color.Transparent, 0.45f to backgroundColor.copy(0.2f), 0.7f to backgroundColor)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val backgroundAlpha by animateFloatAsState(
            targetValue = if (collapsed) 0.85f else 0f,
            animationSpec = tween(easing = FastOutLinearInEasing),
            label = "BackgroundAlphaAnimation"
        )

        GlideImage(
            model = avatar,
            contentDescription = null,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(brush = topFadeBrush, blendMode = BlendMode.SrcOver)
                },
            contentScale = ContentScale.FillWidth,
            alpha = backgroundAlpha
        ) {
            it.transform(BlurTransformation(imgProcessor, 90f))
        }
    }
}

@Composable
fun UserProfilePage(
    uid: Long,
    navigator: NavController,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val connection: CollapseScrollConnection = rememberCollapseConnection(coroutineScope)

    val uiState by viewModel.uiState
    val isError by remember { derivedStateOf { uiState.error != null } }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isError = isError,
        isLoading = uiState.isRefreshing,
        onReload = viewModel::refresh,
        errorScreen = { ErrorScreen(error = uiState.error.getOrNull()) }
    ) {
        val userProfile = uiState.profile?: return@StateScreen
        val account = LocalAccount.current
        val isSelf = remember(account) { account?.uid == uid.toString() }

        val tabs = remember {
            with(userProfile) {
                listOfNotNull(
                    Tab.THREADS(text = context.getString(R.string.title_profile_threads, threadNum)),

                    Tab.POSTS(text = context.getString(R.string.title_profile_posts, postNum)).takeIf { isSelf },

                    Tab.FORUMS(text = context.getString(R.string.title_profile_forums, forumNum))
                ).toImmutableList()
            }
        }

        val pagerState = rememberPagerState { tabs.size }
        val collapsed by remember { derivedStateOf { connection.ratio == 0.0f } }

        val avatarUrl = remember { StringUtil.getAvatarUrl(portrait = userProfile.portrait) }

        // Replace Scaffold's background with AvatarBackground()
        var backgroundColor = ExtendedTheme.colors.background
        viewModel.imageProcessor?.let {
            backgroundColor = Color.Transparent
            AvatarBackground(imgProcessor = it, avatar = avatarUrl, collapsed = collapsed)
        }
        // ?: power saver is on, do noting

        MyScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column {
                    UserProfileToolbar (
                        title = {
                            AnimatedVisibility(
                                visible = collapsed.not(),
                                enter =  fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                            ) {
                                ToolbarTitle(avatarUrl = avatarUrl, name = userProfile.name)
                            }
                        },
                        isSelf = isSelf,
                        isFollowing = userProfile.following,
                        onActionClicked = {
                            if (isSelf) {
                                context.goToActivity<EditProfileActivity>()
                            } else if (userProfile.following) {
                                viewModel.onUnFollowClicked(tbs = account!!.tbs)
                            } else {
                                viewModel.onFollowClicked(tbs = account!!.tbs)
                            }
                        }.takeUnless { uiState.disableButton || account == null },
                        block = uiState.block,
                        onBlackListClicked = viewModel::onBlackListClicked,
                        onWhiteListClicked = viewModel::onWhiteListClicked,
                        onBack = navigator::navigateUp
                    ) {
                        AnimatedVisibility(
                            visible = collapsed,
                            enter =  fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                        ) {
                            UserProfileDetail(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                profile = userProfile
                            )
                        }
                    } // End of UserProfileToolbar

                    UserProfileTabRow(tabs = tabs, pagerState = pagerState)
                }
            },
            backgroundColor = backgroundColor,
            contentColor = contentColorFor(MaterialTheme.colors.background)
        ) { paddingValues ->
            ProvideNavigator(navigator = navigator) {
                HorizontalPager(
                    state = pagerState,
                    key = { it.toString() },
                    modifier = Modifier.nestedScroll(connection),
                    contentPadding = paddingValues
                ) {
                    when(tabs[it]) {
                        is Tab.THREADS -> UserPostPage(uid = uid, isThread = true)
                        is Tab.POSTS -> UserPostPage(uid = uid, isThread = false)
                        is Tab.FORUMS -> UserLikeForumPage(uid = uid)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileToolbar(
    title: @Composable () -> Unit,
    block: Block? = null,
    isSelf: Boolean = false,
    isFollowing: Boolean = false,
    onActionClicked: (() -> Unit)? = null,
    onBlackListClicked: () -> Unit = {},
    onWhiteListClicked: () -> Unit = {},
    onBack: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Toolbar(
        title = title,
        navigationIcon = {
            BackNavigationIcon(onBackPressed = onBack)
        },
        actions = {
            if (onActionClicked != null) {
                TextButton(
                    onClick = onActionClicked
                ) {
                    if (isSelf) {
                        Text(text = stringResource(id = R.string.menu_edit_info))
                    } else if (isFollowing) {
                        Text(text = stringResource(id = R.string.button_unfollow))
                    } else {
                        Text(text = stringResource(id = R.string.button_follow))
                    }
                }
            }

            if (isSelf) return@Toolbar
            val isInBlackList = block != null && block.category == Block.CATEGORY_BLACK_LIST
            val isInWhiteList = block != null && block.category == Block.CATEGORY_WHITE_LIST

            ClickMenu(
                menuContent = {
                    TextMenuItem (
                        text = if (isInBlackList) R.string.title_remove_black else R.string.title_add_black,
                        onClick = onBlackListClicked
                    )
                    TextMenuItem(
                        text = if (isInWhiteList) R.string.title_remove_white else R.string.title_add_white,
                        onClick = onWhiteListClicked
                    )
                },
                triggerShape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.NoAccounts,
                    contentDescription = stringResource(id = R.string.btn_block),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        },
        backgroundColor = Color.Transparent,
        elevation = Dp.Hairline,
        content = content
    )
}

@Composable
private fun UserProfileTabRow(modifier: Modifier = Modifier, tabs: ImmutableList<Tab>, pagerState: PagerState) {
    val coroutineScope = rememberCoroutineScope()
    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions ->
            PagerTabIndicator(pagerState = pagerState, tabPositions = tabPositions)
        },
        divider = {},
        backgroundColor = Color.Transparent,
        contentColor = ExtendedTheme.colors.primary,
        edgePadding = Dp.Hairline,
        modifier = modifier,
    ) {
        tabs.fastForEachIndexed { i, tab ->
            Tab(
                selected = pagerState.currentPage == i,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(i)
                    }
                },
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                selectedContentColor = ExtendedTheme.colors.primary,
                unselectedContentColor = ExtendedTheme.colors.textSecondary,
            ) {
                Text(text = tab.text, color = LocalContentColor.current, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ToolbarTitle(modifier: Modifier = Modifier, avatarUrl: String, name: String) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(data = avatarUrl, size = Sizes.Small, contentDescription = null)
        Text(text = name, Modifier.padding(start = 8.dp), maxLines = 1)
    }
}

@Composable
private fun StatusText(modifier: Modifier = Modifier, name: String, status: String) {
    Column (
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = name, color = ExtendedTheme.colors.textSecondary)

        Text(text = status, color = ExtendedTheme.colors.text, fontWeight = FontWeight.Bold)
    }
}

/**
 * 显示吧主认证, 大神认证
 * */
@Composable
private fun VerifiedText(modifier: Modifier = Modifier, verify: String) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Verified,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = ExtendedTheme.colors.primary,
        )
        Text(
            text = verify,
            style = MaterialTheme.typography.body2,
            color = ExtendedTheme.colors.primary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserProfileDetail(modifier: Modifier = Modifier, profile: UserProfile) {
    val context = LocalContext.current
    val avatarUrl: String = remember { StringUtil.getBigAvatarUrl(profile.portrait) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Avatar(data = avatarUrl, size = 96.dp, contentDescription = null)

            Column {
                Text(
                    text = profile.name,
                    color = MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.h6,
                )
                profile.userName?.let { userName -> // 同时显示用户名与昵称
                    Text(
                        text = remember { "($userName)" },
                        color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(12.dp))
                ProvideTextStyle(MaterialTheme.typography.body2) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(IntrinsicSize.Min)
                    ) {
                        // 关注
                        StatusText(
                            name = stringResource(id = R.string.text_stat_follow),
                            status = profile.followNum,
                        )
                        // 粉丝
                        HorizontalDivider(modifier = Modifier.fillMaxHeight())
                        StatusText(
                            name = stringResource(id = R.string.text_stat_fans),
                            status = remember { profile.fans.getShortNumString() },
                        )
                        // 赞
                        HorizontalDivider(modifier = Modifier.fillMaxHeight())
                        StatusText(
                            name = stringResource(id = R.string.text_stat_agrees),
                            status = profile.agreeNum,
                        )
                        // 吧龄
                        HorizontalDivider(modifier = Modifier.fillMaxHeight())
                        StatusText(
                            name = stringResource(id = R.string.text_stat_tbage),
                            status = stringResource(id = R.string.text_profile_tb_age, profile.tbAge)
                        )
                    }
                }
            }
        }

        Text(
            text = profile.intro ?: stringResource(id = R.string.tip_no_intro),
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow (
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            profile.bazuDesc?.let {
                VerifiedText(verify = it)
            }

            profile.newGod?.let {
                VerifiedText(verify = stringResource(id = R.string.text_god_verify, it))
            }

            if (profile.isOfficial) {
                VerifiedText(verify = stringResource(id = R.string.text_official_account))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chip(text = profile.sex, invertColor = true)
            Chip(
                text = stringResource(id = R.string.text_profile_user_id, profile.tiebaUid),
                appendIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.menu_copy)
                    )
                },
                onClick = { TiebaUtil.copyText(context, profile.tiebaUid) }
            )
            profile.address?.let {
                Chip(text = stringResource(id = R.string.text_profile_ip_location, it))
            }
        }
    }
}

@Composable
private fun HorizontalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .background(ExtendedTheme.colors.textSecondary)
            .width(1.dp)
            .then(modifier)
    )
}

@Preview("UserProfileDetail", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun UserProfileDetailPreview() {
    TiebaLiteTheme {
        UserProfileDetail(
            modifier = Modifier.padding(16.dp),
            profile = UserProfileViewModel.parseUserProfile(
                User(
                    name = "tieba#0812",
                    nameShow = "我是谁",
                    tieba_uid = "114514",
                    tb_age = "10",
                    ip_address = "第二经济开发区",
                    bazhu_grade = BazhuSign("吃瓜吧吧主"),
                    new_god_data = NewGodInfo(999, field_name = "吃瓜")
                )
            )
        )
    }
}