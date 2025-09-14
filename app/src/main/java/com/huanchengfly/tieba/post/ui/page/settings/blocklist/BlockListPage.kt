package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.BlockManager
import kotlinx.coroutines.launch

// Type: Category(enum), Title(resId), TabTitle(resId)
private typealias BlockPage = Triple<Int, Int, Int>

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockListPage(
    viewModel: BlockListViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val pages = remember {
        listOf(
            BlockPage(Block.CATEGORY_BLACK_LIST, R.string.title_add_black, R.string.title_black_list),
            BlockPage(Block.CATEGORY_WHITE_LIST, R.string.title_add_white, R.string.title_white_list)
        )
    }
    val pagerState = rememberPagerState { pages.size }

    val dialogState = rememberDialogState()
    PromptDialog(
        onConfirm = {
            val currentPage = pages[pagerState.currentPage]
            viewModel.addKeyword(category = currentPage.first, keyword = it)
        },
        dialogState = dialogState,
        isError = BlockManager::hasKeyword,
        title = { Text(text = stringResource(id = pages[pagerState.currentPage].second)) }
    ) {
        Text(text = stringResource(id = R.string.tip_add_block))
    }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = R.string.title_block_list,
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = onBack)
                },
            ) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = {
                        FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
                    },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    pages.fastForEachIndexed { i, (_, _, tabTitle) ->
                        Tab(
                            selected = pagerState.currentPage == i,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(i) }
                            },
                            text = { Text(text = stringResource(id = tabTitle)) },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = dialogState::show) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = pages[pagerState.currentPage].second)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it },
            contentPadding = paddingValues,
            verticalAlignment = Alignment.Top
        ) { position ->
            val items by when(pages[position].first) {
                Block.CATEGORY_BLACK_LIST -> viewModel.blackList.collectAsStateWithLifecycle()
                Block.CATEGORY_WHITE_LIST -> viewModel.whiteList.collectAsStateWithLifecycle()
                else -> throw RuntimeException()
            }

            val isEmpty by remember {
                derivedStateOf { items.isNullOrEmpty() }
            }

            val isLoading by remember {
                derivedStateOf { items == null }
            }

            StateScreen(
                isEmpty = isEmpty,
                isError = false,
                isLoading = isLoading,
                modifier = Modifier.fillMaxSize()
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                    MyLazyColumn(Modifier.fillMaxSize()) {
                        items(items!!, key = { it.id }) {
                            LongClickMenu(
                                menuContent = {
                                    TextMenuItem(
                                        text = R.string.title_delete,
                                        onClick = { viewModel.remove(it) }
                                    )
                                }
                            ) {
                                BlockItem(item = it)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockItem(
    item: Block,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.type == Block.TYPE_USER) {
                Icons.Outlined.AccountCircle
            } else {
                ImageVector.vectorResource(id = R.drawable.ic_comment_new)
            },
            contentDescription = if (item.type == Block.TYPE_USER) {
                stringResource(id = R.string.block_type_user)
            } else {
                stringResource(id = R.string.block_type_keywords)
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (item.type == Block.TYPE_USER) {
                Text(text = item.username.orEmpty())
                Text(
                    text = "UID: ${item.uid}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(text = item.keyword!!)
            }
        }
    }
}

@Preview("BlockItem")
@Composable
private fun BlockItemPreview() = TiebaLiteTheme {
    ProvideTextStyle(MaterialTheme.typography.titleMedium) {
        Column {
            BlockItem(Block(type = Block.TYPE_USER, username = "Test", uid = Long.MAX_VALUE))
            BlockItem(Block(type = Block.TYPE_USER, username = "Test User", uid = 0))

            HorizontalDivider()

            BlockItem(Block(type = Block.TYPE_KEYWORD, keyword = "keyword"))
            BlockItem(Block(type = Block.TYPE_KEYWORD, keyword = "null"))
        }
    }
}