package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FabPosition
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.animateEnterExit
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.HotTopicList
import com.huanchengfly.tieba.post.ui.page.Destination.Search
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.consumeResult
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.ui.page.main.bottomNavigationPlaceholder
import com.huanchengfly.tieba.post.ui.page.main.calculateMainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.HotPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResult
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResultKey
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultHazeStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultInputScale
import com.huanchengfly.tieba.post.ui.widgets.compose.enterAlwaysOnLowerBoundScrollBehavior
import com.huanchengfly.tieba.post.ui.widgets.compose.hazeSource
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.utils.BooleanBitSet
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
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
    val onAuthorClicked: (ThreadItem) -> Unit,
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
    onAuthorClicked = { thread ->
        val route = thread.run {
            Destination.UserProfile(user = author, transitionKey = this.id.toString())
        }
        onNavigate(route, null, null)
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

// Note: Obtain Root AnimatedVisibilityScope by LocalAnimatedVisibilityScope.current
@Composable
fun AnimatedVisibilityScope.ExplorePage(loggedIn: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navigator = LocalNavController.current
    val navigationSuiteType = calculateMainNavigationSuiteType()
    // Hide FAB on FloatingNavigationBarCompact
    val isFloatingNavBarCompat = navigationSuiteType === MainNavigationSuiteType.FloatingNavigationBarCompact
    val hazeState: HazeState? = LocalHazeState.current
    val hazeInputScale = defaultInputScale()
    val sharedTransitionScope = LocalSharedTransitionScope.current

    val pages = remember(loggedIn) {
        listOfNotNull(
            ExplorePageItem.Concern.takeIf { loggedIn },
            ExplorePageItem.Personalized,
            ExplorePageItem.Hot
        )
    }
    val pagerState = rememberPagerState(initialPage = if (loggedIn) 1 else 0) { pages.size }
    val listStates = rememberPagerListStates(pages.size)

    val scrollOrientationConnection = rememberScrollOrientationConnection()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysOnLowerBoundScrollBehavior()

    val onScrollToTopClicked: () -> Unit = {
        coroutineScope.launch {
            listStates[pagerState.currentPage].scrollToItem(0)
            scrollBehavior.state.contentOffset = 0f
        }
    }

    // FAB visibility of each page
    var fabHideStates by remember(pages) { mutableStateOf(BooleanBitSet()) }

    // Like event from explorePages
    onGlobalEvent<ThreadLikeUiEvent>(coroutineScope) {
        context.toastShort(it.toMessage(context))
    }

    if (isFloatingNavBarCompat) {
        onGlobalEvent<GlobalEvent.ScrollToTop>(filter = { it.tag == MainDestination.Explore }) {
            onScrollToTopClicked()
        }
    }

    MyScaffold(
        useMD2Layout = hazeState == null,
        topBar = {
            TopAppBarPaged(
                modifier = Modifier
                    .topAppBarBlurEffect(
                        sharedTransitionScope = sharedTransitionScope,
                        rootAnimatedVisibilityScope = LocalAnimatedVisibilityScope.current,
                        hazeState = hazeState,
                        style = defaultHazeStyle(),
                        inputScale = hazeInputScale,
                        blurEnabled = { !fabHideStates[pagerState.currentPage] || pagerState.isScrolling }
                    ),
                title = { Text(text = stringResource(R.string.title_explore)) },
                navigationIcon = {
                    AccountNavIconIfCompact(onLoginClicked = { navigator.navigate(Destination.Login) })
                },
                actions = {
                    ActionItem(
                        icon = Icons.Rounded.Search,
                        contentDescription = R.string.title_search,
                        onClick = { navigator.navigateDebounced(route = Search) }
                    )
                },
                scrollBehavior = scrollBehavior,
                canScrollBackward = { // No transition running && canScrollBackward
                    sharedTransitionScope?.isTransitionActive != true && !transition.isRunning &&
                            listStates[pagerState.currentPage].canScrollBackward
                }
            ) {
                ExplorePageTab(pagerState = pagerState, pages = pages)
            }
        },
        bottomBar = bottomNavigationPlaceholder, // MainPage BottomNavBar placeholder
        bottomBarAtop = navigationSuiteType.isFloatingNavigationBar,
        floatingActionButton = {
            if (isFloatingNavBarCompat) return@MyScaffold
            // FAB visibility: scrolling forward, pager not scrolling, current page not refreshing
            val visible by remember {
                derivedStateOf {
                    !transition.isRunning && scrollOrientationConnection.isScrollingForward &&
                    !pagerState.isScrolling && !fabHideStates[pagerState.currentPage]
                }
            }
            DefaultBackToTopFAB(visible = visible, onClick = onScrollToTopClicked)
        },
        floatingActionButtonPosition = if (isFloatingNavBarCompat) FabPosition.EndOverlay else FabPosition.End,
    ) { contentPadding ->
        Container(
            modifier = Modifier.onNotNull(hazeState) { hazeSource(state = it) }
        ) {
            HorizontalPager(
                state = pagerState,
                key = { pages[it].title },
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollOrientationConnection),
                verticalAlignment = Alignment.Top,
                flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f)
            ) { index ->
                // Attach ScrollBehavior connections
                val modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                val onHideFab: (Boolean) -> Unit = { hideFab ->
                    fabHideStates = fabHideStates.set(index, hideFab)
                }
                val listState = listStates[index]

                when (pages[index]) {
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
}

context(mainAnimatedContentScope: AnimatedVisibilityScope)
fun Modifier.topAppBarBlurEffect(
    sharedTransitionScope: SharedTransitionScope?,
    rootAnimatedVisibilityScope: AnimatedVisibilityScope?,
    hazeState: HazeState?,
    style: HazeStyle = HazeStyle.Unspecified,
    inputScale: HazeInputScale = HazeInputScale.None,
    block: (HazeEffectScope.() -> Unit)? = null,
    blurEnabled: () -> Boolean,
): Modifier = this then Modifier
    .onNotNull(rootAnimatedVisibilityScope, sharedTransitionScope) { (rootAnimatedVisibilityScope, sharedTransitionScope) ->
        animateEnterExit(
            zIndexInOverlay = 1.0f,
            animatedVisibilityScope = rootAnimatedVisibilityScope,
            sharedTransitionScope = sharedTransitionScope
        )
    }
    .onNotNull(hazeState) {
        hazeEffect(state = it, style = style) {
            // Disable background blur when MainNavHost transition is running
            this.blurEnabled = !mainAnimatedContentScope.transition.isRunning && blurEnabled()
            this.inputScale = inputScale
            if (block != null) block()
        }
    }

@Composable
fun LaunchedFabStateEffect(
    listState: LazyListState,
    onHideFab: (Boolean) -> Unit,
    isRefreshing: Boolean,
    isError: Boolean
) {
    val noScrollBackward by remember { derivedStateOf { !listState.canScrollBackward } }

    LaunchedEffect(noScrollBackward, onHideFab, isRefreshing, isError) {
        onHideFab(noScrollBackward || isRefreshing || isError)
    }
}

@Composable
inline fun <reified Route : Any> ConsumeThreadPageResult(
    navigator: NavController,
    crossinline onThreadResult: (threadId: Long, Like) -> Unit
) {
    LaunchedEffect(Unit) {
        navigator.consumeResult<Route, ThreadResult>(ThreadResultKey)?.run {
            onThreadResult(threadId, Like(liked, likes))
        }
    }
}
