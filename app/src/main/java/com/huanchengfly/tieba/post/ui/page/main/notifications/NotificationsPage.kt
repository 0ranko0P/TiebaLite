package com.huanchengfly.tieba.post.ui.page.main.notifications

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.Destination.Search
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.emptyBlurBottomNavigation
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.page.main.rememberTopAppBarScrollBehaviors
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurNavigationBarPlaceHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsPage(
    initialPage: NotificationsType = NotificationsType.ReplyMe,
    fromHome: Boolean = false,
    navigator: NavController = LocalNavController.current
) {
    val pages = NotificationsType.entries
    val pagerState = rememberPagerState(initialPage = initialPage.ordinal, pageCount = { pages.size })
    val scrollBehaviors = rememberTopAppBarScrollBehaviors(pages.size) {
        TopAppBarDefaults.pinnedScrollBehavior(state = it)
    }
    val coroutineScope = rememberCoroutineScope()

    MyScaffold(
        useMD2Layout = true,
        topBar = {
            NotificationsToolBar(
                navigator = navigator,
                fromHome = fromHome,
                scrollBehavior = { scrollBehaviors[pagerState.currentPage] }
            ) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = {
                        FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
                    },
                    containerColor = Color.Transparent // Use Toolbar color
                ) {
                    pages.fastForEachIndexed { index, type ->
                        val text = when (type) {
                            NotificationsType.ReplyMe -> R.string.title_reply_me

                            NotificationsType.AtMe -> R.string.title_at_me
                        }

                        Tab(
                            text = {
                                Text(text = stringResource(id = text), letterSpacing = 0.75.sp)
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
        },
        bottomBar = if (fromHome) emptyBlurBottomNavigation else BlurNavigationBarPlaceHolder,
    ) { contentPadding ->
        ProvideNavigator(navigator = navigator) {
            LazyLoadHorizontalPager(
                state = pagerState,
                key = { pages[it] }
            ) {
                NotificationsListPage(
                    modifier = Modifier.nestedScroll(scrollBehaviors[it].nestedScrollConnection),
                    type = NotificationsType.entries[it],
                    contentPadding = contentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsToolBar(
    navigator: NavController,
    fromHome: Boolean,
    scrollBehavior: () -> TopAppBarScrollBehavior?,
    content: (@Composable ColumnScope.() -> Unit)?
) {
    if (fromHome) {
        TopAppBar(
            titleRes = R.string.title_notifications,
            navigationIcon = accountNavIconIfCompact,
            actions = {
                ActionItem(
                    icon = Icons.Rounded.Search,
                    contentDescription = stringResource(id = R.string.title_search),
                    onClick = { navigator.navigate(Search) }
                )
            },
            scrollBehavior = scrollBehavior(),
            content = content
        )
    } else {
        CenterAlignedTopAppBar(
            titleRes = R.string.title_notifications,
            navigationIcon = {
                BackNavigationIcon(onBackPressed = navigator::navigateUp)
            },
            scrollBehavior = scrollBehavior(),
            content = content
        )
    }
}