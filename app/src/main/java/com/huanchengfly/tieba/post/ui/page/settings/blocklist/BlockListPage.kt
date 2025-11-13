package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.coroutines.launch

private sealed class BlockType(val title: Int, val contentDescription: Int) {
    object Blacklist: BlockType(R.string.title_black_list, R.string.title_add_black)

    object Whitelist: BlockType(R.string.title_white_list, R.string.title_add_white)
}

@Composable
fun UserBlockListPage(
    onBack: () -> Unit,
    viewModel: BlockListViewModel = hiltViewModel(),
) {
    val blackList by viewModel.userBlacklist.collectAsStateWithLifecycle()
    val whitelist by viewModel.userWhitelist.collectAsStateWithLifecycle()

    BlockListScaffold(
        title = R.string.settings_block_user,
        blackList = { blackList },
        whitelist = { whitelist },
        onBack = onBack,
        itemKeyProvider = { it.uid },
    ) { user ->
        LongClickMenu(
            menuContent = {
                TextMenuItem(text = R.string.title_delete, onClick = { viewModel.onDelete(user) })
            },
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            UserItem(user = user)
        }
    }
}

@Composable
private fun KeywordBlockDialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    blockType: BlockType? = null,
    isError: ((String) -> Boolean)? = null,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    if (blockType == null) return
    LaunchedEffect(blockType) {
        dialogState.show()
    }

    PromptDialog(
        onConfirm = onConfirm,
        modifier = modifier,
        dialogState = dialogState,
        isError = isError,
        onCancel = onCancel,
        title = {
            Text(text = stringResource(id = blockType.contentDescription))
        }
    ) {
       if (blockType == BlockType.Blacklist) {
           Text(text = stringResource(R.string.dialog_add_blocklist))
       } else {
           Text(text = stringResource(R.string.dialog_add_whitelist))
       }
    }
}

@Composable
fun KeywordBlockListPage(
    onBack: () -> Unit,
    viewModel: BlockListViewModel = hiltViewModel(),
) {
    var addKeywordType: BlockType? by remember { mutableStateOf(null) }
    KeywordBlockDialog(
        blockType = addKeywordType,
        isError = viewModel::hasKeyword,
        onConfirm = { keyword ->
            viewModel.addKeyword(keyword, whitelisted = addKeywordType == BlockType.Whitelist)
        },
        onCancel = { addKeywordType = null },
    )

    val blackList by viewModel.getBlacklist().collectAsStateWithLifecycle()
    val whitelist by viewModel.getWhitelist().collectAsStateWithLifecycle()

    BlockListScaffold(
        blackList = { blackList },
        whitelist = { whitelist },
        onAddClicked = { addKeywordType = it },
        onBack = onBack,
        itemKeyProvider = { it },
    ) { keyword ->
        LongClickMenu(
            menuContent = {
                TextMenuItem(text = R.string.title_delete, onClick = { viewModel.onDelete(keyword) })
            },
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            KeywordItem(keyword = keyword)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> BlockListScaffold(
    title: Int = R.string.settings_block_keyword,
    blackList: () -> List<T>?,
    whitelist: () -> List<T>?,
    onAddClicked: ((type: BlockType) -> Unit)? = null,
    onBack: () -> Unit = {},
    itemKeyProvider: (item: T) -> Any = { it.toString() },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val pages = remember { listOf(BlockType.Blacklist, BlockType.Whitelist) }
    val pagerState = rememberPagerState { pages.size }
    val pagerMovableContent = remember {
        pages.map { page ->
            movableContentOf<PaddingValues> { contentPadding ->
                val items = if (page == BlockType.Blacklist) blackList() else whitelist()
                StateScreen(
                    isEmpty = items.isNullOrEmpty(),
                    isError = false,
                    isLoading = items == null,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding
                    ) {
                        items(items ?: emptyList(), key = itemKeyProvider, itemContent = itemContent)
                    }
                }
            }
        }
    }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = title,
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = onBack)
                },
                scrollBehavior = scrollBehavior
            ) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = {
                        FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
                    },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    pages.fastForEachIndexed { i, page ->
                        Tab(
                            selected = pagerState.currentPage == i,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(i) }
                            },
                            text = { Text(text = stringResource(id = page.title)) },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (onAddClicked == null) return@MyScaffold

            FloatingActionButton(
                onClick = { onAddClicked(pages[pagerState.currentPage]) }
            ) {
                val description = stringResource(pages[pagerState.currentPage].contentDescription)
                Icon(imageVector = Icons.Filled.Add, contentDescription = description)
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it },
            verticalAlignment = Alignment.Top
        ) { index ->
            pagerMovableContent[index](contentPadding)
        }
    }
}

@Composable
private fun UserItem(modifier: Modifier = Modifier, user: BlockUser) {
    val uidText = remember { "UID: " + user.uid.toString() }
    Row(
        modifier = modifier
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = user.name ?: uidText
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Outlined.AccountCircle, contentDescription = null)

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!user.name.isNullOrEmpty()) {
                Text(text = user.name, style = MaterialTheme.typography.titleMedium)
            }

            Text(text = uidText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun KeywordItem(modifier: Modifier = Modifier, keyword: String) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = keyword
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ImageVector.vectorResource(id = R.drawable.ic_comment_new), contentDescription = null)

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = keyword,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Preview("BlockListScaffold Keyword")
@Composable
private fun BlockListScaffoldKeywordPreview() = TiebaLiteTheme {
    val blackList = (0..10).map { "Test keyword: $it" }
    BlockListScaffold(
        blackList = { blackList },
        whitelist = { emptyList() },
        onAddClicked = {},
        itemContent = { KeywordItem(keyword = it) }
    )
}
@Preview("BlockListScaffold User")
@Composable
private fun BlockListScaffoldUserPreview() = TiebaLiteTheme {
    val blackList = (0..10L).map { BlockUser(uid = it, name = "User: $it", whitelisted = false) }
    BlockListScaffold(
        blackList = { blackList },
        whitelist = { emptyList() },
        itemKeyProvider = { it.uid },
        itemContent = { UserItem(user = it) }
    )
}
