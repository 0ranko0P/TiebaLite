package com.huanchengfly.tieba.post.ui.page.user

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.PermissionListBean
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.components.glide.BlurTransformation
import com.huanchengfly.tieba.post.components.imageProcessor.ImageProcessor
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.models.database.UserProfile
import com.huanchengfly.tieba.post.theme.FloatProducer
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isLooseWindowWidth
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.page.user.edit.EditProfileActivity
import com.huanchengfly.tieba.post.ui.page.user.likeforum.UserLikeForumPage
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostPage
import com.huanchengfly.tieba.post.ui.page.user.thread.UserThreadPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.CollapsingAvatarTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Switch
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TextPref
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.DefaultLoadingScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreenScope
import com.huanchengfly.tieba.post.utils.ColorUtils
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class Tab(val titleRes: Int) {
    THREADS(R.string.title_profile_threads),

    POSTS(R.string.title_profile_posts),

    FORUMS(R.string.title_profile_forums);

    fun formatTitle(number: Int, context: Context): String {
        return context.getString(titleRes, if (number > 0) number.getShortNumString() else " ")
    }
}

private typealias TabWithTitle = Pair<Tab, String>

/** The default size of an avatar in [UserProfileDetail] */
private val AvatarLandscapeSize: Dp
    @Composable @ReadOnlyComposable get() = if (isWindowHeightCompact()) 96.dp else 144.dp

/** The default expanded size of an avatar in [UserProfileTopAppBar] */
private val AvatarExpandedSize: Dp
    @Composable @ReadOnlyComposable get() = if (isWindowWidthCompact()) 96.dp else 128.dp

/** The default expanded height of a [UserProfileTopAppBar] */
private val UserAppbarExpandHeight: Dp
    @Composable @ReadOnlyComposable get() = 160.dp + AvatarExpandedSize

/** Whether to use landscape layout or not */
private val ContentLandscapeLayout: Boolean
    @Composable @ReadOnlyComposable get() = isLooseWindowWidth()

// Blurry avatar background
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AvatarBackground(
    imgProcessor: ImageProcessor,
    avatar: String,
    collapseFraction: FloatProducer
) {
    val color = MaterialTheme.colorScheme.background
    val collapsedAlpha = remember(color) {
        if (ColorUtils.isColorLight(color.toArgb())) 0.9f else 0.8f
    }

    GlideImage(
        model = avatar,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    // draw surface mask with alpha, this is better than change alpha directly
                    drawRect(
                        color = color.copy(lerp(collapsedAlpha, 1f, collapseFraction()))
                    )
                }
            },
        contentScale = ContentScale.Crop,
    ) {
        it.transform(BlurTransformation(imgProcessor, 120f))
    }
}

/**
 * Creates a transparent [TopAppBarColors] for displaying [AvatarBackground].
 * */
