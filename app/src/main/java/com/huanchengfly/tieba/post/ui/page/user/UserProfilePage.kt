package com.huanchengfly.tieba.post.ui.page.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.huanchengfly.tieba.post.theme.FloatProducer
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isLooseWindowWidth
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.user.edit.EditProfileActivity
import com.huanchengfly.tieba.post.ui.page.user.likeforum.UserLikeForumPage
import com.huanchengfly.tieba.post.ui.page.user.likeforum.UserLikeForumPageEmpty
import com.huanchengfly.tieba.post.ui.page.user.likeforum.UserLikeForumPageHide
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TwoRowsTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlin.math.abs

private sealed class Tab(val text: String) {
    class POSTS(text: String): Tab(text)

    class THREADS(text: String): Tab(text)

    class FORUMS(text: String): Tab(text)
}

private val UserProfileExpandHeight = 206.dp

// Blurry avatar background
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AvatarBackground(
    imgProcessor: ImageProcessor,
    avatar: String,
    collapseFraction: FloatProducer
) {
    val color = MaterialTheme.colorScheme.background
    GlideImage(
        model = avatar,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    // draw surface mask with alpha(0.8..0.98)
                    // this looks way better than change alpha directly and no banding artifact
                    drawRect(
                        color = color.copy(alpha = collapseFraction() * 0.18f + 0.8f)
                    )
                }
            },
        contentScale = ContentScale.Crop,
    ) {
        it.transform(BlurTransformation(imgProcessor, 120f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@NonRestartableComposable
@Composable
private fun rememberAvatarTopBarColors(): TopAppBarColors {
    val colorScheme = MaterialTheme.colorScheme
    val defaultColors = TopAppBarDefaults.topAppBarColors()
    return remember(colorScheme) {
        defaultColors.copy(containerColor = colorScheme.surfaceContainer.copy(0.01f)) // Nearly transparent
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfilePage(
    uid: Long,
    navigator: NavController,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isError by remember { derivedStateOf { uiState.error != null } }
    val isRefreshing by remember { derivedStateOf { uiState.isRefreshing } }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isError = isError,
        isLoading = isRefreshing,
        onReload = viewModel::refresh,
        errorScreen = { ErrorScreen(error = uiState.error.getOrNull()) }
    ) {
        val userProfile = uiState.profile?: return@StateScreen
        val account = LocalAccount.current
        val isSelf = remember(account) { account?.uid == uid.toString() }

        val tabs = remember {
            with(userProfile) {
                listOfNotNull(
                    Tab.THREADS(
                        text = context.getString(R.string.title_profile_threads, threadNum.getShortNumString())
                    ),

                    Tab.POSTS(
                        text = context.getString(R.string.title_profile_posts, postNum.getShortNumString())
                    ).takeIf { isSelf },

                    Tab.FORUMS(
                        text = context.getString(R.string.title_profile_forums, forumNum.getShortNumString())
                    )
                ).toImmutableList()
            }
        }

        val pagerState = rememberPagerState { tabs.size }
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val collapseFraction = FloatProducer { scrollBehavior.state.collapsedFraction }

        val avatarUrl = remember { StringUtil.getAvatarUrl(portrait = userProfile.portrait) }

        // Replace Scaffold's background with AvatarBackground()
        var backgroundColor = MaterialTheme.colorScheme.surface
        viewModel.imageProcessor?.let {
            backgroundColor = Color.Transparent
            AvatarBackground(imgProcessor = it, avatar = avatarUrl, collapseFraction)
        }
        // ?: power saver is on, do noting

        val pagerContent = remember(pagerState) {
            movableContentOf<Pair<Boolean, NestedScrollConnection?>> { (fluid, scrollConnection) ->
                HorizontalPager(
                    state = pagerState,
                    key = { it },
                    modifier = Modifier
                        .fillMaxSize()
                        .onNotNull(scrollConnection) { nestedScroll(connection = it) },
                ) {
                    when(tabs[it]) {
                        is Tab.THREADS -> UserPostPage(uid = uid, isThread = true, fluid = fluid)

                        is Tab.POSTS -> UserPostPage(uid = uid, isThread = false, fluid = fluid)

                        is Tab.FORUMS -> {
                            when {
                                userProfile.privateForum -> UserLikeForumPageHide()

                                userProfile.forumNum == 0 -> UserLikeForumPageEmpty()

                                else -> UserLikeForumPage(uid = uid, fluid = fluid)
                            }
                        }
                    }
                }
            }
        }
        val userProfileDetail: @Composable () -> Unit = {
            UserProfileDetail(profile = userProfile)
        }

        val isLooseWindowWidth = isLooseWindowWidth()

        MyScaffold(
            topBar = {
                UserProfileToolbar(
                    title = userProfileDetail.takeUnless { isLooseWindowWidth },
                    collapsedTitle = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Avatar(data = avatarUrl, size = Sizes.Small)
                            Text(text = userProfile.name, maxLines = 1)
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
                    onBack = navigator::navigateUp,
                    scrollBehavior = scrollBehavior
                ) {
                    if (isLooseWindowWidth) return@UserProfileToolbar
                    UserProfileTabRow(tabs, pagerState, collapseFraction)
                }
            },
            backgroundColor = backgroundColor
        ) { paddingValues ->
            ProvideNavigator(navigator = navigator) {
                if (isLooseWindowWidth) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(paddingValues),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.35f)
                                .padding(start = 16.dp, bottom = 16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            userProfileDetail()
                        }

                        Column {
                            UserProfileTabRow(tabs, pagerState, collapseFraction)
                            pagerContent(true to null) // fluid and set ScrollBehavior to null
                        }
                    }
                } else {
                    pagerContent(false to scrollBehavior.nestedScrollConnection)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileToolbar(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)?,
    collapsedTitle: @Composable () -> Unit,
    block: Block? = null,
    isSelf: Boolean = false,
    isFollowing: Boolean = false,
    onActionClicked: (() -> Unit)? = null,
    onBlackListClicked: () -> Unit = {},
    onWhiteListClicked: () -> Unit = {},
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val actionsMenu: @Composable RowScope.() -> Unit = {
        if (onActionClicked != null) {
            TextButton(onClick = onActionClicked) {
                Text(
                    text = when {
                        isSelf -> stringResource(id = R.string.menu_edit_info)
                        isFollowing -> stringResource(id = R.string.button_unfollow)
                        else -> stringResource(id = R.string.button_follow)
                    }
                )
            }
        }

        if (!isSelf) {
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
                modifier = Modifier.padding(end = 12.dp),
                triggerShape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.NoAccounts,
                    contentDescription = stringResource(id = R.string.btn_block),
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }
    }

    if (title != null) {
        TwoRowsTopAppBar(
            modifier = modifier,
            title = title,
            smallTitle = collapsedTitle,
            navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
            actions = actionsMenu,
            expandedHeight = UserProfileExpandHeight,
            colors = rememberAvatarTopBarColors(),
            scrollBehavior = scrollBehavior,
            content = content
        )
    } else {
        TopAppBar(
            modifier = modifier,
            title = {},
            navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
            actions = actionsMenu,
            colors = rememberAvatarTopBarColors(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileTabRow(
    tabs: ImmutableList<Tab>,
    pagerState: PagerState,
    collapseFraction: FloatProducer,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = {
            FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
        },
        divider = { // visible when collapsed
            HorizontalDivider(modifier = Modifier.graphicsLayer { alpha = collapseFraction() })
        },
        containerColor = Color.Transparent,
        contentColor = colorScheme.primary,
        modifier = modifier,
    ) {
        tabs.fastForEachIndexed { i, tab ->
            Tab(
                selected = pagerState.currentPage == i,
                onClick = {
                    coroutineScope.launch {
                        if (abs(pagerState.currentPage - i) > 1) {
                            pagerState.scrollToPage(i)
                        } else {
                            pagerState.animateScrollToPage(i)
                        }
                    }
                },
                text = {
                    Text(text = tab.text)
                },
                unselectedContentColor = colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatusText(modifier: Modifier = Modifier, name: String, status: String) {
    Column (
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = name, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(text = status, color = LocalContentColor.current, fontWeight = FontWeight.Bold)
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
        val tintColor = MaterialTheme.colorScheme.primary
        Icon(
            imageVector = Icons.Rounded.Verified,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tintColor,
        )
        Text(
            text = verify,
            style = MaterialTheme.typography.bodyMedium,
            color = tintColor
        )
    }
}

@Composable
private fun UserProfileDetail(modifier: Modifier = Modifier, profile: UserProfile) {
    val context = LocalContext.current
    val avatarUrl: String = remember { StringUtil.getBigAvatarUrl(profile.portrait) }
    val flowArrangement = Arrangement.spacedBy(8.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = flowArrangement
    ) {
        FlowRow(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Avatar(data = avatarUrl, size = 96.dp, contentDescription = null)

            Spacer(Modifier.size(16.dp))

            Column {
                Text(
                    text = profile.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                profile.userName?.let { userName -> // 同时显示用户名与昵称
                    Text(
                        text = userName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
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
                        VerticalDivider()
                        StatusText(
                            name = stringResource(id = R.string.text_stat_fans),
                            status = remember { profile.fans.getShortNumString() },
                        )
                        // 赞
                        VerticalDivider()
                        StatusText(
                            name = stringResource(id = R.string.text_stat_agrees),
                            status = profile.agreeNum,
                        )
                        // 吧龄
                        VerticalDivider()
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
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        FlowRow (
            horizontalArrangement = flowArrangement,
            verticalArrangement = flowArrangement
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

        FlowRow(
            horizontalArrangement = flowArrangement,
            verticalArrangement = flowArrangement
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

@Preview("UserProfileDetail", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun UserProfileDetailPreview() {
    TiebaLiteTheme {
        UserProfileDetail(
            modifier = Modifier.padding(16.dp),
            profile = UserProfileViewModel.parseUserProfile(
                User(
                    name = "(tieba#0812)",
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