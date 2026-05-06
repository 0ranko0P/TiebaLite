package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.models.database.BlockKeyword
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.FadedVisibility
import com.huanchengfly.tieba.post.ui.icons.RegularExpression
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultToggleFloatingActionButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DeleteIconButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.ExtendedFabHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PlainTooltipBox
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedListItemColors
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.coroutines.launch
import kotlin.random.Random

private sealed class BlockType(val title: Int, val contentDescription: Int) {
    object Blacklist: BlockType(R.string.title_black_list, R.string.title_add_black)

    object Whitelist: BlockType(R.string.title_white_list, R.string.title_add_white)
}

private class BlockKeywordOption(val isRegex: Boolean, val isWhitelisted: Boolean)

// FAB Menu has extra padding, offset FAB to make them aligned
// See FloatingActionButtonMenu.FabMenuButtonPaddingBottom
private fun Modifier.fabMenuOffset(): Modifier = this then Modifier.offset(x = -(16).dp, y = -(16).dp)

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
private fun ForumBlockDialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    type: BlockType? = null,
    isError: ((String) -> Boolean)? = null,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    if (type == null) return
    LaunchedEffect(type) {
        dialogState.show()
    }

    PromptDialog(
        onConfirm = onConfirm,
        modifier = modifier,
        dialogState = dialogState,
        onValueChange = { new, _ -> !new.contains(' ') },
        isError = isError,
        onCancel = onCancel,
        title = { Text(text = stringResource(id = type.contentDescription)) }
    ) {
        Text(text = stringResource(id = R.string.dialog_add_blocklist_forum))
    }
}

