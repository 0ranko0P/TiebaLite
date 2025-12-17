package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.models.search.SearchThreadInfo
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.search.SearchHistoryList
import com.huanchengfly.tieba.post.ui.page.search.SearchUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchThreadItem
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.Options
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.persistentMapOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumSearchPostPage(
    forumName: String,
    navigator: NavController,
    viewModel: ForumSearchPostViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = rememberSnackbarHostState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect {
            when (it) {
                is SearchUiEvent -> snackbarHostState.showSnackbar(message = it.toMessage(context))
                else -> {/* Unknown UI Event */}
            }
        }
    }

    var inputKeyword by rememberSaveable { mutableStateOf("") }

    val currentSortType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::sortType,
        initial = ForumSearchPostSortType.NEWEST
    )
    val currentFilterType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::filterType,
        initial = ForumSearchPostFilterType.ALL
    )

    // Submitted search keyword
    val isKeywordNotEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::isKeywordNotEmpty,
        initial = false
    )

    // Input search keyword
    val isInputKeywordNotEmpty by remember {
        derivedStateOf { inputKeyword.isNotEmpty() && inputKeyword.isNotBlank() }
    }

    val onKeywordSubmit: (String) -> Unit = {
        val newKeyword = it.trim()
        viewModel.onSubmitKeyword(newKeyword)
        keyboardController?.hide()
        if (inputKeyword != newKeyword) inputKeyword = newKeyword
        focusManager.clearFocus()
        scrollBehavior.state.contentOffset = 0f // reset
    }

    val threadClickListener: (SearchThreadInfo) -> Unit = {
        when {
            it.postInfoContent != null -> {
                navigator.navigate(SubPosts(threadId = it.tid, subPostId = it.cid))
            }

            it.mainPostTitle != null -> {
                navigator.navigate(Thread(threadId = it.tid, postId = it.pid, scrollToReply = true))
            }

            else -> navigator.navigate(Thread(threadId = it.tid))
        }
    }

    BlurScaffold(
        topHazeBlock = {
            blurEnabled = isKeywordNotEmpty && scrollBehavior.isOverlapping
        },
        topBar = {
            TopAppBar(
                title = {
                    SearchBox(
                        keyword = inputKeyword,
                        onKeywordChange = { inputKeyword = it },
                        modifier = Modifier.padding(top = 8.dp, end = 18.dp, bottom = 8.dp),
                        onKeywordSubmit = onKeywordSubmit,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.hint_search_in_ba, forumName),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        prependIcon = {
                            Icon(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = navigator::navigateUp),
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.button_back)
                            )
                        }
                    )
                },
                scrollBehavior = scrollBehavior,
            ) {
                AnimatedVisibility(visible = isInputKeywordNotEmpty && isKeywordNotEmpty) {
                    SortToolBar(
                        modifier = Modifier.fillMaxWidth(),
                        sortType = { currentSortType },
                        filterType = { currentFilterType },
                        onSortTypeChanged = viewModel::onSortTypeChanged,
                        onFilterTypeChanged = viewModel::onFilterTypeChanged
                    )
                }
            }
        },
        snackbarHostState = snackbarHostState
    ) { contentPadding ->
        var isSearchHistoryExpanded by rememberSaveable { mutableStateOf(false) }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val error = uiState.error
        val isLoadingMore = uiState.isLoadingMore
        val hasMore = uiState.hasMore
        val data = uiState.data

        if (!isInputKeywordNotEmpty) {
            Container(
                modifier = Modifier.padding(contentPadding)
            ) {
                val history by viewModel.searchHistories.collectAsStateWithLifecycle()
                SearchHistoryList(
                    history = history,
                    onHistoryClick = onKeywordSubmit,
                    expanded = { isSearchHistoryExpanded },
                    onToggleExpand = { isSearchHistoryExpanded = !isSearchHistoryExpanded },
                    onDelete = viewModel::onDeleteHistory,
                    onClear = viewModel::onClearHistory
                )
            }
        } else if (isKeywordNotEmpty) {
            StateScreen(
                isEmpty = data.isEmpty(),
                isLoading = uiState.isRefreshing,
                error = error,
                onReload = viewModel::onRefresh,
                screenPadding = contentPadding,
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::onRefresh,
                    contentPadding = contentPadding
                ) {
                    ProvideNavigator(navigator = navigator) {
                        SwipeUpLazyLoadColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = contentPadding,
                            isLoading = isLoadingMore,
                            onLazyLoad = {
                                if (hasMore) viewModel.onLoadMore()
                            },
                            onLoad = null, // Refuse manual load more!
                            bottomIndicator = { onThreshold ->
                                LoadMoreIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    isLoading = isLoadingMore,
                                    noMore = !hasMore,
                                    onThreshold = onThreshold
                                )
                            }
                        ) {
                            itemsIndexed(data) { index, item ->
                                if (index > 0) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                SearchThreadItem(
                                    item = item,
                                    onClick = threadClickListener,
                                    onValidUserClick = {
                                        navigator.navigate(UserProfile(item.author.id))
                                    },
                                    onForumClick = null, // Hide forum info
                                    onQuotePostClick = {
                                        navigator.navigate(
                                            Thread(threadId = item.tid, postId = item.pid, scrollToReply = true)
                                        )
                                    },
                                    onMainPostClick = {
                                        navigator.navigate(Thread(threadId = item.tid))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortToolBar(
    modifier: Modifier = Modifier,
    sortType: () -> Int,
    filterType: () -> Int,
    onSortTypeChanged: (type: Int) -> Unit,
    onFilterTypeChanged: (type: Int) -> Unit
) {
    val sortTypes: Options<Int> = remember {
        persistentMapOf(
            ForumSearchPostSortType.NEWEST to R.string.title_search_post_sort_by_time,
            ForumSearchPostSortType.RELATIVE to R.string.title_search_post_sort_by_relevant,
        )
    }

    val filterTypes: List<Int> = remember {
        listOf(
            ForumSearchPostFilterType.ALL,
            ForumSearchPostFilterType.ONLY_THREAD
        )
    }
    val textColor = LocalContentColor.current
    val selectedTextColor = MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(IntrinsicSize.Min)
    ) {
        val menuState = rememberMenuState()

        val rotate by animateFloatAsState(
            targetValue = if (menuState.expanded) 180f else 0f,
            label = "ArrowIndicatorRotate"
        )

        ClickMenu(
            menuContent = {
                ListPickerMenuItems(items = sortTypes, picked = sortType(), onItemPicked = onSortTypeChanged)
            },
            menuState = menuState,
            indication = null
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(sortTypes[sortType()]!!),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = rotate }
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        filterTypes.fastForEachIndexed { index, type ->
            val selected = type == filterType()

            Text(
                text = stringResource(
                    id = when (type) {
                        ForumSearchPostFilterType.ALL -> R.string.title_search_filter_all

                        ForumSearchPostFilterType.ONLY_THREAD -> R.string.title_search_filter_only_thread

                        else -> throw RuntimeException("Invalid type: $type")
                    }
                ),
                color = if (selected) selectedTextColor else textColor,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clickableNoIndication(
                        role = Role.RadioButton,
                        enabled = !selected,
                        onClick = { onFilterTypeChanged(type) }
                    )
            )

            if (index != filterTypes.lastIndex) {
                VerticalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}
