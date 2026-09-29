package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentScope
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
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.core.network.model.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.core.ui.animation.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.core.navigation.Navigator
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.theme.compose.withNonNull
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.explore.ExploreType
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteType
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.core.ui.util.isListDetail
import com.huanchengfly.tieba.post.ui.page.main.OnMainNavigationScrollTopEvent
import com.huanchengfly.tieba.post.ui.page.main.bottomNavigationPlaceholder
import com.huanchengfly.tieba.post.ui.page.main.calculateMainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.HotPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TbHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.hazeSource
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.utils.BooleanBitSet
import dev.chrisbanes.haze.HazeTint
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ExploreType.title: Int
    @StringRes get() = when(this) {
        ExploreType.CONCERN -> R.string.title_concern
        ExploreType.PERSONALIZED -> R.string.title_personalized
        ExploreType.HOT -> R.string.title_hot
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
    val onOriginThreadClicked: (OriginThreadInfo) -> Unit,
    val onNavigateHotTopicList: () -> Unit // Not a thread click listener, place here just for convenience
)

fun createThreadClickListeners(
    onNavigate: (Destination) -> Unit
) = ThreadClickListeners(
    onClicked = { thread ->
        val (forumId, _, _) = thread.simpleForum
        onNavigate(Destination.Thread(threadId = thread.id, forumId))
    },
    onReplyClicked = { thread ->
        val (forumId, _, _) = thread.simpleForum
        onNavigate(Destination.Thread(threadId = thread.id, forumId, scrollToReply = true))
    },
    onAuthorClicked = { thread ->
        val navKey = thread.run {
            Destination.UserProfile(user = author, transitionKey = this.id.toString())
        }
        onNavigate(navKey)
    },
    onForumClicked = { thread ->
        val (_, forumName, forumAvatar) = thread.simpleForum
        val extraKey = thread.id.toString()
        onNavigate(Destination.Forum(forumName, forumAvatar, extraKey))
    },
    onOriginThreadClicked = {
        val navKey = Destination.Thread(threadId = it.tid.toLong(), forumId = it.fid)
        onNavigate(navKey)
    },
    onNavigateHotTopicList = { onNavigate(Destination.HotTopicList) }
)

@Composable
private fun ExplorePageTab(
    pagerState: PagerState,
    pages: List<ExploreType>
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

@Composable
fun AnimatedContentScope.ExplorePage(
    loggedIn: Boolean,
    navigator: Navigator,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    val navigationSuiteType = calculateMainNavigationSuiteType()
    // Hide FAB on FloatingNavigationBarCompact
    val isFloatingNavBarCompat = navigationSuiteType === TbNavigationSuiteType.FloatingNavigationBarCompact
    val isListDetail = isListDetail()
    val hazeState = LocalHazeState.current
    val sharedTransitionScope = LocalSharedTransitionScope.current

    val pages = remember(loggedIn) {
        listOfNotNull(
            ExploreType.CONCERN.takeIf { loggedIn },
            ExploreType.PERSONALIZED,
            ExploreType.HOT
        )
    }
    val pagerState = rememberPagerState(initialPage = if (loggedIn) 1 else 0) { pages.size }
    val listStates = rememberPagerListStates(pages.size)

    val scrollOrientationConnection = rememberScrollOrientationConnection()
    val scrollBehavior = if (!isListDetail) TopAppBarDefaults.enterAlwaysScrollBehavior() else null

    // FAB visibility of each page
    var fabHideStates by remember(pages) { mutableStateOf(BooleanBitSet()) }

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigate)
    }

    // Like event from explorePages
    onGlobalEvent<ThreadLikeUiEvent>(coroutineScope) {
        context.toastShort(it.toMessage(context))
    }

    OnMainNavigationScrollTopEvent<MainDestination.Explore>(
        coroutineScope = coroutineScope,
        topAppBarState = scrollBehavior?.state,
        listState = { listStates.getOrNull(pagerState.currentPage) }
    )

    MyScaffold(
        useMD2Layout = hazeState == null,
        topBar = {
            TopAppBarPaged(
                modifier = Modifier
                    .topAppBarBlurEffect(
                        transitionTint = HazeTint(colorScheme.surface),
                        hazeState = hazeState,
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
                        onClick = { navigator.navigate(key = Destination.Search) }
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
            // FAB visibility: scrolling forward, pager !scrolling, list !scrolling, not refreshing
            val visible by remember {
                derivedStateOf {
                    !transition.isRunning && scrollOrientationConnection.isScrollingForward &&
                            !pagerState.isScrolling && !listStates[pagerState.currentPage].isScrollInProgress &&
                            !fabHideStates[pagerState.currentPage]
                }
            }
            DefaultBackToTopFAB(visible = visible) {
                coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(MainDestination.Explore))
            }
        },
        floatingActionButtonPosition = if (isFloatingNavBarCompat) FabPosition.EndOverlay else FabPosition.End,
    ) { contentPadding ->
        Container(
            modifier = Modifier.onNotNull(hazeState) { hazeSource(state = it.state) },
            fluid = isListDetail,
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
                val modifier = scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier
                val onHideFab: (Boolean) -> Unit = { hideFab ->
                    fabHideStates = fabHideStates.set(index, hideFab)
                }
                val listState = listStates[index]

                when (pages[index]) {
                    ExploreType.CONCERN -> {
                        ConcernPage(modifier, contentPadding, threadClickListeners, listState, onHideFab)
                    }

                    ExploreType.PERSONALIZED -> {
                        PersonalizedPage(modifier, contentPadding, threadClickListeners, listState, onHideFab)
                    }

                    ExploreType.HOT -> HotPage(
                        modifier = modifier,
                        contentPadding = contentPadding,
                        listState = listState,
                        threadClickListeners = threadClickListeners,
                        onNavigateHotTopic = { navigator.navigate(key = it) },
                        onHideFab = onHideFab,
                    )
                }
            }
        }
    }
}

context(animatedContentScope: AnimatedContentScope)
fun Modifier.topAppBarBlurEffect(
    transitionTint: HazeTint = HazeTint.Unspecified,
    hazeState: TbHazeState?,
    blurEnabled: () -> Boolean,
): Modifier = this then Modifier
    .withNonNull(hazeState) {
        Modifier.defaultHazeEffect {
            // Disable background blur when MainNavHost transition is running
            this.blurEnabled = !animatedContentScope.transition.isRunning && blurEnabled()
            if (transitionTint !== HazeTint.Unspecified) {
                this.fallbackTint = if (this.blurEnabled) HazeTint.Unspecified else transitionTint
            }
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
