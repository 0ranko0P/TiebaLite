package com.huanchengfly.tieba.post.ui.page.user.followlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.core.designsystem.icon.PartnerHeart
import com.huanchengfly.tieba.post.core.designsystem.icon.PersonHeart
import com.huanchengfly.tieba.post.ui.models.user.ConcernType
import com.huanchengfly.tieba.post.ui.models.user.FollowUser
import com.huanchengfly.tieba.post.ui.page.user.followlist.FollowListViewModel.Companion.FollowListFilter
import com.huanchengfly.tieba.post.ui.page.user.followlist.FollowListViewModel.Companion.FollowListVmFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingTab
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.MoreMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedListItemColors
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.stickyHeaderBackground
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlin.random.Random

private val FollowListFilter.contentDescription: Int
    get() = when (this) {
        FollowListFilter.All -> R.string.title_concern
        FollowListFilter.Mutual -> R.string.filter_follow_mutual
    }

private const val TipsContentType = Int.MIN_VALUE
private const val HeaderContentType = ""
// FollowUserContentType is Null by default

@Composable
fun FollowListPage(
    uid: Long = 0,
    onBack: () -> Unit,
    onUserClicked: (uid: Long) -> Unit,
    viewModel: FollowListViewModel = hiltViewModel<FollowListViewModel, FollowListVmFactory> { factory ->
        factory.create(uid)
    },
) {
    val showActions = (uid == 0L || uid == LocalAccount.current?.uid)
    val lazyListState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = rememberSnackbarHostState()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val uiMessage = when (it) {
            is FollowListUiEvent.FollowFailed -> getString(R.string.toast_like_failed, it.message)

            is FollowListUiEvent.UnfollowFailed -> getString(R.string.toast_unlike_failed, it.message)

            is CommonUiEvent.Toast -> it.message

            else -> Unit
        }
        if (uiMessage is String) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(uiMessage)
        }
    }

    var markedUnfollowUser: FollowUser? by remember { mutableStateOf(null) }
    ConfirmUnfollowDialog(
        user = markedUnfollowUser,
        onConfirm = viewModel::onUnfollowClicked,
        onDismiss = { markedUnfollowUser = null }
    )

    MyScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.title_follow_list))
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
            FollowList(
                data = filteredUsers,
                tipsText = uiState.tipsText,
                totalFollowNum = uiState.totalFollowNum,
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
                onFollowActionClicked = { user: FollowUser ->
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
private fun FollowListFilter(
    filter: FollowListFilter,
    onFilterChange: (FollowListFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = remember {
        listOf(
            FollowListFilter.All to Icons.Rounded.PersonHeart,
            FollowListFilter.Mutual to Icons.Rounded.PartnerHeart,
        )
    }
    FloatingTabRow(
        modifier = modifier.padding(bottom = FloatingToolbarDefaults.ScreenOffset / 2),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        filters.fastForEach { (it, icon) ->
            FloatingTab(
                selected = filter === it,
                onClick = {
                    if (filter != it) onFilterChange(it)
                },
                icon = {
                    AnimatedVisibility(visible = filter === it) {
                        Icon(imageVector = icon, contentDescription = null)
                    }
                },
                content = { Text(text = stringResource(id = it.contentDescription)) }
            )
        }
    }
}

@Composable
private fun FollowList(
    data: List<FollowUser>,
    tipsText: String?,
    totalFollowNum: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    state: LazyListState = rememberLazyListState(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    isLoading: () -> Boolean = { false },
    onLazyLoad: (() -> Unit)? = null,
    onClick: (FollowUser) -> Unit = {},
    onFollowActionClicked: ((FollowUser) -> Unit)? = null,
) {
    val listItemColors = SegmentedListItemColors
    val listItemElevation = ListItemElevation(Dp.Hairline, Dp.Hairline)
    val listItemContentPadding = PaddingValues(10.dp) // ListItem.InteractiveListStartPadding

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
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TipsAndUpdates,
                        contentDescription = tipsText,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = it,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
                    text = stringResource(id = R.string.text_follow_list_count, totalFollowNum),
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
                modifier = Modifier.padding(horizontal = 16.dp),
                leadingContent = {
                    Avatar(data = user.avatar, size = Sizes.Small)
                },
                trailingContent = onFollowActionClicked?.let {
                    { FollowListFollowButton(user, onClick = { onFollowActionClicked(user) }) }
                },
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
                    user.intro?.let {
                        Text(
                            text = it,
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
private fun FollowListFollowButton(
    user: FollowUser,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = user.concernType != ConcernType.UPDATING,
        colors = if (user.concernType <= ConcernType.NONE) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        if (user.concernType != ConcernType.UPDATING) {
            Text(
                text = when (user.concernType) {
                    ConcernType.FANS,
                    ConcernType.NONE -> stringResource(id = R.string.button_follow)
                    ConcernType.FOLLOWING -> stringResource(id = R.string.text_followed)
                    ConcernType.MUTUAL -> stringResource(id = R.string.filter_follow_mutual)
                    else -> throw IllegalStateException()
                },
                style = MaterialTheme.typography.labelMedium,
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(Sizes.Tiny))
        }
    }
}

@Composable
private fun ConfirmUnfollowDialog(
    dialogState: DialogState = rememberDialogState(),
    user: FollowUser?,
    onConfirm: (FollowUser) -> Unit,
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

@Preview("FollowListPreview")
@Composable
private fun FollowListPreview() = TiebaLiteTheme {
    val users = LongRange(0, 20).map { i ->
        FollowUser(
            uid = i,
            avatar = "",
            displayName = "Test User · $i",
            portrait = "",
            intro = "This is a test user $i".takeIf { Random.nextBoolean() },
            concernType = Random.nextInt(0, 3),
        )
    }

    Surface {
        FollowList(
            data = users,
            tipsText = "仅展示登录的用户和正常账号",
            totalFollowNum = users.size,
            onFollowActionClicked = {},
        )
    }
}
