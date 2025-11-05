package com.huanchengfly.tieba.post.ui.page.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.models.search.SearchForum
import com.huanchengfly.tieba.post.ui.models.search.SearchSuggestion
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.rememberTopAppBarScrollBehaviors
import com.huanchengfly.tieba.post.ui.page.search.forum.SearchForumItem
import com.huanchengfly.tieba.post.ui.page.search.forum.SearchForumPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import com.huanchengfly.tieba.post.ui.page.search.user.SearchUserPage
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchBox
import com.huanchengfly.tieba.post.ui.widgets.compose.TabClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.Options
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import kotlin.math.abs

object SearchToolbarSharedBoundsKey

object SearchIconSharedElementKey

private enum class SearchPages(val titleRes: Int) {
    Forum(titleRes = R.string.title_search_forum),
    Thread(titleRes = R.string.title_search_thread),
    User(titleRes = R.string.title_search_user)
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    navigator: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = rememberSnackbarHostState()

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

    val sortTypes = remember {
        persistentMapOf(
            SearchThreadSortType.SORT_TYPE_NEWEST to R.string.title_search_order_new,
            SearchThreadSortType.SORT_TYPE_OLDEST to R.string.title_search_order_old,
            SearchThreadSortType.SORT_TYPE_RELATIVE to R.string.title_search_order_relevant
        )
    }

    val pages: List<SearchPages> = SearchPages.entries
    val pagerState = rememberPagerState(0) { pages.size }
    val listStates = rememberPagerListStates(pages.size)
    val scrollBehaviors = rememberTopAppBarScrollBehaviors(pages.size) {
        TopAppBarDefaults.pinnedScrollBehavior(state = it)
    }
    val movablePageContents = remember {
        pages.map { page ->
            movableContentOf<PaddingValues, String, Int> { contentPadding, keyword, threadSortType ->
                // attach ScrollBehavior connection
                val modifier = Modifier.nestedScroll(scrollBehaviors[page.ordinal].nestedScrollConnection)
                val listState = listStates[page.ordinal]
                when(page) {
                    SearchPages.Forum -> SearchForumPage(modifier, keyword, contentPadding, listState)

                    SearchPages.Thread -> {
                        SearchThreadPage(modifier, keyword, threadSortType, contentPadding, listState)
                    }

                    SearchPages.User -> SearchUserPage(modifier, keyword, contentPadding, listState)
                }
            }
        }
    }

    var inputKeyword by rememberSaveable { mutableStateOf("") }

