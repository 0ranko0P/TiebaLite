package com.huanchengfly.tieba.post.ui.page.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.LocalDevicePosture
import com.huanchengfly.tieba.post.LocalNotificationCountFlow
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowHeightSizeClass
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.user.UserPage
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.utils.MainNavigationType
import com.huanchengfly.tieba.post.ui.utils.getNavType
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
private fun NavigationWrapper(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationType: MainNavigationType,
    navigationContentPosition: MainNavigationContentPosition,
    content: @Composable BoxScope.() -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = navigationType == MainNavigationType.PERMANENT_NAVIGATION_DRAWER) {
            NavigationDrawerContent(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        AnimatedVisibility(visible = navigationType == MainNavigationType.NAVIGATION_RAIL) {
            NavigationRail(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            content = content
        )
    }
}

/**
 * Workaround to enable background blurring since MainPage's BottomBar is outside the [BlurScaffold].
 *
 * This requires compose [emptyBlurBottomNavigation] inside the BlurScaffold.
 * */
@NonRestartableComposable
@Composable
private fun BlurBottomNavigation(
    modifier: Modifier = Modifier,
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
) = BottomNavigation(
    modifier = modifier,
    currentPosition = currentPosition,
    onChangePosition = onChangePosition,
    onReselected = onReselected,
    navigationItems = navigationItems,
    themeColors = LocalExtendedColors.current.copy(bottomBar = Color.Transparent)
)

// Empty BottomNavigation for background blurring
val emptyBlurBottomNavigation: @Composable () -> Unit = {
    val devicePosture by LocalDevicePosture.current
    val windowWidthSizeClass by rememberUpdatedState(LocalWindowSizeClass.current.widthSizeClass)
    val isBottomNavigation by remember { derivedStateOf {
        windowWidthSizeClass.getNavType(devicePosture) == MainNavigationType.BOTTOM_NAVIGATION
    } }

    if (isBottomNavigation) {
        Box(
            modifier = Modifier
                .background(color = LocalExtendedColors.current.bottomBar)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(BottomNavigationHeight)
        )
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun MainPage(
    navHostController: NavHostController,
    viewModel: MainViewModel = pageViewModel<MainUiIntent, MainViewModel>(emptyList()),
) {
    val coroutineScope = rememberCoroutineScope()
    val windowSizeClass = LocalWindowSizeClass.current
    val windowHeightSizeClass by rememberUpdatedState(newValue = windowSizeClass.heightSizeClass)
    val windowWidthSizeClass by rememberUpdatedState(newValue = windowSizeClass.widthSizeClass)
    val foldingDevicePosture by LocalDevicePosture.current

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

    val pagerState = rememberPagerState { 4 }

    val onItemClicked: (position: Int) -> Unit = remember { {
        coroutineScope.launch { pagerState.scrollToPage(it) }
    } }

    val navigationItems = remember { persistentListOf(
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
        ),
        NavigationItem(
            id = "user",
            icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_person) },
            title = R.string.title_user,
        ))
    }
    val navigationType by remember {
        derivedStateOf { windowWidthSizeClass.getNavType(foldingDevicePosture) }
    }

    /**
     * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
     * ergonomics and reachability depending upon the height of the device.
     */
    val navigationContentPosition by remember {
        derivedStateOf {
            when (windowHeightSizeClass) {
                WindowHeightSizeClass.Compact -> {
                    MainNavigationContentPosition.TOP
                }

                WindowHeightSizeClass.Medium,
                WindowHeightSizeClass.Expanded -> {
                    MainNavigationContentPosition.CENTER
                }

                else -> {
                    MainNavigationContentPosition.TOP
                }
            }
        }
    }
    val onReselected: (Int) -> Unit = {
        coroutineScope.emitGlobalEvent(
            GlobalEvent.Refresh(navigationItems[it].id)
        )
    }
    ProvideNavigator(navigator = navHostController) {
        NavigationWrapper(
            currentPosition = pagerState.currentPage,
            onChangePosition = onItemClicked,
            onReselected = onReselected,
            navigationItems = navigationItems,
            navigationType = navigationType,
            navigationContentPosition = navigationContentPosition
        ) {
            LazyLoadHorizontalPager(
                state = pagerState,
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

            AnimatedVisibility(
                visible = navigationType == MainNavigationType.BOTTOM_NAVIGATION,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BlurBottomNavigation(
                    currentPosition = pagerState.currentPage,
                    onChangePosition = onItemClicked,
                    onReselected = onReselected,
                    navigationItems = navigationItems
                )
            }
        }
    }
    BackHandler(enabled = pagerState.currentPage != 0) {
        onItemClicked(0)
    }
}

