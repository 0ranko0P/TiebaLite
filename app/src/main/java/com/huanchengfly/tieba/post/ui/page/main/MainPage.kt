package com.huanchengfly.tieba.post.ui.page.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastMap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.user.UserPage
import com.huanchengfly.tieba.post.ui.utils.MainNavigationType
import com.huanchengfly.tieba.post.ui.utils.calculateNavigationType
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.NavigationSuiteScaffold
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

/**
 * Workaround to enable background blurring since MainPage's BottomBar is outside the [BlurScaffold].
 *
 * This requires compose [emptyBlurBottomNavigation] inside the BlurScaffold.
 * */
@NonRestartableComposable
@Composable
fun BlurBottomNavigation(
    modifier: Modifier = Modifier,
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    navigationItems: List<NavigationItem>,
) = BottomNavigation(
    modifier = modifier,
    currentPosition = currentPosition,
    onChangePosition = onChangePosition,
    navigationItems = navigationItems,
    containerColor = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onSurface
)

val emptyBlurBottomNavigation: @Composable () -> Unit = {
    val colorSchemeExt by ThemeUtil.colorState

    when {
        colorSchemeExt.colorScheme.isTranslucent -> {}

        isBottomNavigation() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = colorSchemeExt.navigationContainer)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(BottomNavigationHeight)
            )
        }
        else -> {
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
@ReadOnlyComposable
fun isBottomNavigation(): Boolean {
    val windowInfo = LocalWindowAdaptiveInfo.current
    return calculateNavigationType(windowInfo) == MainNavigationType.BOTTOM_NAVIGATION
}

@Composable
fun rememberNavigationItems(
    loggedIn: Boolean,
    messageCount: () -> Int
): List<NavigationItem> = remember(loggedIn) {
    listOfNotNull(
        NavigationItem(
            icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_inventory_2) },
            title = R.string.title_main,
        ),
        NavigationItem(
            icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_toy_fans) },
            title = R.string.title_explore,
        ),
        if (loggedIn) { // hide notifications when not logged-in
            NavigationItem(
                icon = {
                    AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_notifications)
                },
                title = R.string.title_notifications,
                badgeText = { messageCount().takeIf { it > 0 }?.toString() },
            )
        } else {
            null
        },
        NavigationItem(
            icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_person) },
            title = R.string.title_user,
        )
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun MainPage(
    navHostController: NavHostController,
    vm: MainPageViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val loggedIn = LocalAccount.current != null

    val messageCount by vm.messageCountFlow.collectAsStateWithLifecycle()

    val navigationItems = rememberNavigationItems(loggedIn, messageCount = { messageCount })
    val pagerState = rememberPagerState { navigationItems.size }

    val onItemClicked: (position: Int) -> Unit = {
        coroutineScope.launch { pagerState.scrollToPage(it) }
        if (navigationItems[it].title == R.string.title_notifications && messageCount > 0) {
            vm.onNavigateNotification()
        }
    }

    ProvideNavigator(navigator = navHostController) {
        NavigationSuiteScaffold(
            currentPosition = pagerState.currentPage,
            onChangePosition = onItemClicked,
            navigationItems = navigationItems,
            navigationBarAtop = !ThemeUtil.isTranslucentTheme(),
        ) {
            LazyLoadHorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets.only(WindowInsetsSides.End)),
                key = { navigationItems[it].title },
                verticalAlignment = Alignment.Top,
                userScrollEnabled = false
            ) {
                when(navigationItems[it].title) {
                    R.string.title_main -> HomePage(onOpenExplore = { onItemClicked(1) })

                    R.string.title_explore -> ExplorePage()

                    R.string.title_notifications -> NotificationsPage(fromHome = true)

                    R.string.title_user -> UserPage()
                }
            }
        }
    }
    BackHandler(enabled = pagerState.currentPage != 0) {
        onItemClicked(0)
    }
}

/**
 * Creates a list of [TopAppBarState] that is remembered across compositions.
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberTopAppBarScrollBehaviors(
    size: Int,
    init: @Composable (TopAppBarState) -> TopAppBarScrollBehavior
): List<TopAppBarScrollBehavior> {
    var result: List<TopAppBarScrollBehavior> by remember(size) { mutableStateOf(emptyList()) }

    val stateList = rememberSaveable(size, saver = Saver) {
        val states = mutableListOf<TopAppBarState>()
        repeat(size) {
            states.add(TopAppBarState(-Float.MAX_VALUE, 0f, 0f))
        }
        states.toImmutableList()
    }

    if (result.isEmpty()) {
        result = stateList.fastMap { init(it) }
    }
    return result
}

/** The default [Saver] implementation for list of [TopAppBarState]. */
@Suppress("UNCHECKED_CAST")
private val Saver: Saver<List<TopAppBarState>, *> = listSaver(
    save = {
        it.mapIndexed { i, it ->
            mutableListOf(it.heightOffsetLimit, it.heightOffset, it.contentOffset)
        }.reduce { rec, list ->
            rec.apply { addAll(list) }
        }
    },
    restore = {
        assert(it.isNotEmpty())
        val states = mutableListOf<TopAppBarState>()
        var trimList = it
        val saver = (TopAppBarState.Saver as Saver<TopAppBarState, List<Float>>)
        repeat(it.size / 3) { i ->
            trimList = it.subList(i * 3, it.size)
            val state = saver.restore(trimList)!!
            states.add(state)
        }
        states.toImmutableList()
    }
)
