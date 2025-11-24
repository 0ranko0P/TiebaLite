package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.HotTopicList
import com.huanchengfly.tieba.post.ui.page.Destination.Search
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.consumeResult
import com.huanchengfly.tieba.post.ui.page.main.emptyBlurBottomNavigation
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.HotPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.page.main.rememberTopAppBarScrollBehaviors
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResult
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResultKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabEnterTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabExitTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultHazeBlock
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultInputScale
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberScrollStateConnection
import com.huanchengfly.tieba.post.utils.BooleanBitSet
import com.huanchengfly.tieba.post.utils.LocalAccount
import dev.chrisbanes.haze.ExperimentalHazeApi
import kotlinx.coroutines.launch
import kotlin.math.abs

sealed class ExplorePageItem(val title: Int){
    object Concern : ExplorePageItem(R.string.title_concern)

    object Personalized : ExplorePageItem(R.string.title_personalized)

    object Hot : ExplorePageItem(R.string.title_hot)
}

/**
 * Common [ThreadItem] onClick listeners for [ConcernPage], [PersonalizedPage] and [HotPage]
 * */
@Immutable
class ThreadClickListeners(
    val onClicked: (ThreadItem) -> Unit,
    val onReplyClicked: (ThreadItem) -> Unit,
    val onAuthorClicked: (Author) -> Unit,
    val onForumClicked: (ThreadItem) -> Unit,
    val onNavigateHotTopicList: () -> Unit // Not a thread click listener, place here just for convenience
)

fun createThreadClickListeners(
    onNavigate: (Destination, navOptions: NavOptions?, navigatorExtras: Navigator.Extras?) -> Unit
) = ThreadClickListeners(
    onClicked = { thread ->
        val (forumId, _, _) = thread.simpleForum
        onNavigate(Destination.Thread(threadId = thread.id, forumId), null, null)
    },
    onReplyClicked = { thread ->
        val (forumId, _, _) = thread.simpleForum
        onNavigate(Destination.Thread(threadId = thread.id, forumId, scrollToReply = true), null, null)
    },
    onAuthorClicked = { author ->
        onNavigate(Destination.UserProfile(uid = author.id), null, null)
    },
    onForumClicked = { thread ->
        val (_, forumName, forumAvatar) = thread.simpleForum
        val extraKey = thread.id.toString()
        onNavigate(Destination.Forum(forumName, forumAvatar, extraKey), null, null)
    },
    onNavigateHotTopicList = {
        onNavigate(HotTopicList, null, null)
    }
)

