package com.huanchengfly.tieba.post.ui.page.main.notifications

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.Destination.Search
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.emptyBlurBottomNavigation
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurNavigationBarPlaceHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
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
    val pages = NotificationsType.entries
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    BlurScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            NotificationsToolBar(navigator = navigator, fromHome = fromHome) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = { tabPositions ->
                        PagerTabIndicator(pagerState, tabPositions, tabWidth = 36.dp)
                    },
                    divider = {},
                    backgroundColor = Color.Transparent, // Use Toolbar color
                    contentColor = ExtendedTheme.colors.primary,
                ) {
                    pages.fastForEachIndexed { index, type ->
                        val text = when(type) {
                            NotificationsType.ReplyMe -> R.string.title_reply_me

                            NotificationsType.AtMe -> R.string.title_at_me
                        }

                        Tab(
                            text = { Text(text = stringResource(id = text)) },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            unselectedContentColor = ExtendedTheme.colors.textSecondary
                        )
                    }
                }
            }
        },
        bottomBar = if (fromHome) emptyBlurBottomNavigation else BlurNavigationBarPlaceHolder,
    ) { contentPadding ->
        LazyLoadHorizontalPager(
            state = pagerState,
            key = { pages[it] }
        ) {
            ProvideNavigator(navigator = navigator) {
                NotificationsListPage(type = NotificationsType.entries[it], contentPadding)
            }
        }
    }
}

@Composable
private fun NotificationsToolBar(
    navigator: NavController,
    fromHome: Boolean,
    content: (@Composable ColumnScope.() -> Unit)?
) {
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
            },
            elevation = Dp.Hairline,
            content = content
        )
    } else {
        TitleCentredToolbar(
            title = {
                Text(stringResource(id = R.string.title_notifications))
            },
            elevation = Dp.Hairline,
            navigationIcon = {
                BackNavigationIcon(onBackPressed = navigator::navigateUp)
            },
            content = content
        )
    }
}