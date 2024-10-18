package com.huanchengfly.tieba.post.ui.page.main.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.Destination.Search
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import kotlinx.coroutines.launch

@Composable
fun NotificationsPage(
    fromHome: Boolean = false,
    navigator: NavController = LocalNavController.current
) {
    val pages = listOf<Pair<Int, (@Composable () -> Unit)>>(
        R.string.title_reply_me to @Composable {
            NotificationsListPage(type = NotificationsType.ReplyMe)
        },
        R.string.title_at_me to @Composable {
            NotificationsListPage(type = NotificationsType.AtMe)
        }
    )
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Column {
                NotificationsToolBar(navigator = navigator, fromHome = fromHome)

                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = { tabPositions ->
                        PagerTabIndicator(pagerState, tabPositions, tabWidth = 36.dp)
                    },
                    divider = {},
                    backgroundColor = ExtendedTheme.colors.topBar,
                    contentColor = ExtendedTheme.colors.onTopBar,
                ) {
                    pages.fastForEachIndexed { index, pair ->
                        Tab(
                            text = { Text(text = stringResource(id = pair.first)) },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyLoadHorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            userScrollEnabled = true,
            key = { pages[it].first }
        ) {
            ProvideNavigator(navigator = navigator) {
                pages[it].second()
            }
        }
    }
}

@Composable
private fun NotificationsToolBar(navigator: NavController, fromHome: Boolean) {
    if (fromHome) {
        Toolbar(
            title = stringResource(id = R.string.title_notifications),
            navigationIcon = accountNavIconIfCompact(),
            actions = {
                ActionItem(
                    icon = Icons.Rounded.Search,
                    contentDescription = stringResource(id = R.string.title_search),
                    onClick = { navigator.navigate(Search) }
                )
            }
        )
    } else {
        TitleCentredToolbar(
            title = {
                Text(stringResource(id = R.string.title_notifications))
            },
            navigationIcon = {
                BackNavigationIcon(onBackPressed = navigator::navigateUp)
            }
        )
    }
}