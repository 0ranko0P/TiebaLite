package com.huanchengfly.tieba.post.ui.page.search

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.sealedValues
import com.huanchengfly.tieba.post.models.database.KeywordProvider
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.rememberTopAppBarScrollBehaviors
import com.huanchengfly.tieba.post.ui.page.search.forum.SearchForumPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadUiEvent
import com.huanchengfly.tieba.post.ui.page.search.user.SearchUserPage
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchBox
import com.huanchengfly.tieba.post.ui.widgets.compose.TabClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.Options
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.math.abs

object SearchToolbarSharedBoundsKey

object SearchIconSharedElementKey

sealed class SearchPages(@StringRes val titleRes: Int) {
    object Forum : SearchPages(titleRes = R.string.title_search_forum)

    object Thread : SearchPages(titleRes = R.string.title_search_thread)

    object User : SearchPages(titleRes = R.string.title_search_user)
}

@OptIn(FlowPreview::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    navigator: NavController,
    viewModel: SearchViewModel = pageViewModel<SearchUiIntent, SearchViewModel>(
        listOf(SearchUiIntent.Init)
    ),
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchHistories by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::searchHistories,
        initial = persistentListOf()
    )
    val keyword by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::keyword,
        initial = ""
    )

    val isKeywordEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::isKeywordEmpty,
        initial = true
    )
    val suggestions by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::suggestions,
        initial = persistentListOf()
    )

    val showSuggestions by remember {
        derivedStateOf { suggestions.isNotEmpty() }
    }

    var inputKeyword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { inputKeyword }
            .debounce(500)
            .collect {
                viewModel.send(SearchUiIntent.KeywordInputChanged(it))
            }
    }

    LaunchedEffect(keyword) {
        if (keyword.isNotEmpty() && keyword != inputKeyword) {
            inputKeyword = keyword
        }
    }

    BackHandler(enabled = !isKeywordEmpty) {
        viewModel.send(SearchUiIntent.SubmitKeyword(""))
    }

    val sortTypes = remember {
        persistentMapOf(
            SearchThreadSortType.SORT_TYPE_NEWEST to R.string.title_search_order_new,
            SearchThreadSortType.SORT_TYPE_OLDEST to R.string.title_search_order_old,
            SearchThreadSortType.SORT_TYPE_RELATIVE to R.string.title_search_order_relevant
        )
    }
    val initialSortType = SearchThreadSortType.SORT_TYPE_NEWEST
    var searchThreadSortType by remember { mutableIntStateOf(initialSortType) }

    LaunchedEffect(searchThreadSortType) {
        emitGlobalEvent(SearchThreadUiEvent.SwitchSortType(searchThreadSortType))
    }
    viewModel.onEvent<SearchUiEvent.KeywordChanged> {
        inputKeyword = it.keyword
        if (it.keyword.isNotBlank()) emitGlobalEventSuspend(it)
    }

    // Callback for HistoryList, SearchBox and SuggestionList
    val onKeywordSubmit: (String) -> Unit = {
        if (inputKeyword != it) {
            inputKeyword = it
        }
        viewModel.send(SearchUiIntent.SubmitKeyword(it))
        keyboardController?.hide()
    }

    val pages = remember { sealedValues<SearchPages>() }
    val pagerState = rememberPagerState(0) { pages.size }
    val scrollBehaviors = rememberTopAppBarScrollBehaviors(pages.size) {
        TopAppBarDefaults.pinnedScrollBehavior(state = it)
    }

    BlurScaffold(
        topHazeBlock = {
            blurEnabled = !isKeywordEmpty && scrollBehaviors.isOverlapping(pagerState)
        },
        bottomHazeBlock = {
            blurEnabled = !isKeywordEmpty
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
                        onKeywordChange = { inputKeyword = it },
                        onKeywordSubmit = onKeywordSubmit,
                        onBack = {
                            if (isKeywordEmpty) {
                                navigator.navigateUp()
                            } else {
                                viewModel.send(SearchUiIntent.SubmitKeyword(""))
                            }
                        }
                    )
                },
                scrollBehavior = scrollBehaviors[pagerState.currentPage]
            ) {
                AnimatedVisibility(
                    visible = !isKeywordEmpty,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    SearchTabRow(
                        pagerState = pagerState,
                        pages = pages,
                        sortTypes = sortTypes,
                        selectedSortType = searchThreadSortType,
                        onSelectSortType = {
                            searchThreadSortType = it
                        }
                    )
                }
            }
        }
    ) { contentPadding ->
        var isSearchHistoryExpanded by rememberSaveable { mutableStateOf(false) }

        if (!isKeywordEmpty) {
            ProvideNavigator(navigator = navigator) {
                LazyLoadHorizontalPager(
                    state = pagerState,
                    key = { pages[it].titleRes },
                    modifier = Modifier.fillMaxSize(),
                    flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f)
                ) {
                    // Attach ScrollBehaviors connection on each page
                    val pageModifier = Modifier.nestedScroll(scrollBehaviors[it].nestedScrollConnection)

                    when(pages[it]) {
                        SearchPages.Forum -> SearchForumPage(pageModifier, keyword, contentPadding)

                        SearchPages.Thread -> {
                            SearchThreadPage(pageModifier, keyword, initialSortType, contentPadding)
                        }

                        SearchPages.User -> SearchUserPage(pageModifier, keyword, contentPadding)
                    }
                }
            }
        } else {
            if (showSuggestions) {
                SearchSuggestionList(contentPadding, suggestions, onItemClick = onKeywordSubmit)
            } else {
                Container(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(contentPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    SearchHistoryList(
                        searchHistories = searchHistories,
                        onSearchHistoryClick = onKeywordSubmit,
                        expanded = { isSearchHistoryExpanded },
                        onToggleExpand = { isSearchHistoryExpanded = !isSearchHistoryExpanded },
                        onDelete = {
                            val id = (it as SearchHistory).id
                            viewModel.send(SearchUiIntent.DeleteSearchHistory(id))
                        },
                        onClear = { viewModel.send(SearchUiIntent.ClearSearchHistory) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionList(
    contentPadding: PaddingValues = PaddingNone,
    suggestions: ImmutableList<String>,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .padding(horizontal = 16.dp) // Align with search bar
            .fillMaxWidth(),
        contentPadding = contentPadding
    ) {
        items(items = suggestions, key = { it }) {
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

@Preview("SearchSuggestionList", backgroundColor = 0xFFFFFFFF)
@Composable
private fun SearchSuggestionListPreview() {
    TiebaLiteTheme {
        Surface {
            SearchSuggestionList(
                suggestions = persistentListOf("1", "2", "3"),
                onItemClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    searchHistories: ImmutableList<KeywordProvider>,
    onSearchHistoryClick: (String) -> Unit,
    expanded: () -> Boolean,
    onToggleExpand: () -> Unit = {},
    onDelete: (KeywordProvider) -> Unit = {},
    onClear: () -> Unit = {},
) {
    val hasItem = searchHistories.isNotEmpty()
    val hasMore = searchHistories.size > 6

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.title_search_history),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge
            )
            if (hasItem) {
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
            val maxItemViewSize = if (!expanded() && hasMore) 6 else searchHistories.size
            val historyBackground = MaterialTheme.colorScheme.secondaryContainer

            for (i in 0 until maxItemViewSize) {
                val searchHistory = searchHistories[i]
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = historyBackground,
                ) {
                    Text(
                        text = searchHistory.getKeyword(),
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onSearchHistoryClick(searchHistory.getKeyword()) },
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
        } else if (!hasItem) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@OptIn(ExperimentalMaterial3Api::class)
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
                searchHistories = (0..20).map {
                    SearchHistory(content = if (it % 2 == 0) "记录$it" else "搜索记录$it")
                }.toImmutableList(),
                onSearchHistoryClick = {},
                expanded = { expanded },
                onToggleExpand = { expanded = !expanded },
            )
        }
    }
}