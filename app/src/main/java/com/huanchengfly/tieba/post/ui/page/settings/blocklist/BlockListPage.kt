package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.icons.RegularExpression
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingActionButtonMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingActionButtonMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ToggleFloatingActionButton
import com.huanchengfly.tieba.post.ui.widgets.compose.ToggleFloatingActionButtonDefaults.animateIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.coroutines.launch
import kotlin.random.Random

private sealed class BlockType(val title: Int, val contentDescription: Int) {
    object Blacklist: BlockType(R.string.title_black_list, R.string.title_add_black)

    object Whitelist: BlockType(R.string.title_white_list, R.string.title_add_white)
}

private class BlockKeywordOption(val isRegex: Boolean, val isWhitelisted: Boolean)

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
    option: BlockKeywordOption? = null,
    isError: ((String) -> Boolean)? = null,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    if (option == null) return
    LaunchedEffect(option) {
        dialogState.show()
    }

    PromptDialog(
        onConfirm = onConfirm,
        modifier = modifier,
        dialogState = dialogState,
        isError = isError,
        onCancel = onCancel,
        title = {
            val blockType = if (option.isWhitelisted) BlockType.Whitelist else BlockType.Blacklist
            Text(text = stringResource(id = blockType.contentDescription))
        }
    ) {
        val dialogContent = when {
            option.isWhitelisted && option.isRegex -> R.string.dialog_add_whitelist_regex
            option.isWhitelisted && !option.isRegex -> R.string.dialog_add_whitelist
            !option.isWhitelisted && option.isRegex -> R.string.dialog_add_blocklist_regex
            else -> R.string.dialog_add_blocklist
        }
        Text(text = stringResource(dialogContent))
    }
}

@Composable
fun KeywordBlockListPage(
    onBack: () -> Unit,
    viewModel: BlockListViewModel = hiltViewModel(),
) {
    val (addKeywordOpt, setKeywordOpt) = remember { mutableStateOf<BlockKeywordOption?>(null) }

    KeywordBlockDialog(
        option = addKeywordOpt,
        isError = {
            viewModel.isKeywordInvalid(keyword = it.trim(), isRegex = addKeywordOpt!!.isRegex)
        },
        onConfirm = { keyword ->
            addKeywordOpt?.apply { viewModel.addKeyword(keyword, isRegex, isWhitelisted) }
        },
        onCancel = { setKeywordOpt(null) },
    )

    val blackList by viewModel.keywordBlacklist.collectAsStateWithLifecycle()
    val whitelist by viewModel.keywordWhitelist.collectAsStateWithLifecycle()

    BlockListScaffold(
        blackList = { blackList },
        whitelist = { whitelist },
        onAddClicked = setKeywordOpt,
        onBack = onBack,
        itemKeyProvider = { it.keyword },
    ) { item ->
        LongClickMenu(
            menuContent = {
                TextMenuItem(text = R.string.title_delete, onClick = { viewModel.onDelete(item) })
            },
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            KeywordItem(keyword = item.keyword, isRegex = item.isRegex)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> BlockListScaffold(
    title: Int = R.string.settings_block_keyword,
    blackList: () -> List<T>?,
    whitelist: () -> List<T>?,
    onAddClicked: ((BlockKeywordOption) -> Unit)? = null,
    onBack: () -> Unit = {},
    itemKeyProvider: (item: T) -> Any = { it.toString() },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollOrientationConnection = rememberScrollOrientationConnection()

    val pages = remember { listOf(BlockType.Blacklist, BlockType.Whitelist) }
    val pagerState = rememberPagerState { pages.size }
    val pagerMovableContent = remember {
        pages.map { page ->
            movableContentOf<PaddingValues> { contentPadding ->
                val items = if (page == BlockType.Blacklist) blackList() else whitelist()
                StateScreen(
                    modifier = Modifier.nestedScroll(connection = scrollOrientationConnection),
                    isEmpty = items.isNullOrEmpty(),
                    isError = false,
                    isLoading = items == null,
                    screenPadding = contentPadding,
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

            BlockFloatingActionButtonMenu(
                description = pages[pagerState.currentPage].contentDescription,
                visibleState = {
                    !pagerState.isScrolling && scrollOrientationConnection.isScrollingForward
                },
                onAdd = { isRegex ->
                    val isWhitelisted = pages[pagerState.currentPage] === BlockType.Whitelist
                    onAddClicked(BlockKeywordOption(isRegex, isWhitelisted))
                }
            )
        },
        backgroundColor = MaterialTheme.colorScheme.background // Higher contrast on translucent theme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockFloatingActionButtonMenu(
    modifier: Modifier = Modifier,
    description: Int,
    visibleState: () -> Boolean,
    onAdd: (isRegex: Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val items = remember {
        listOf(
            Icons.AutoMirrored.Rounded.Notes to context.getString(R.string.button_add_keyword),
            Icons.Rounded.RegularExpression to context.getString(R.string.button_add_regex)
        )
    }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    val visible = visibleState()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { it },
        exit = fadeOut() + slideOutHorizontally { it }
    ) {
        FloatingActionButtonMenu(
            modifier = modifier,
            expanded = fabMenuExpanded,
            button = {
                ToggleFloatingActionButton(
                    modifier =
                        Modifier
                            .semantics {
                                traversalIndex = -1f
                                contentDescription = context.getString(description)
                            }
                            .focusRequester(focusRequester),
                    checked = fabMenuExpanded,
                    onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                ) {
                    val imageVector by remember {
                        derivedStateOf {
                            if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                        }
                    }
                    Icon(
                        painter = rememberVectorPainter(imageVector),
                        contentDescription = null,
                        modifier = Modifier.animateIcon({ checkedProgress }),
                    )
                }
            },
        ) {
            items.forEachIndexed { i, (icon, menuText) ->
                val isRegex = i != 0
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        onAdd(isRegex)
                    },
                    icon = { Icon(imageVector = icon, contentDescription = null) },
                    text = { Text(text = menuText) },
                )
            }
        }
    }

    if (!visible && fabMenuExpanded) {
        LaunchedEffect(Unit) { fabMenuExpanded = false }
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
private fun KeywordItem(modifier: Modifier = Modifier, keyword: String, isRegex: Boolean) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = keyword
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRegex) {
            Icon(imageVector = Icons.Rounded.RegularExpression, contentDescription = null)
        } else {
            Icon(imageVector = Icons.AutoMirrored.Rounded.Notes, contentDescription = null)
        }

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
        itemContent = { KeywordItem(keyword = it, isRegex = Random.nextBoolean()) }
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