@Composable
fun ForumBlockListPage(
    onBack: () -> Unit,
    viewModel: ForumBlockListViewModel = hiltViewModel(),
) {
    val blackList by viewModel.blackList.collectAsStateWithLifecycle()
    val isUpdating by viewModel.updating.collectAsStateWithLifecycle()
    val (addBlockForum, setBlockForum) = remember { mutableStateOf<BlockType?>(null) }

    if (addBlockForum != null) {
        ForumBlockDialog(
            type = addBlockForum,
            isError = viewModel::isForumInvalid,
            onConfirm = viewModel::upsert,
            onCancel = { setBlockForum(null) },
        )
    }

    BlockListScaffold(
        title = R.string.settings_block_forum,
        pages = listOf(BlockType.Blacklist),
        blackList = { blackList },
        whitelist = { null },
        onBack = onBack,
        onSelectItems = viewModel::delete,
        isUpdating = { isUpdating },
        itemKeyProvider = { _, keyword -> keyword },
        floatingActionButton = { visible, blockType ->
            PlainTooltipBox(
                contentDescription = stringResource(blockType.contentDescription),
            ) {
                FloatingActionButton(
                    modifier = Modifier
                        .fabMenuOffset()
                        .animateFloatingActionButton(visible, alignment = Alignment.Center),
                    onClick = {
                        setBlockForum(blockType) // Launch PromptDialog
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                }
            }
        }
    ) { forumName ->
        ForumItem(forumName = forumName)
    }
}

@Composable
fun KeywordBlockListPage(
    onBack: () -> Unit,
    viewModel: KeywordBlockListViewModel = hiltViewModel(),
) {
    val (addKeywordOpt, setKeywordOpt) = remember { mutableStateOf<BlockKeywordOption?>(null) }

    KeywordBlockDialog(
        option = addKeywordOpt,
        isError = {
            viewModel.isInvalid(keyword = it.trim(), isRegex = addKeywordOpt!!.isRegex)
        },
        onConfirm = { keyword ->
            addKeywordOpt?.apply {
                viewModel.upsert(BlockKeyword(-1, keyword, isRegex, isWhitelisted))
            }
        },
        onCancel = { setKeywordOpt(null) },
    )

    val blackList by viewModel.blackList.collectAsStateWithLifecycle()
    val whitelist by viewModel.whiteList.collectAsStateWithLifecycle()
    val isUpdating by viewModel.updating.collectAsStateWithLifecycle()

    BlockListScaffold(
        blackList = { blackList },
        whitelist = { whitelist },
        onBack = onBack,
        onSelectItems = viewModel::delete,
        isUpdating = { isUpdating },
        floatingActionButton = { visible, blockType ->
            BlockFloatingActionButtonMenu(visible = visible) { isRegex ->
                val isWhitelisted = blockType === BlockType.Whitelist
                setKeywordOpt(BlockKeywordOption(isRegex, isWhitelisted))
            }
        },
        itemKeyProvider = { _, item -> item.id },
    ) { item ->
        KeywordItem(keyword = item.keyword, isRegex = item.isRegex)
    }
}

@Composable
fun UserBlockListPage(
    onBack: () -> Unit,
    viewModel: UserBlockListViewModel = hiltViewModel(),
) {
    val blackList by viewModel.blackList.collectAsStateWithLifecycle()
    val whitelist by viewModel.whiteList.collectAsStateWithLifecycle()
    val isUpdating by viewModel.updating.collectAsStateWithLifecycle()

    BlockListScaffold(
        title = R.string.settings_block_user,
        blackList = { blackList },
        whitelist = { whitelist },
        onBack = onBack,
        onSelectItems = viewModel::delete,
        isUpdating = { isUpdating },
        itemKeyProvider = { _, item -> item.uid },
    ) {
        UserItem(user = it)
    }
}

@Composable
private fun <T> BlockListScaffold(
    title: Int = R.string.settings_block_keyword,
    pages: List<BlockType> = remember { listOf(BlockType.Blacklist, BlockType.Whitelist) },
    blackList: () -> List<T>?,
    whitelist: () -> List<T>?,
    onBack: () -> Unit = {},
    onSelectItems: (List<T>) -> Unit = {},
    isUpdating: () -> Boolean = { false },
    floatingActionButton: (@Composable (visible: Boolean, BlockType) -> Unit)? = null,
    itemKeyProvider: (Int, item: T) -> Any = { _, it -> it.toString() },
    itemContent: @Composable (item: T) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val pagerState = rememberPagerState { pages.size }

    val listItemColors = SegmentedListItemColors
    val listItemElevation = ListItemElevation(Dp.Hairline, Dp.Hairline)
    val listContentPadding = PaddingValues(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 48.dp + ExtendedFabHeight)

    val selectedItems = remember { mutableStateSetOf<T>() }
    var selectMode by remember { mutableStateOf(false) }
    BackHandler(enabled = selectMode) {
        selectMode = false
        selectedItems.clear()
    }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = title,
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = onBack)
                },
                actions = {
                    FadedVisibility(visible = selectMode || isUpdating()) {
                        DeleteIconButton(deleting = isUpdating(), enabled = selectedItems.isNotEmpty()) {
                            selectMode = false
                            onSelectItems(selectedItems.toList())
                            selectedItems.clear()
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            ) {
                if (pagerState.pageCount <= 1) return@CenterAlignedTopAppBar
                AnimatedVisibility(visible = !selectMode) {
                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        indicator = {
                            FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
                        },
                        containerColor = Color.Transparent,
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
            }
        },
        floatingActionButton = {
            if (floatingActionButton == null) return@MyScaffold

            val fabVisibleState by remember {
                derivedStateOf { !isUpdating() && !selectMode && !pagerState.isScrolling }
            }
            floatingActionButton(fabVisibleState, pages[pagerState.currentPage])
        },
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState,
            key = { it },
            userScrollEnabled = !selectMode
        ) { index ->
            val items = if (pages[index] == BlockType.Blacklist) blackList() else whitelist()
            StateScreen(
                isEmpty = items.isNullOrEmpty(),
                isError = false,
                isLoading = items == null,
                screenPadding = contentPadding,
            ) {
                if (items == null) return@StateScreen
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding + listContentPadding,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    itemsIndexed(items, key = itemKeyProvider) { i, item ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val selected = selectedItems.contains(item)
                        SegmentedListItem(
                            selected = selected,
                            onClick = {
                                if (selectMode) {
                                    if (selected) selectedItems -= item else selectedItems += item
                                }
                            },
                            shapes = ListItemDefaults.segmentedShapes(i, count = items.size),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .animateItem(),
                            trailingContent = {
                                if (selectMode) {
                                    Checkbox(selected, onCheckedChange = null, interactionSource = interactionSource)
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            onLongClick = {
                                if (!isUpdating() && !selectMode) {
                                    selectedItems += item
                                    selectMode = true
                                }
                            },
                            colors = listItemColors,
                            elevation = listItemElevation,
                            interactionSource = interactionSource,
                            content = { itemContent(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockFloatingActionButtonMenu(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onAdd: (isRegex: Boolean) -> Unit
) {
    val context = LocalContext.current
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val items = remember {
        listOf(
            Icons.AutoMirrored.Rounded.Notes to context.getString(R.string.button_add_keyword),
            Icons.Rounded.RegularExpression to context.getString(R.string.button_add_regex)
        )
    }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { it },
        exit = fadeOut() + slideOutHorizontally { it }
    ) {
        FloatingActionButtonMenu(
            modifier = modifier,
            expanded = fabMenuExpanded,
            button = {
                DefaultToggleFloatingActionButton(
                    checked = fabMenuExpanded,
                    onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                )
            },
        ) {
            items.fastForEachIndexed { i, (icon, menuText) ->
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

@NonRestartableComposable
@Composable
private fun BaseBlockRuleItem(
    modifier: Modifier = Modifier,
    contentDescription: String,
    icon: @Composable RowScope.() -> Unit,
    content: @Composable RowScope.() -> Unit = { Text(text = contentDescription) },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()

        ProvideTextStyle(MaterialTheme.typography.titleMedium) {
            content()
        }
    }
}

@Composable
private fun ForumItem(modifier: Modifier = Modifier, forumName: String) {
    BaseBlockRuleItem(
        modifier = modifier,
        contentDescription = forumName,
        icon = {
            Icon(imageVector = Icons.Outlined.Forum, contentDescription = null)
        },
    )
}

@Composable
private fun UserItem(modifier: Modifier = Modifier, user: BlockUser) {
    val uidText = remember { "UID: " + user.uid }
    BaseBlockRuleItem(
        modifier = modifier,
        contentDescription = user.name ?: uidText,
        icon = {
            Icon(imageVector = Icons.Outlined.AccountCircle, contentDescription = null)
        },
        content = {
            Column {
                if (!user.name.isNullOrEmpty()) {
                    Text(text = user.name)
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(text = uidText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}

@Composable
private fun KeywordItem(modifier: Modifier = Modifier, keyword: String, isRegex: Boolean) {
    BaseBlockRuleItem(
        modifier = modifier,
        contentDescription = keyword,
        icon = {
            if (isRegex) {
                Icon(imageVector = Icons.Rounded.RegularExpression, contentDescription = null)
            } else {
                Icon(imageVector = Icons.AutoMirrored.Rounded.Notes, contentDescription = null)
            }
        },
    )
}

@Preview("BlockListScaffold Keyword")
@Composable
private fun BlockListScaffoldKeywordPreview() = TiebaLiteTheme {
    val blackList = (0..10).map { "Test keyword: $it" }
    BlockListScaffold(
        blackList = { blackList },
        whitelist = { emptyList() },
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
        itemContent = { UserItem(user = it) }
    )
}
