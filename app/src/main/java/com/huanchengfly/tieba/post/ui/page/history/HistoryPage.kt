package com.huanchengfly.tieba.post.ui.page.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListPage
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.HistoryUtil
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun HistoryPage(navigator: NavController) {
    val tabs = persistentListOf(R.string.title_history_thread, R.string.title_history_forum)
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    val context = LocalContext.current

    MyScaffold(
        backgroundColor = Color.Transparent,
        scaffoldState = scaffoldState,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_history),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                elevation = Dp.Hairline,
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            HistoryUtil.deleteAll()
                            emitGlobalEvent(HistoryListUiEvent.DeleteAll)
                            launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    context.getString(
                                        R.string.toast_clear_success
                                    )
                                )
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.title_history_delete),
                            tint = ExtendedTheme.colors.onTopBar
                        )
                    }
                },
                content = {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        indicator = { tabPositions ->
                            PagerTabIndicator(pagerState = pagerState, tabPositions = tabPositions)
                        },
                        divider = {},
                        backgroundColor = Color.Transparent,
                        contentColor = ExtendedTheme.colors.primary,
                        modifier = Modifier
                            .width(100.dp * 2)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        tabs.fastForEachIndexed { i, stringRes ->
                            Tab(
                                text = {
                                    Text(text = stringResource(id = stringRes))
                                },
                                selected = pagerState.currentPage == i,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(i) }
                                },
                                unselectedContentColor = ExtendedTheme.colors.textSecondary
                            )
                        }
                    }
                }
            )
        }
    ) { contentPadding ->
        ProvideNavigator(navigator = navigator) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
                verticalAlignment = Alignment.Top,
                userScrollEnabled = true,
            ) {
                if (it == 0) {
                    HistoryListPage(type = HistoryUtil.TYPE_THREAD, contentPadding = contentPadding)
                } else {
                    HistoryListPage(type = HistoryUtil.TYPE_FORUM, contentPadding = contentPadding)
                }
            }
        }
    }
}