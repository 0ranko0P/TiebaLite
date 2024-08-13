package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.ScrollableTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TabClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.ForumSortType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

const val TAB_FORUM_LATEST = 0
const val TAB_FORUM_GOOD = 1

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ForumTab(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    sortType: Int,
    onSortTypeChanged: (Int, Boolean) -> Unit
) {
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val coroutineScope = rememberCoroutineScope()

    val tabTextStyle = MaterialTheme.typography.button.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 2.sp
    )

    ScrollableTabRow(
        selectedTabIndex = currentPage,
        indicator = { tabPositions ->
            PagerTabIndicator(
                pagerState = pagerState,
                tabPositions = tabPositions
            )
        },
        divider = {},
        backgroundColor = Color.Transparent,
        contentColor = ExtendedTheme.colors.primary,
        edgePadding = 0.dp,
        modifier = modifier.wrapContentWidth(align = Alignment.Start)
    ) {
        TabClickMenu(
            selected = currentPage == TAB_FORUM_LATEST,
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(TAB_FORUM_GOOD)
                }
            },
            text = {
                Text(
                    text = stringResource(id = R.string.tab_forum_latest),
                    style = tabTextStyle
                )
            },
            menuContent = {
                ListSinglePicker(
                    itemTitles = persistentListOf(
                        stringResource(id = R.string.title_sort_by_reply),
                        stringResource(id = R.string.title_sort_by_send)
                    ),
                    itemValues = persistentListOf(
                        ForumSortType.BY_REPLY,
                        ForumSortType.BY_SEND
                    ),
                    selectedPosition = sortType,
                    onItemSelected = { _, _, value, changed ->
                        if (changed) {
                            onSortTypeChanged(value, currentPage == TAB_FORUM_GOOD)
                        }
                        dismiss()
                    }
                )
            },
            selectedContentColor = ExtendedTheme.colors.onTopBar,
            unselectedContentColor = ExtendedTheme.colors.onTopBarSecondary
        )
        Tab(
            selected = currentPage == TAB_FORUM_GOOD,
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(TAB_FORUM_GOOD)
                }
            },
            selectedContentColor = ExtendedTheme.colors.onTopBar,
            unselectedContentColor = ExtendedTheme.colors.onTopBarSecondary
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.tab_forum_good),
                    style = tabTextStyle
                )
            }
        }
    }
}