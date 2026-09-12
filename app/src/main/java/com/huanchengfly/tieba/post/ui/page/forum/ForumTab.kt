package com.huanchengfly.tieba.post.ui.page.forum

import android.util.SparseIntArray
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.Sort
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.util.getOrDefault
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.core.network.model.protos.FrsTabInfo
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.Options
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

private val FORUM_TAB_HEIGHT = 40.dp

@Composable
private fun ForumSortButton(
    modifier: Modifier = Modifier,
    currentSortType: () -> Int,
    onSortTypeChanged: (sortType: Int) -> Unit,
    enabled: Boolean = true,
) {
    ClickMenu(
        modifier = modifier,
        menuContent = {
            ListPickerMenuItems(
                items = TabSortTypes,
                picked = currentSortType(),
                onItemPicked = onSortTypeChanged
            )
        },
        triggerShape = MaterialTheme.shapes.small,
        enabled = enabled,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Sharp.Sort,
            contentDescription = null,
            modifier = Modifier.padding(ButtonDefaults.SmallContentPadding),
            tint = LocalContentColor.current.let { if (enabled) it else it.copy(alpha = 0.38f) }
        )
    }
}

@Composable
fun ForumTab(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    tabs: List<FrsTabInfo>,
    sortTypes: SparseIntArray,
    onSortTypeChanged: (sortType: Int) -> Unit,
) {
    val currentPage = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()
    val defaultSortType = LocalHabitSettings.current.forumSortType

    val tabsMovableContent = remember {
        movableContentOf<List<FrsTabInfo>> { tabList ->
            val tabTextStyle = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)

            tabList.fastForEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Box(
                        modifier = Modifier.height(FORUM_TAB_HEIGHT).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = tab.tabName, style = tabTextStyle)
                    }
                }
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ForumSortButton(
            currentSortType = {
                val currentTabId = tabs[pagerState.currentPage].tabId
                sortTypes.getOrDefault(currentTabId, defaultSortType)
            },
            onSortTypeChanged = onSortTypeChanged,
            enabled = remember { derivedStateOf { pagerState.currentPage != TAB_FORUM_GOOD } }.value
        )

        if (tabs.size > 2) {
            SecondaryScrollableTabRow(
                selectedTabIndex = currentPage,
                indicator = {
                    FancyAnimatedIndicatorWithModifier(index = currentPage, scrollable = true)
                },
                divider = {},
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1.0f),
                tabs = { tabsMovableContent(tabs) },
            )
        } else {
            SecondaryTabRow(
                selectedTabIndex = currentPage,
                indicator = {
                    FancyAnimatedIndicatorWithModifier(index = currentPage)
                },
                divider = {},
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1.0f),
                tabs = { tabsMovableContent(tabs) },
            )
        }
    }
}