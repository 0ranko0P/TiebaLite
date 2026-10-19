package com.huanchengfly.tieba.post.ui.page.user.fans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.core.designsystem.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.models.user.ConcernType
import com.huanchengfly.tieba.post.ui.models.user.FanUser
import com.huanchengfly.tieba.post.ui.page.user.fans.UserFansViewModel.Companion.UserFansVmFactory
import com.huanchengfly.tieba.post.ui.page.user.followlist.FollowListFilter
import com.huanchengfly.tieba.post.ui.page.user.followlist.FollowListTipsItem
import com.huanchengfly.tieba.post.ui.page.user.followlist.UserFollowButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.MoreMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.containerWidthFraction
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedListItemColors
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.stickyHeaderBackground
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlin.random.Random

private const val TipsContentType = Int.MIN_VALUE
private const val HeaderContentType = ""
// FanUserContentType uses Null by default

@Composable
fun UserFansPage(
    uid: Long = 0,
    onBack: () -> Unit,
    onUserClicked: (uid: Long) -> Unit,
    viewModel: UserFansViewModel = hiltViewModel<UserFansViewModel, UserFansVmFactory> { factory ->
        factory.create(uid)
    },
) {
    val showActions = (uid == 0L || uid == LocalAccount.current?.uid)
    val lazyListState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = rememberSnackbarHostState()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val uiMessage = when (it) {
            is UserFansUiEvent.FollowFailed -> getString(R.string.toast_like_failed, it.message)

            is UserFansUiEvent.UnfollowFailed -> getString(R.string.toast_unlike_failed, it.message)

            is CommonUiEvent.Toast -> it.message

            else -> Unit
        }
        if (uiMessage is String) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(uiMessage)
        }
    }

    var markedUnfollowUser: FanUser? by remember { mutableStateOf(null) }
    ConfirmUnfollowDialog(
        user = markedUnfollowUser,
        onConfirm = viewModel::onUnfollowClicked,
        onDismiss = { markedUnfollowUser = null }
    )

    MyScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.title_fans))
                },
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
                actions = {
                    ClickMenu(
                        menuContent = {
                            TextIconMenuItem(
                                text = stringResource(R.string.btn_refresh),
                                icon = Icons.Rounded.Refresh
                            ) {
                                lazyListState.requestScrollToItem(0)
                                scrollBehavior.state.contentOffset = 0f
                                viewModel.onRefresh()
                            }

                            TextIconMenuItem(
                                text = stringResource(R.string.btn_back_to_top),
                                icon = Icons.Rounded.VerticalAlignTop,
                                onClick = {
                                    lazyListState.requestScrollToItem(0)
                                    scrollBehavior.state.contentOffset = 0f
                                }
                            )
                        },
                        triggerShape = CircleShape,
                        content = MoreMenuItem,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHostState = snackbarHostState,
        backgroundColor = MaterialTheme.colorScheme.background,
    ) { contentPaddings ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val filter = uiState.filter
        val filteredUsers by viewModel.filteredUsers.collectAsStateWithLifecycle()

        StateScreen(
            isEmpty = uiState.users.isEmpty(),
            isLoading = uiState.isRefreshing,
            error = uiState.error,
            onReload = viewModel::onRefresh,
            screenPadding = contentPaddings,
        ) {
            FanList(
                data = filteredUsers,
                tipsText = uiState.tipsText,
                fansCount = uiState.fansCount,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = if (showActions) {
                    contentPaddings + PaddingValues(bottom = 72.dp) // Bottom padding for FollowListFilter
                } else {
                    contentPaddings + PaddingValues(bottom = 16.dp)
                },
                state = lazyListState,
                scrollBehavior = scrollBehavior,
                isLoading = {
                    uiState.isLoadingMore
                },
                onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                onClick = {
                    onUserClicked(it.uid)
                },
                onFollowActionClicked = { user: FanUser ->
                    when (user.concernType) {
                        ConcernType.FOLLOWING,
                        ConcernType.MUTUAL -> markedUnfollowUser = user // Show confirm Dialog
                        ConcernType.NONE,
                        ConcernType.FANS -> viewModel.onFollowClicked(user)
                        else -> Unit
                    }
                }.takeIf { showActions }
            )

            if (showActions) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    FollowListFilter(
                        modifier = Modifier.padding(contentPaddings),
                        filter = filter,
                        onFilterChange = viewModel::onFilterChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun FanList(
    data: List<FanUser>,
    tipsText: String?,
    fansCount: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    state: LazyListState = rememberLazyListState(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    isLoading: () -> Boolean = { false },
    onLazyLoad: (() -> Unit)? = null,
    onClick: (FanUser) -> Unit = {},
    onFollowActionClicked: ((FanUser) -> Unit)? = null,
) {
    val context = LocalContext.current
    val listItemColors = SegmentedListItemColors
    val listItemElevation = ListItemElevation(Dp.Hairline, Dp.Hairline)
    val listItemContentPadding = PaddingValues(10.dp) // ListItem.InteractiveListStartPadding
    val listItemWidthFraction by containerWidthFraction()

    SwipeUpLazyLoadColumn(
        modifier = Modifier.fillMaxSize() then modifier,
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
        isLoading = isLoading(),
        onLazyLoad = onLazyLoad,
        bottomIndicator = {},
    ) {
        tipsText?.let {
            item(key = TipsContentType, contentType = TipsContentType) {
                FollowListTipsItem(tips = it)
            }
        }

        stickyHeader(key = HeaderContentType, contentType = HeaderContentType) {
            val colors = TopAppBarDefaults.topAppBarColors()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onNotNull(scrollBehavior?.state) { stickyHeaderBackground(it, colors, state) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = if (fansCount > 0) {
                        stringResource(id = R.string.text_fans_count, fansCount)
                    } else {
                        stringResource(id = R.string.text_fans_none)
                    },
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        itemsIndexed(items = data, key = { _, it -> it.uid }) { index, user ->
            SegmentedListItem(
                onClick = {
                    onClick(user)
                },
                shapes = ListItemDefaults.segmentedShapes(index, count = data.size),
                modifier = Modifier
                    .fillMaxWidth(listItemWidthFraction)
                    .padding(horizontal = 16.dp),
                leadingContent = {
                    Avatar(data = user.avatar, size = Sizes.Small)
                },
                trailingContent = onFollowActionClicked?.let { {
                    UserFollowButton(
                        concernType = user.concernType,
                        onClick = { onFollowActionClicked(user) }
                    )
                } },
                verticalAlignment = Alignment.CenterVertically,
                colors = listItemColors,
                elevation = listItemElevation,
                contentPadding = listItemContentPadding,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = user.displayName,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (user.followTime > 0) {
                        Text(
                            text = remember {
                                val relativeTime = DateTimeUtils.getRelativeTimeString(
                                    context = context,
                                    timestamp = user.followTime * 1000L
                                )
                                context.getString(R.string.text_fans_follow_time, relativeTime)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else if (!user.intro.isNullOrEmpty()) {
                        Text(
                            text = user.intro,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmUnfollowDialog(
    dialogState: DialogState = rememberDialogState(),
    user: FanUser?,
    onConfirm: (FanUser) -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(user) {
        dialogState.show = user != null
    }

    if (!dialogState.show || user == null) return
    ConfirmDialog(
        dialogState = dialogState,
        onConfirm = {
            onConfirm(user)
        },
        onDismiss = onDismiss,
        title = {
            Text(text = stringResource(R.string.button_unfollow))
        },
        content = {
            Text(text = stringResource(R.string.title_dialog_unfollow_user, user.displayName))
        }
    )
}

@Preview("UserFansPreview")
@Composable
private fun UserFansPreview() = TiebaLiteTheme {
    ComposeUiFlags.isMediaQueryIntegrationEnabled = true
    val users = LongRange(0, 20).map { i ->
        FanUser(
            uid = i,
            avatar = "",
            displayName = "Test User · $i",
            portrait = "",
            intro = "This is a test user $i".takeIf { Random.nextBoolean() },
            followTime = 0,
            concernType = Random.nextInt(0, 3),
        )
    }

    Surface {
        FanList(
            data = users,
            tipsText = "仅展示登录的用户和正常账号",
            fansCount = users.size,
            onFollowActionClicked = {},
        )
    }
}
