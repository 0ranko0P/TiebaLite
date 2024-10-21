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
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.BlockManager
import kotlinx.coroutines.launch

@Composable
fun BlockListPage(
    viewModel: BlockListViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val pagerState = rememberPagerState { 2 }
    val coroutineScope = rememberCoroutineScope()
    val category by remember { derivedStateOf {
        if (pagerState.currentPage == 0) Block.CATEGORY_BLACK_LIST else Block.CATEGORY_WHITE_LIST
    } }

    val dialogState = rememberDialogState()

    PromptDialog(
        onConfirm = {
            viewModel.addKeyword(category, it)
        },
        dialogState = dialogState,
        isError = BlockManager::hasKeyword,
        title = {
            Text(
                text = if (category == Block.CATEGORY_WHITE_LIST) stringResource(id = R.string.title_add_white)
                else stringResource(id = R.string.title_add_black)
            )
        }
    ) {
        Text(text = stringResource(id = R.string.tip_add_block))
    }

    val blackList by viewModel.blackList.collectAsState(null)
    val whiteList by viewModel.whiteList.collectAsState(null)

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_block_list),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = onBack)
                },
                content = {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        indicator = { tabPositions ->
                            PagerTabIndicator(
                                pagerState = pagerState,
                                tabPositions = tabPositions
                            )
                        },
                        divider = {},
                        backgroundColor = Color.Transparent,
                        contentColor = ExtendedTheme.colors.onTopBar,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(84.dp * 2),
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                            text = { Text(text = stringResource(id = R.string.title_black_list)) },
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                            text = { Text(text = stringResource(id = R.string.title_white_list)) },
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = dialogState::show
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(
                        id = if (category == Block.CATEGORY_BLACK_LIST) R.string.title_add_black else R.string.title_add_white
                    )
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
            val items by remember {
                derivedStateOf {
                    if (position == 0) blackList else whiteList
                }
            }
            StateScreen(
                isEmpty = items?.isEmpty() ?: true,
                isError = false,
                isLoading = items == null,
                modifier = Modifier.fillMaxSize()
            ) {
                MyLazyColumn(Modifier.fillMaxSize()) {
                    items(items!!, key = { it.id }) {
                        LongClickMenu(
                            menuContent = {
                                DropdownMenuItem(
                                    onClick = { viewModel.remove(it) },
                                    content = { Text(text = stringResource(R.string.title_delete)) }
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

@Composable
private fun BlockItem(
    item: Block,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.type == Block.TYPE_USER) Icons.Outlined.AccountCircle else ImageVector.vectorResource(
                id = R.drawable.ic_comment_new
            ),
            contentDescription = if (item.type == Block.TYPE_USER) stringResource(
                id = R.string.block_type_user
            ) else stringResource(
                id = R.string.block_type_keywords
            )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (item.type == Block.TYPE_USER) {
                Text(
                    text = "${item.username}",
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = "UID: ${item.uid}",
                    style = MaterialTheme.typography.caption
                )
            } else {
                Text(text = item.keyword!!, style = MaterialTheme.typography.subtitle1)
            }
        }
    }
}