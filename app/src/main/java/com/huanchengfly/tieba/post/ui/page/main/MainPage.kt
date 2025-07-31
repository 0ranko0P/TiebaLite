package com.huanchengfly.tieba.post.ui.page.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.LocalNotificationCountFlow
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.user.UserPage
import com.huanchengfly.tieba.post.ui.utils.MainNavigationType
import com.huanchengfly.tieba.post.ui.utils.calculateNavigationType
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.NavigationBarWindowInsets
import com.huanchengfly.tieba.post.ui.widgets.compose.NavigationSuiteScaffold
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.ThemeUtil
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
    themeColors = LocalExtendedColors.current.copy(bottomBar = Color.Transparent)
)

// Empty BottomNavigation for background blurring
val emptyBlurBottomNavigation: @Composable () -> Unit = {
    if (!ThemeUtil.isTranslucentTheme() && isBottomNavigation()) {
        Box(
            modifier = Modifier
                .background(color = LocalExtendedColors.current.bottomBar)
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarWindowInsets)
                .height(BottomNavigationHeight)
        )
    }
}

@Composable
@ReadOnlyComposable
fun isBottomNavigation(): Boolean {
    val windowInfo = LocalWindowAdaptiveInfo.current
    return calculateNavigationType(windowInfo) == MainNavigationType.BOTTOM_NAVIGATION
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun MainPage(
    navHostController: NavHostController,
    viewModel: MainViewModel = pageViewModel<MainUiIntent, MainViewModel>(emptyList()),
) {
    val coroutineScope = rememberCoroutineScope()
    val account = LocalAccount.current

    val messageCount by viewModel.uiState.collectPartialAsState(
        prop1 = MainUiState::messageCount,
        initial = 0
    )

    val notificationCountFlow = LocalNotificationCountFlow.current
    LaunchedEffect(null) {
        notificationCountFlow.collect {
            viewModel.send(MainUiIntent.NewMessage.Receive(it))
        }
    }

    val navigationItems = remember { listOfNotNull(
        NavigationItem(
            id = "home",
            icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_inventory_2) },
            title = R.string.title_main,
        ),
        NavigationItem(
            id = "explore",
            icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_toy_fans) },
            title = R.string.title_explore,
        ),
        NavigationItem(
            id = "notification",
            icon = {
                AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_notifications)
            },
            title = R.string.title_notifications,
            badgeText = {messageCount.takeIf { it > 0 }?.toString() },
            onClick = {
                viewModel.send(MainUiIntent.NewMessage.Clear)
            },
        ).takeIf { account != null },
        NavigationItem(
            id = "user",
            icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_person) },
            title = R.string.title_user,
        ))
    }

    val pagerState = rememberPagerState { navigationItems.size }

    val onItemClicked: (position: Int) -> Unit = remember { {
        coroutineScope.launch { pagerState.scrollToPage(it) }
    } }

    ProvideNavigator(navigator = navHostController) {
        NavigationSuiteScaffold(
            currentPosition = pagerState.currentPage,
            onChangePosition = onItemClicked,
            navigationItems = navigationItems,
            navigationBarAtop = !ThemeUtil.isTranslucentTheme(),
        ) {
            LazyLoadHorizontalPager(
                state = pagerState,
                modifier = Modifier.windowInsetsPadding(NavigationBarWindowInsets.only(WindowInsetsSides.End)),
                key = { navigationItems[it].id },
                verticalAlignment = Alignment.Top,
                userScrollEnabled = false
            ) {
                when(navigationItems[it].title) {
                    R.string.title_main -> HomePage(canOpenExplore = true, onOpenExplore = { onItemClicked(1) })

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