    // Callback for HistoryList, SearchBox and SuggestionList
    val onKeywordSubmit: (String) -> Unit = {
        val newKeyword = it.trim()
        viewModel.onSubmitKeyword(newKeyword)
        keyboardController?.hide()
        if (inputKeyword != newKeyword) inputKeyword = newKeyword
        focusManager.clearFocus()
        scrollBehaviors.fastForEach { scrollBehavior ->
            scrollBehavior.state.contentOffset = 0f // reset all scroll behavior
        }
        coroutineScope.launch {
            listStates[pagerState.currentPage].scrollToItem(0)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Submitted search keyword
    val isKeywordNotEmpty = uiState.isKeywordNotEmpty
    // Input search keyword
    val isInputKeywordNotEmpty by remember {
        derivedStateOf { inputKeyword.isNotEmpty() && inputKeyword.isNotBlank() }
    }

    val showSuggestion by remember {
        derivedStateOf { isInputKeywordNotEmpty && inputKeyword != uiState.submittedKeyword }
    }

    BackHandler(enabled = isKeywordNotEmpty) {
        onKeywordSubmit("")
    }

    BlurScaffold(
        topHazeBlock = {
            blurEnabled = isKeywordNotEmpty && scrollBehaviors.isOverlapping(pagerState)
        },
        bottomHazeBlock = {
            blurEnabled = isKeywordNotEmpty
        },
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = {
                    SearchTopBar(
                        modifier = Modifier
                            .padding(start = Dp.Hairline, top = 8.dp, end = 18.dp, bottom = 8.dp)
                            .localSharedBounds(key = SearchToolbarSharedBoundsKey),
                        keyword = inputKeyword,
                        onKeywordChange = {
                            inputKeyword = it
                            viewModel.onKeywordInputChanged(keyword = it.trim())
                        },
                        onKeywordSubmit = onKeywordSubmit,
                        onBack = navigator::navigateUp
                    )
                },
                scrollBehavior = scrollBehaviors[pagerState.currentPage]
            ) {
                AnimatedVisibility(
                    visible = isInputKeywordNotEmpty && isKeywordNotEmpty && !showSuggestion,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    SearchTabRow(
                        pagerState = pagerState,
                        pages = pages,
                        sortTypes = sortTypes,
                        selectedSortType = uiState.sortType,
                        onSelectSortType = viewModel::onSortTypeChanged
                    )
                }
            }
        },
        snackbarHostState = snackbarHostState
    ) { contentPadding ->
        var isSearchHistoryExpanded by rememberSaveable { mutableStateOf(false) }

        if (!isInputKeywordNotEmpty) { // History list: empty input keyword
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
        } else if (showSuggestion) { // Suggestion list: input keyword != submitted keyword
            uiState.suggestion?.let {
                SearchSuggestionList(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    suggestion = it,
                    onForumClick = { f ->
                        val transitionKey = f.id.toString() // use forum ID as transition animation key
                        navigator.navigate(Destination.Forum(forumName = f.name, avatar = f.avatar, transitionKey))
                    },
                    onItemClick = onKeywordSubmit
                )
            }
        } else if (isKeywordNotEmpty) { // Search result pager
            ProvideNavigator(navigator = navigator) {
                LazyLoadHorizontalPager(
                    state = pagerState,
                    key = { pages[it].titleRes },
                    modifier = Modifier.fillMaxSize(),
                    flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f)
                ) { i ->
                    movablePageContents[i](contentPadding, uiState.submittedKeyword, uiState.sortType)
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingNone,
    suggestion: SearchSuggestion,
    onForumClick: (SearchForum) -> Unit = {},
    onItemClick: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp), // Align with search bar
        contentPadding = contentPadding
    ) {
        suggestion.forum?.let { forum ->
            item(key = forum.id, contentType = 0) {
                SearchForumItem(forum, transitionKey = forum.id.toString(), onClick = onForumClick)
            }
        }

        items(items = suggestion.suggestions, key = { it }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .animateItem()
                    .clickable {
                        onItemClick(it)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(id = R.string.desc_search_sug, it),
                )

                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SearchTabRow(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    pages: List<SearchPages>,
    sortTypes: Options<Int>,
    selectedSortType: Int,
    onSelectSortType: (Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val tabTextStyle = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)

    val onTabClicked: (index: Int) -> Unit = remember { { index ->
        coroutineScope.launch {
            if (abs(pagerState.currentPage - index) > 1) {
                pagerState.scrollToPage(index)
            } else {
                pagerState.animateScrollToPage(index)
            }
        }
    } }

    SecondaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = {
            FancyAnimatedIndicatorWithModifier(pagerState.currentPage, verticalPadding = 6.dp)
        },
        divider = {},
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier.width(75.dp * pages.size),
    ) {
        pages.fastForEachIndexed { index, item ->
            val selected = pagerState.currentPage == index

            if (item == SearchPages.Thread) {
                TabClickMenu(
                    selected = selected,
                    onClick = { onTabClicked(index) },
                    text = {
                        Text(text = stringResource(item.titleRes), style = tabTextStyle)
                    },
                    menuContent = {
                        ListPickerMenuItems(
                            items = sortTypes,
                            picked = selectedSortType,
                            onItemPicked = onSelectSortType
                        )
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Tab(
                    text = {
                        Text(text = stringResource(item.titleRes), style = tabTextStyle)
                    },
                    selected = selected,
                    onClick = { onTabClicked(index) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SearchHistoryList(
    modifier: Modifier = Modifier,
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    expanded: () -> Boolean,
    onToggleExpand: () -> Unit = {},
    onDelete: (String) -> Unit = {},
    onClear: () -> Unit = {},
) {
    val hasMore = history.size > 6

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .then(modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.title_search_history),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge
            )
            if (history.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.button_clear_all),
                    modifier = Modifier.clickableNoIndication(onClick = onClear),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        FlowRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val maxItemViewSize = if (!expanded() && hasMore) 6 else history.size
            val historyBackground = MaterialTheme.colorScheme.secondaryContainer

            for (i in 0 until maxItemViewSize) {
                val searchHistory = history[i]
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = historyBackground,
                ) {
                    Text(
                        text = searchHistory,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onHistoryClick(searchHistory) },
                                onLongClick = { onDelete(searchHistory) }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (hasMore) {
            TextButton(
                onClick = onToggleExpand,
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
            ) {
                Icon(
                    imageVector = if (expanded()) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (expanded()) {
                        stringResource(id = R.string.button_expand_less_history)
                    } else {
                        stringResource(id = R.string.button_expand_more_history)
                    }
                )
            }
        } else if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.tip_empty),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    modifier: Modifier = Modifier,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onKeywordSubmit: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    SearchBox(
        keyword = keyword,
        onKeywordChange = onKeywordChange,
        modifier = modifier.fillMaxWidth(),
        onKeywordSubmit = onKeywordSubmit,
        placeholder = {
            Text(
                text = stringResource(id = R.string.hint_search),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        prependIcon = {
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(id = R.string.button_back)
            )
        }
    )
}

@Preview("SearchBox")
@Composable
private fun PreviewSearchBox() {
    var keyword by remember { mutableStateOf("") }
    TiebaLiteTheme {
        Surface(
            modifier = Modifier
                .height(TopAppBarDefaults.TopAppBarExpandedHeight)
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            SearchTopBar(
                keyword = keyword,
                onKeywordChange = { keyword = it }
            )
        }
    }
}

@Preview("SearchHistoryList")
@Composable
private fun PreviewSearchHistoryList() {
    TiebaLiteTheme {
        var expanded by remember { mutableStateOf(false) }
        Surface {
            SearchHistoryList(
                history = (0..20).map { if (it % 2 == 0) "记录$it" else "搜索记录$it" },
                onHistoryClick = {},
                expanded = { expanded },
                onToggleExpand = { expanded = !expanded },
            )
        }
    }
}

@Preview("SearchSuggestionList", backgroundColor = 0xFFFFFFFF)
@Composable
private fun SearchSuggestionListPreview() = TiebaLiteTheme {
    Surface {
        SearchSuggestionList(
            suggestion = SearchSuggestion(
                forum = SearchForum(name = "吃瓜吧", slogan = "Test"),
                suggestions = listOf("1", "2", "3")
            )
        )
    }
}