@Composable
private fun ExplorePageTab(
    pagerState: PagerState,
    pages: List<ExplorePageItem>
) {
    val coroutineScope = rememberCoroutineScope()

    SecondaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = {
            FancyAnimatedIndicatorWithModifier(index = pagerState.currentPage)
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        pages.fastForEachIndexed { index, item ->
            val selected = pagerState.currentPage == index
            Tab(
                text = {
                    Text(
                        text = stringResource(id = item.title),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                },
                selected = selected,
                onClick = {
                    if (selected) return@Tab
                    coroutineScope.launch {
                        if (abs(pagerState.currentPage - index) > 1) {
                            pagerState.scrollToPage(index)
                        } else {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                },
                unselectedContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalHazeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExplorePage() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val account = LocalAccount.current
    val navigator = LocalNavController.current
    val loggedIn by remember { derivedStateOf { account != null } }
    val windowSize = LocalWindowAdaptiveInfo.current.windowSizeClass

    val pages = remember(loggedIn) {
        listOfNotNull(
            ExplorePageItem.Concern.takeIf { loggedIn },
            ExplorePageItem.Personalized,
            ExplorePageItem.Hot
        )
    }
    val pagerState = rememberPagerState(initialPage = if (loggedIn) 1 else 0) { pages.size }
    val listStates = rememberPagerListStates(pages.size)

    val scrollBehaviors = rememberTopAppBarScrollBehaviors(pages.size) {
        if (windowSize.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)) {
            TopAppBarDefaults.pinnedScrollBehavior(state = it)
        } else {
            TopAppBarDefaults.enterAlwaysScrollBehavior(state = it)
        }
    }

    val scrollStateConnection = rememberScrollStateConnection()

    // FAB visibility of each page
    var fabHideStates by remember(pages) { mutableStateOf(BooleanBitSet()) }

    // Like event from explorePages
    onGlobalEvent<ThreadLikeUiEvent>(coroutineScope) {
        context.toastShort(it.toMessage(context))
    }
    val explorePages = remember(pages.size, account?.uid) {
        pages.mapIndexed { index, page: ExplorePageItem ->
            movableContentOf<Modifier, PaddingValues, (Boolean) -> Unit> { modifier, contentPadding, onHideFab ->
                val listState = listStates[index]
                when (page) {
                    ExplorePageItem.Concern -> {
                        ConcernPage(modifier, contentPadding, listState, navigator, onHideFab)
                    }

                    ExplorePageItem.Personalized -> {
                        PersonalizedPage(modifier, contentPadding, listState, navigator, onHideFab)
                    }

                    ExplorePageItem.Hot -> {
                        HotPage(modifier, contentPadding, listState, navigator, onHideFab)
                    }
                }
            }
        }
    }

    BlurScaffold(
        topHazeBlock = {
            blurEnabled = !fabHideStates[pagerState.currentPage] || pagerState.isScrolling
            inputScale = DefaultInputScale
        },
        topBar = {
            TopAppBar(
                titleRes = R.string.title_explore,
                navigationIcon = accountNavIconIfCompact,
                actions = {
                    ActionItem(
                        icon = Icons.Rounded.Search,
                        contentDescription = R.string.title_search,
                        onClick = { navigator.navigate(route = Search) }
                    )
                },
                scrollBehavior = scrollBehaviors[pagerState.currentPage]
            ) {
                ExplorePageTab(pagerState = pagerState, pages = pages)
            }
        },
        bottomBar = emptyBlurBottomNavigation, // MainPage workaround when enabling BottomBar blurring
        bottomHazeBlock = DefaultHazeBlock,
        floatingActionButton = {
            // FAB visibility: not scrolling, pager not scrolling, current page not refreshing
            val visible by remember {
                derivedStateOf {
                    !scrollStateConnection.isScrolling && !pagerState.isScrolling && !fabHideStates[pagerState.currentPage]
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = DefaultFabEnterTransition,
                exit = DefaultFabExitTransition
            ) {
                FloatingActionButton(
                    onClick = {
                        val currentPageItem = pages[pagerState.currentPage]
                        coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(currentPageItem))
                    },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = Dp.Hairline)
                ) {
                    Icon(Icons.Rounded.VerticalAlignTop, stringResource(R.string.btn_back_to_top))
                }
            }
        }
    ) { contentPadding ->
        Container {
            HorizontalPager(
                state = pagerState,
                key = { pages[it].title },
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollStateConnection),
                verticalAlignment = Alignment.Top,
                flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f)
            ) { i ->
                // Attach ScrollBehavior connections
                val pageModifier = Modifier.nestedScroll(scrollBehaviors[i].nestedScrollConnection)

                // Callbacks when page requesting FAB to hide
                val onHideFab: (Boolean) -> Unit = { hide: Boolean -> fabHideStates = fabHideStates.set(i, hide) }

                explorePages[i](pageModifier, contentPadding, onHideFab)
            }
        }
    }
}

@Composable
fun LaunchedFabStateEffect(
    item: ExplorePageItem,
    listState: LazyListState,
    onHideFab: (Boolean) -> Unit,
    isRefreshing: Boolean,
    isError: Boolean
) {
    onGlobalEvent<GlobalEvent.ScrollToTop> {
        if (item === it.tag) {
            listState.scrollToItem(0)
        }
    }

    val isTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset > 0 }
    }

    LaunchedEffect(isTop, onHideFab, isRefreshing, isError) {
        onHideFab(!isTop || isRefreshing || isError)
    }
}

@Composable
fun ConsumeThreadPageResult(navigator: NavController, onThreadResult: (threadId: Long, Like) -> Unit) {
    LaunchedEffect(Unit) {
        navigator.consumeResult<ThreadResult>(ThreadResultKey)?.run {
            onThreadResult(threadId, Like(liked, likes))
        }
    }
}
