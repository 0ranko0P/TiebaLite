package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.localSharedElements
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.Destination.Search
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.emptyBlurBottomNavigation
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.HotPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.page.search.SearchIconSharedElementKey
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

private typealias ExplorePageItem = Int

@Composable
private fun ColumnScope.ExplorePageTab(
    pagerState: PagerState,
    pages: ImmutableList<ExplorePageItem>
) {
    val coroutineScope = rememberCoroutineScope()

    TabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions ->
            PagerTabIndicator(
                pagerState = pagerState,
                tabPositions = tabPositions
            )
        },
        divider = {},
        backgroundColor = Color.Transparent,
        contentColor = ExtendedTheme.colors.primary,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .width(76.dp * pages.size),
    ) {
        var selected = false
        pages.fastForEachIndexed { index, item ->
            selected = pagerState.currentPage == index

            Tab(
                text = {
                    Text(
                        text = stringResource(id = item),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.75.sp
                    )
                },
                selected = selected,
                onClick = {
                    coroutineScope.launch {
                        if (!selected) pagerState.animateScrollToPage(index)
                    }
                },
                unselectedContentColor = ExtendedTheme.colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ExplorePage() {
    val account = LocalAccount.current
    val navigator = LocalNavController.current
    val loggedIn = remember(account) { account != null }

    val pages = remember {
        listOfNotNull(
            R.string.title_concern.takeIf { loggedIn },
            R.string.title_personalized,
            R.string.title_hot,
        ).toImmutableList()
    }
    val pagerState = rememberPagerState(initialPage = if (account != null) 1 else 0) { pages.size }
    val listStates = rememberPagerListStates(size = pagerState.pageCount)

    BlurScaffold(
        backgroundColor = Color.Transparent,
        topHazeBlock = remember { {
            blurEnabled = with(pagerState) {
                currentPageOffsetFraction != 0f || listStates.getOrNull(currentPage)?.canScrollBackward == true
            }
        } },
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.title_explore),
                navigationIcon = accountNavIconIfCompact(),
                actions = {
                    IconButton(onClick = { navigator.navigate(Search) }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(id = R.string.title_search),
                            modifier = Modifier.localSharedElements(SearchIconSharedElementKey)
                        )
                    }
                },
                elevation = Dp.Hairline
            ) {
                ExplorePageTab(pagerState = pagerState, pages = pages)
            }
        },
        bottomBar = emptyBlurBottomNavigation,
    ) { contentPadding ->
        LazyLoadHorizontalPager(
            state = pagerState,
            key = { pages[it] },
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = true,
        ) {
            val listState = listStates[it]
            when(pages[it]) {
                R.string.title_concern -> ConcernPage(navigator, contentPadding, listState)

                R.string.title_personalized -> PersonalizedPage(navigator, contentPadding, listState)

                R.string.title_hot -> HotPage(navigator, contentPadding, listState)
            }
        }
    }
}