@Composable
private fun rememberAvatarTopBarColors(): TopAppBarColors {
    val defaultColors = TopAppBarDefaults.topAppBarColors()
    return remember(defaultColors) {
        defaultColors.copy(
            containerColor = defaultColors.containerColor.copy(0.01f), // Nearly transparent
            scrolledContainerColor = defaultColors.containerColor.copy(0.01f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfilePage(
    uid: Long,
    avatar: String?,
    nickname: String?,
    username: String?,
    transitionKey: String?,
    navigator: NavController,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val onBack: () -> Unit = navigator::navigateUp
    val snackbarHostState = rememberSnackbarHostState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collapseFraction = FloatProducer { scrollBehavior.state.collapsedFraction }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val uiMessage = when (it) {
            is UserProfileUiEvent.FollowSuccess -> it.message

            is UserProfileUiEvent.FollowFailed -> getString(R.string.toast_like_failed, it.message)

            is UserProfileUiEvent.UnfollowFailed -> getString(R.string.toast_unlike_failed, it.message)

            is UserProfileUiEvent.PermissionListException -> getString(R.string.toast_ban_interact_failed, it.message)

            is CommonUiEvent.Toast -> it.message.toString()

            else -> Unit
        }
        if (uiMessage is String) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(uiMessage)
        }
    }

    // Compose fullscreen avatar background when possible
    val avatarUrl = avatar ?: uiState.userProfile?.let { remember { StringUtil.getAvatarUrl(it.portrait) } }
    if (!avatarUrl.isNullOrEmpty()) {
        AvatarBackground(viewModel.imageProcessor, avatar = avatarUrl, collapseFraction)
    }

    StateScreen(
        isLoading = uiState.isRefreshing || uiState.userProfile == null,
        error = uiState.error,
        onReload = viewModel::onRefresh,
        loadingScreen = {
            LoadingScreen(Modifier, uid, avatar, nickname, username, transitionKey, onBack)
        }
    ) {
        val userProfile = uiState.userProfile ?: return@StateScreen
        val account = LocalAccount.current
        val isSelf = account?.uid == uid
        val blockState by viewModel.blockState.collectAsStateWithLifecycle()

        var showPermissionSettingDialogDialog by remember { mutableStateOf(false) }

        if (showPermissionSettingDialogDialog) {
            PermissionSettingDialogM2(
                initialPermissionList = uiState.permList ?: PermissionListBean(),
                onDismissRequest = { showPermissionSettingDialogDialog = false },
                onConfirm = { updatedBean ->
                    viewModel.setUserBlack(updatedBean)
                }
            )
        }

        val tabs: List<TabWithTitle> = remember {
            Tab.entries.mapNotNull {
                when(it) {
                    Tab.THREADS -> it to it.formatTitle(userProfile.thread, context)

                    Tab.POSTS -> (it to it.formatTitle(userProfile.post, context)).takeIf { isSelf }

                    Tab.FORUMS -> it to it.formatTitle(userProfile.forum, context)
                }
            }
        }

        val pagerState = rememberPagerState { tabs.size }
        val lazyListStates = rememberPagerListStates(tabs.size)
        val pagerContents = remember(tabs.size) {
            tabs.mapIndexed { i, (tab, _) ->
                movableContentOf<Boolean> { fluid ->
                    val lazyListState = lazyListStates[i]
                    when (tab) {
                        Tab.THREADS -> UserThreadPage(uid, fluid, lazyListState)

                        Tab.POSTS -> UserPostPage(uid, fluid, lazyListState)

                        Tab.FORUMS -> when {
                            userProfile.privateForum -> UserPageHide(hiddenTab = tab)

                            userProfile.forum == 0 -> UserPageEmpty()

                            else -> UserLikeForumPage(uid, fluid, lazyListState)
                        }
                    }
                }
            }
        }

        val movablePager = remember(tabs.size) {
            movableContentOf<Modifier, Boolean> { modifier, fluid ->
                Column {
                    UserProfileTabRow(tabs, pagerState, collapseFraction)

                    HorizontalPager(pagerState, Modifier.fillMaxSize() then modifier, key = { it }) {
                        ProvideNavigator(navigator) {
                            pagerContents[it](fluid)
                        }
                    }
                }
            }
        }

        val contentLandscapeLayout = ContentLandscapeLayout
        MyScaffold(
            topBar = {
                UserProfileTopAppBar(
                    transitionKey = transitionKey,
                    profile = userProfile,
                    myUid = account?.uid,
                    isFollowing = userProfile.following,
                    isRequestingFollow = uiState.isRequestingFollow,
                    onActionClicked = {
                        when {
                            isSelf -> context.goToActivity<EditProfileActivity>()

                            userProfile.following -> viewModel.onUnFollowClicked()

                            else -> viewModel.onFollowClicked()
                        }
                    }.takeUnless { uiState.isRefreshing || account == null },
                    onRefreshClicked = viewModel::onRefresh,
                    block = blockState,
                    onBlackListClicked = viewModel::onUserBlacklisted,
                    onWhiteListClicked = viewModel::onUserWhitelisted,
                    onSetUserBlack = { showPermissionSettingDialogDialog = true },
                    onBack = onBack,
                    scrollBehavior = scrollBehavior,
                ) {
                    UserIntro(
                        modifier = Modifier.padding(start = 16.dp),
                        profile = userProfile,
                        landscape = contentLandscapeLayout
                    )
                }
            },
            snackbarHostState = snackbarHostState,
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (contentLandscapeLayout) {
                    Row {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.35f)
                                .verticalScroll(rememberScrollState())
                                .padding(start = 16.dp, bottom = 16.dp)
                        ) {
                            UserProfileDetail(
                                profile = userProfile,
                                block = blockState,
                                transitionKey = transitionKey,
                                landscape = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            UserIntro(profile = userProfile, landscape = true)
                        }

                        movablePager(Modifier.weight(1.0f), true/* fluid */)
                    }
                } else {
                    movablePager(
                        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        false // fluid
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileTopAppBar(
    modifier: Modifier = Modifier,
    transitionKey: String? = null,
    profile: UserProfile,
    block: UserBlockState,
    myUid: Long? = null,
    isFollowing: Boolean = false,
    isRequestingFollow: Boolean = false,
    onActionClicked: (() -> Unit)? = null,
    onRefreshClicked: () -> Unit = {},
    onBlackListClicked: () -> Unit = {},
    onWhiteListClicked: () -> Unit = {},
    onSetUserBlack: () -> Unit,
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isSelf = myUid == profile.uid
    val actionsMenu: @Composable RowScope.() -> Unit = {
        if (isRequestingFollow) {
            CircularProgressIndicator(
                modifier = Modifier.minimumInteractiveComponentSize(),
                strokeWidth = 2.5.dp
            )
        } else if (onActionClicked != null) {
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

        ClickMenu(
            menuContent = {
                TextMenuItem(
                    text = R.string.btn_refresh,
                    onClick = onRefreshClicked
                )
                if (!isSelf) {
                    val isInBlackList = block == UserBlockState.Blacklisted
                    val isInWhiteList = block == UserBlockState.Whitelisted
                    TextMenuItem (
                        text = if (isInBlackList) R.string.title_remove_black else R.string.title_add_black,
                        onClick = onBlackListClicked
                    )
                    TextMenuItem(
                        text = if (isInWhiteList) R.string.title_remove_white else R.string.title_add_white,
                        onClick = onWhiteListClicked
                    )
                    if (myUid != null) {
                        TextMenuItem(text = R.string.ban_interact, onClick = onSetUserBlack)
                    }
                }
            },
            triggerShape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.btn_more),
                modifier = Modifier.minimumInteractiveComponentSize()
            )
        }
    }

    if (!ContentLandscapeLayout) {
        val titleModifier = Modifier.padding(start = 8.dp)
        CollapsingAvatarTopAppBar(
            modifier = modifier,
            avatar = {
                UserAvatar(
                    modifier = Modifier.matchParentSize(),
                    avatar = remember { StringUtil.getBigAvatarUrl(profile.portrait) },
                    uid = profile.uid,
                    transitionKey = transitionKey
                )
            },
            title = {
                val nickname = profile.nickname ?: profile.name
                Nickname(modifier = titleModifier, nickname, transitionKey, block)
            },
            subtitle = {
                UserProfileDetail(titleModifier, profile, block, transitionKey, landscape = false)
            },
            navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
            actions = actionsMenu,
            expandedHeight = UserAppbarExpandHeight,
            avatarMax = AvatarExpandedSize,
            colors = rememberAvatarTopBarColors(),
            scrollBehavior = scrollBehavior,
            collapsibleExtraContent = true,
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

@Composable
private fun UserProfileTabRow(
    tabs: List<TabWithTitle>,
    pagerState: PagerState,
    collapseFraction: FloatProducer,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = modifier,
        indicator = {
            FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
        },
        containerColor = Color.Transparent,
        contentColor = colorScheme.primary,
        divider = { // visible when collapsed
            HorizontalDivider(modifier = Modifier.graphicsLayer { alpha = collapseFraction() })
        },
    ) {
        tabs.fastForEachIndexed { i, (_, title) ->
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
                text = { Text(text = title) },
                unselectedContentColor = colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun UserAvatar(modifier: Modifier = Modifier, avatar: String?, uid: Long, transitionKey: String?) {
    if (avatar.isNullOrEmpty()) {
        Box(modifier = modifier.placeholder(shape = CircleShape))
    } else {
        val context = LocalContext.current
        Avatar(
            modifier = modifier
                .clickable {
                    PhotoViewActivity.launchSinglePhoto(context, url = avatar, useTbGlideUrl = false)
                }
                .onNotNull(avatar) { sharedUserAvatar(uid = uid, extraKey = transitionKey) },
            data = avatar
        )
    }
}

@NonRestartableComposable
@Composable
private fun NameText(
    modifier: Modifier = Modifier,
    name: String,
    block: UserBlockState = UserBlockState.None,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = name,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.MiddleEllipsis,
        textDecoration = TextDecoration.LineThrough.takeIf { block == UserBlockState.Blacklisted },
        style = style
    )
}

@Composable
private fun Nickname(
    modifier: Modifier = Modifier,
    nickname: String?,
    transitionKey: String? = null,
    block: UserBlockState = UserBlockState.None
) {
    NameText(
        modifier = modifier.block {
            if (nickname.isNullOrEmpty()) {
                placeholder()
            } else {
                sharedUserNickname(nickname = nickname, extraKey = transitionKey)
            }
        },
        name = nickname ?: stringResource(R.string.app_name),
        block = block,
    )
}

@Composable
private fun Username(
    modifier: Modifier = Modifier,
    username: String,
    transitionKey: String? = null,
    block: UserBlockState = UserBlockState.None,
) {
    NameText(
        modifier = modifier.sharedUsername(username, extraKey = transitionKey),
        name = remember { "(${username})" },
        block = block,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun StatText(modifier: Modifier = Modifier, title: String, num: String) {
    Column (
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(text = num, color = LocalContentColor.current, fontWeight = FontWeight.Bold)
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
private fun UserProfileDetail(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    block: UserBlockState = UserBlockState.None,
    transitionKey: String? = null,
    landscape: Boolean
) {
    val isWindowHeightCompact = isWindowHeightCompact()
    val context = LocalContext.current

    val userStats = remember(profile) {
        persistentListOf(
            context.getString(R.string.text_stat_follow) to profile.follow.getShortNumString(), // 关注
            context.getString(R.string.text_stat_fans) to profile.fans.getShortNumString(),     // 粉丝
            context.getString(R.string.text_stat_agrees) to profile.agree.getShortNumString(),  // 赞
            context.getString(R.string.text_stat_tbage) to context.getString(R.string.text_profile_tb_age, profile.tbAge), // 吧龄
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = if (isWindowHeightCompact) Alignment.Top else Alignment.CenterVertically
        ),
    ) {
        if (landscape) {
            UserAvatar(
                modifier = Modifier
                    .size(AvatarLandscapeSize)
                    .align(Alignment.CenterHorizontally),
                avatar = remember { StringUtil.getBigAvatarUrl(profile.portrait) },
                uid = profile.uid,
                transitionKey = transitionKey,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (landscape) {
                val nickname = profile.nickname ?: profile.name
                Nickname(nickname = nickname, transitionKey = transitionKey, block = block)
            }

            if (profile.nickname != null && profile.name.isNotEmpty()) {
                Username(username = profile.name, transitionKey = transitionKey, block = block)
            }

            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clipToBounds()
                        .horizontalScroll(rememberScrollState())
                ) {
                    userStats.fastForEachIndexed { i, (title, number) ->
                        StatText(title = title, num = number)
                        if (i != userStats.lastIndex) {
                            VerticalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserIntro(modifier: Modifier = Modifier, profile: UserProfile, landscape: Boolean) {
    val context = LocalContext.current
    val flowArrangement = Arrangement.spacedBy(6.dp)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = profile.intro ?: stringResource(id = R.string.tip_no_intro),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (landscape) Int.MAX_VALUE else 3,
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

@Composable
private fun UserProfileDetailPlaceholder(
    modifier: Modifier = Modifier,
    uid: Long,
    avatar: String? = null,
    nickname: String? = null,
    username: String? = null,
    transitionKey: String? = null,
    landscape: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = if (isWindowHeightCompact()) Alignment.Top else Alignment.CenterVertically
        ),
        horizontalAlignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (landscape) {
            UserAvatar(modifier = Modifier.size(AvatarLandscapeSize), avatar, uid, transitionKey)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (landscape) {
                Nickname(nickname = nickname, transitionKey = transitionKey)
            }

            if (!username.isNullOrEmpty()) {
                Username(username = username, transitionKey = transitionKey)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start
                ),
            ) {
                repeat(4) {
                    Box(modifier = Modifier.size(width = 28.dp, height = 40.dp).placeholder())
                    if (it < 3) {
                        VerticalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StateScreenScope.LoadingScreen(
    modifier: Modifier = Modifier,
    uid: Long = -1,
    avatar: String? = null,
    nickname: String? = null,
    username: String? = null,
    transitionKey: String? = null,
    onBack: () -> Unit = {},
) {
    val landscapeLayout = ContentLandscapeLayout
    val navIcon = remember {
        movableContentOf { BackNavigationIcon(onBackPressed = onBack) }
    }
    val loadingLottieContent = remember {
        movableContentOf<Modifier> {
            Box(modifier = it, contentAlignment = Alignment.Center) {
                DefaultLoadingScreen()
            }
        }
    }
    val introPlaceholderContent = remember {
        movableContentOf<Modifier> {
            Column(modifier = it) {
                Text(
                    text = stringResource(id = R.string.tip_no_intro),
                    modifier = Modifier.placeholder(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Sex, ID, IP
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(38.dp, 24.dp).placeholder(shape = CircleShape))
                    Box(modifier = Modifier.size(140.dp, 24.dp).placeholder(shape = CircleShape))
                    Box(modifier = Modifier.size(86.dp, 24.dp).placeholder(shape = CircleShape))
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (!landscapeLayout) {
            val titleModifier = Modifier.padding(start = 8.dp)
            CollapsingAvatarTopAppBar(
                avatar = {
                    UserAvatar(Modifier.matchParentSize(), avatar, uid, transitionKey)
                },
                title = {
                    Nickname(titleModifier, nickname, transitionKey)
                },
                subtitle = {
                    UserProfileDetailPlaceholder(titleModifier, uid, avatar, nickname, username, transitionKey)
                },
                navigationIcon = navIcon,
                expandedHeight = UserAppbarExpandHeight,
                avatarMax = AvatarExpandedSize,
                colors = rememberAvatarTopBarColors(),
                collapsibleExtraContent = true
            ) {
                introPlaceholderContent(Modifier.padding(start = 16.dp))
            }
        } else {
            TopAppBar(title = {}, navigationIcon = navIcon, colors = rememberAvatarTopBarColors())
        }

        if (landscapeLayout) {
            Row {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.35f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, bottom = 16.dp)
                ) {
                    UserProfileDetailPlaceholder(
                        uid = uid,
                        avatar = avatar,
                        nickname = nickname,
                        username = username,
                        transitionKey = transitionKey,
                        landscape = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    introPlaceholderContent(Modifier)
                }

                loadingLottieContent(Modifier.fillMaxSize())
            }
        } else {
            loadingLottieContent(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun UserPageHide(hiddenTab: Tab) {
    val context = LocalContext.current
    TipScreen(
        title = {
            val hiddenText = remember {
                context.getString(R.string.profile_title_hidden, hiddenTab.formatTitle(0, context))
            }
            Text(text = hiddenText)
        },
        modifier = Modifier.fillMaxSize(),
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_hide))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
                    .aspectRatio(2.5f)
            )
        },
    )
}

@Composable
private fun UserPageEmpty() {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.title_empty)) },
        modifier = Modifier.fillMaxSize(),
        image = {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.lottie_empty_box)
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.0f)
            )
        },
    )
}

@Composable
fun PermissionSettingDialogM2(
    modifier: Modifier = Modifier,
    initialPermissionList: PermissionListBean,
    onDismissRequest: () -> Unit,
    onConfirm: (PermissionListBean) -> Unit
) {
    // 维护对话框内部的临时状态
    var currentBean by remember { mutableStateOf(initialPermissionList.copy()) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "拉黑范围",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(5.dp))
                // 1. 禁止关注
                val followChecked = currentBean.follow == 1
                TextPref(
                    title = "禁止TA关注我",
                    leadingIcon = Icons.Outlined.Block,
                    onClick = {
                        val next = !followChecked
                        currentBean = currentBean.copy(follow = if (next) 1 else 0)
                    }
                ) {
                    Switch(
                        checked = followChecked,
                        onCheckedChange = { isChecked ->
                            currentBean = currentBean.copy(follow = if (isChecked) 1 else 0)
                        }
                    )
                }
                // 2. 禁止互动
                val interactChecked = currentBean.interact == 1
                TextPref(
                    title = "禁止TA互动",
                    leadingIcon = Icons.Outlined.Block,
                    summary = "包含转,评,赞踩,@",
                    onClick = {
                        val next = !interactChecked
                        currentBean = currentBean.copy(interact = if (next) 1 else 0)
                    }
                ) {
                    Switch(
                        checked = interactChecked,
                        onCheckedChange = { isChecked ->
                            currentBean = currentBean.copy(interact = if (isChecked) 1 else 0)
                        }
                    )
                }

                // 3. 禁止私信
                val chatChecked = currentBean.chat == 1
                TextPref(
                    title = "禁止TA私信",
                    leadingIcon = Icons.Outlined.Block,
                    onClick = {
                        val next = !chatChecked
                        currentBean = currentBean.copy(chat = if (next) 1 else 0)
                    }
                ) {
                    Switch(
                        checked = chatChecked,
                        onCheckedChange = { isChecked ->
                            currentBean = currentBean.copy(chat = if (isChecked) 1 else 0)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(currentBean)
                onDismissRequest()
            }) {
                Text("确定", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPermissionDialog() {
    TiebaLiteTheme {
        PermissionSettingDialogM2(
            initialPermissionList = PermissionListBean(1, 1, 1),
            onDismissRequest = {},
            onConfirm = {}
        )
    }
}


@Preview("UserProfileDetail", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun UserProfileDetailPreview() = TiebaLiteTheme {
    UserProfileDetail(
        modifier = Modifier.padding(16.dp),
        profile = UserProfile(
            uid = -1,
            name = "我是0812",
            nickname = "我是谁",
            tbAge = "12.4",
            address = "第二经济开发区",
            bazuDesc = "吃瓜吧吧主",
            newGod = "吃瓜"
        ),
        landscape = false
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("LoadingScreen")
@Composable
private fun LoadingScreenPreview() = TiebaLiteTheme {
    Surface {
        with(StateScreenScope()) {
            LoadingScreen(nickname = "我是谁", username = "user(123)")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("LoadingScreenLandscape", device = Devices.PIXEL_TABLET)
@Composable
private fun LoadingScreenLandscapePreview() = TiebaLiteTheme {
    Surface {
        with(StateScreenScope()) {
            LoadingScreen(nickname = "我是谁", username = "user(123)")
        }
    }
}
