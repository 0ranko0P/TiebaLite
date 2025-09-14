package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.TabClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.Options
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.ForumSortType
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

const val TAB_FORUM_LATEST = 0
const val TAB_FORUM_GOOD = 1

private val TabSortTypes: Options<Int> by unsafeLazy {
    persistentMapOf(
        ForumSortType.BY_REPLY to R.string.title_sort_by_reply,
        ForumSortType.BY_SEND to R.string.title_sort_by_send
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumTab(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    sortType: Int,
    onSortTypeChanged: (sortType: Int, isGood: Boolean) -> Unit
) {
    val currentPage = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()

    val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tabTextStyle = MaterialTheme.typography.labelLarge.copy(
        letterSpacing = 2.sp
    )

    SecondaryTabRow(
        selectedTabIndex = currentPage,
        indicator = {
            FancyAnimatedIndicatorWithModifier(currentPage, verticalPadding = 6.dp)
        },
        divider = {},
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
    ) {
        TabClickMenu(
            selected = currentPage == TAB_FORUM_LATEST,
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(TAB_FORUM_LATEST)
                }
            },
            text = {
                Text(
                    text = stringResource(id = R.string.tab_forum_latest),
                    style = tabTextStyle
                )
            },
            menuContent = {
                ListPickerMenuItems(
                    items = TabSortTypes,
                    picked = sortType,
                    onItemPicked = {
                        onSortTypeChanged(it, currentPage == TAB_FORUM_GOOD)
                    }
                )
            },
            unselectedContentColor = unselectedContentColor
        )

        Tab(
            selected = currentPage == TAB_FORUM_GOOD,
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(TAB_FORUM_GOOD)
                }
            },
            unselectedContentColor = unselectedContentColor
        ) {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = R.string.tab_forum_good), style = tabTextStyle)
            }
        }
    }
}