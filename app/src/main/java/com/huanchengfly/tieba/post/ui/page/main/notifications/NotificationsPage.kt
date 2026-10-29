package com.huanchengfly.tieba.post.ui.page.main.notifications

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation3.runtime.NavKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.LocalNavSuiteScaffoldState
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteType.Companion.isNavigationBar
import com.huanchengfly.tieba.post.core.ui.component.ActionItem
import com.huanchengfly.tieba.post.core.ui.layout.isWindowHeightCompact
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.OnMainNavigationScrollTopEvent
import com.huanchengfly.tieba.post.ui.page.main.bottomNavigationPlaceholder
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurNavigationBarPlaceHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import kotlinx.coroutines.launch

private val NotificationsType.titleRes: Int
     @StringRes get() = when (this) {
        NotificationsType.ReplyMe -> R.string.title_reply_me

        NotificationsType.AtMe -> R.string.title_at_me
    }

@Composable
fun NotificationsPage(
    initialPage: NotificationsType = NotificationsType.ReplyMe,
    fromHome: Boolean = false,
    onBack: () -> Unit = {},
    onNavigate: (NavKey) -> Unit = {},
) {
    val pages = NotificationsType.entries
    val pagerState = rememberPagerState(initialPage = initialPage.ordinal, pageCount = { pages.size })
    val listStates = rememberPagerListStates(pages.size)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()

    OnMainNavigationScrollTopEvent<MainDestination.Notification>(
        coroutineScope = coroutineScope,
        topAppBarState = scrollBehavior.state,
        scrollableState = { listStates.getOrNull(pagerState.currentPage) }
    )

    MyScaffold(
        useMD2Layout = true,
        topBar = {
            NotificationsToolBar(
                onBack = onBack,
                onSearchClicked = {
                    onNavigate(Destination.Search)
                },
                pagerState = pagerState,
                fromHome = fromHome,
                scrollBehavior = scrollBehavior,
                canScrollBackward = {
                    listStates[pagerState.currentPage].canScrollBackward
                }
            )
        },
        bottomBar = if (fromHome) bottomNavigationPlaceholder else BlurNavigationBarPlaceHolder,
        bottomBarAtop = fromHome && LocalNavSuiteScaffoldState.current.layoutType.isFloatingNavigationBar,
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState,
            key = { pages[it] }
        ) {
            NotificationsListPage(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                type = NotificationsType.entries[it],
                onNavigate = onNavigate,
                listState = listStates[pagerState.currentPage],
                contentPadding = contentPadding
            )
        }
    }
}

/** Tab row for main screen */
@Composable
private fun MainTabRow(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    val shapes = ToggleButtonDefaults.shapes(checkedShape = CircleShape)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NotificationsType.entries.fastForEachIndexed { index, type ->
            ToggleButton(
                checked = pagerState.currentPage == index,
                onCheckedChange = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                shapes = shapes,
                colors = colors,
            ) {
                Text(text = stringResource(type.titleRes), letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun PrimaryTab(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
) {
    val coroutineScope = rememberCoroutineScope()
    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = modifier.fillMaxWidth(),
        indicator = {
            FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
        },
        containerColor = Color.Transparent // Use Toolbar color
    ) {
        NotificationsType.entries.fastForEachIndexed { index, type ->
            Tab(
                text = {
                    Text(text = stringResource(type.titleRes), letterSpacing = 0.75.sp)
                },
                selected = pagerState.currentPage == index,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                unselectedContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NotificationsToolBar(
    onBack: () -> Unit,
    onSearchClicked: () -> Unit,
    pagerState: PagerState,
    fromHome: Boolean,
    scrollBehavior: TopAppBarScrollBehavior?,
    canScrollBackward: () -> Boolean,
) {
    if (fromHome) {
        val navigationSuiteType = LocalNavSuiteScaffoldState.current.layoutType
        val compactTopAppBar = navigationSuiteType.isNavigationBar || isWindowHeightCompact()
        TopAppBarPaged(
            title = {
                if (compactTopAppBar) {
                    MainTabRow(pagerState = pagerState)
                } else {
                    Text(text = stringResource(R.string.title_notifications))
                }
            },
            actions = {
                ActionItem(
                    icon = Icons.Rounded.Search,
                    contentDescription = stringResource(id = R.string.title_search),
                    onClick = onSearchClicked,
                )
            },
            scrollBehavior = scrollBehavior,
            canScrollBackward = canScrollBackward,
        ) {
            if (!compactTopAppBar) {
                MainTabRow(
                    modifier = Modifier.padding(start = 14.dp, bottom = 8.dp),
                    pagerState = pagerState
                )
            }
        }
    } else {
        TopAppBarPaged(
            title = { Text(text = stringResource(R.string.title_notifications)) },
            titleHorizontalAlignment = Alignment.CenterHorizontally,
            navigationIcon = {
                BackNavigationIcon(onBackPressed = onBack)
            },
            scrollBehavior = scrollBehavior,
            canScrollBackward = canScrollBackward
        ) {
            PrimaryTab(pagerState = pagerState)
        }
    }
}