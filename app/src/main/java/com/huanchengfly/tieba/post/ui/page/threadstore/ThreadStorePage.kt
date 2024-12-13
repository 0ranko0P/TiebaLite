package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.pxToSp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_COLLECTED_DESC
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_COLLECTED_SEE_LZ

private val UpdateTipTextStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ThreadStorePage(
    navigator: NavController,
    viewModel: ThreadStoreViewModel = pageViewModel()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ThreadStoreUiIntent.Refresh)
        viewModel.initialized = true
    }
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::hasMore,
        initial = true
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::currentPage,
        initial = 0
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::data,
        initial = emptyList()
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::error,
        initial = null
    )
    val isError by remember { derivedStateOf { error != null } }

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    viewModel.onEvent<ThreadStoreUiEvent.Delete.Failure> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(R.string.delete_store_failure, it.errorMsg)
        )
    }
    viewModel.onEvent<ThreadStoreUiEvent.Delete.Success> {
        scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.delete_store_success))
    }
    MyScaffold(
        backgroundColor = Color.Transparent,
        scaffoldState = scaffoldState,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_my_collect),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        }
    ) { contentPaddings ->
        StateScreen(
            isEmpty = data.isEmpty(),
            isError = isError,
            isLoading = isRefreshing,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPaddings),
            onReload = {
                viewModel.send(ThreadStoreUiIntent.Refresh)
            },
            errorScreen = {
                error?.item?.let { ErrorScreen(error = it) }
            }
        ) {
            val pullRefreshState = rememberPullRefreshState(
                refreshing = isRefreshing,
                onRefresh = { viewModel.send(ThreadStoreUiIntent.Refresh) }
            )

            val lazyListState = rememberLazyListState()

            val dataStore = context.dataStore
            val seeLz by dataStore.collectPreferenceAsState(booleanPreferencesKey(KEY_COLLECTED_SEE_LZ), false)
            val descSort by dataStore.collectPreferenceAsState(booleanPreferencesKey(KEY_COLLECTED_DESC), false)

            Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
                SwipeUpLazyLoadColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    isLoading = isLoadingMore,
                    onLazyLoad = {
                        if (hasMore) viewModel.send(ThreadStoreUiIntent.LoadMore(currentPage + 1))
                    },
                    onLoad = null,
                    bottomIndicator = {
                        LoadMoreIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = isLoadingMore,
                            noMore = !hasMore,
                            onThreshold = false
                        )
                    }
                ) {
                    items(items = data, key = { it.threadId }) { info ->
                        StoreItem(
                            info = info,
                            onUserClick = {
                                info.author.lzUid?.let {
                                    navigator.navigate(UserProfile(it.toLong()))
                                }
                            },
                            onClick = {
                                navigator.navigate(
                                    Thread(
                                        threadId = info.threadId.toLong(),
                                        postId = info.markPid.toLong(),
                                        seeLz = seeLz,
                                        sortType = if(descSort) ThreadSortType.BY_DESC else ThreadSortType.DEFAULT,
                                        from = ThreadFrom.Store(
                                            maxPid = info.maxPid.toLong(),
                                            maxFloor = info.postNo.toInt()
                                        )
                                    )
                                )
                            },
                            onDelete = {
                                viewModel.send(ThreadStoreUiIntent.Delete(info.threadId))
                            }
                        )
                    }
                }

                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                    contentColor = ExtendedTheme.colors.primary,
                )
            }
        }
    }
}

@Composable
private fun StoreItem(
    info: ThreadStoreBean.ThreadStoreInfo,
    onUserClick: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUpdate = remember(info) { info.count != "0" && info.postNo != "0" }
    val isDeleted = remember(info) { info.isDeleted == "1" }
    val textMeasurer = rememberTextMeasurer()
    val textColor = ExtendedTheme.colors.text
    LongClickMenu(
        menuContent = {
            DropdownMenuItem(onClick = onDelete) {
                Text(text = stringResource(id = R.string.title_collect_on))
            }
        },
        onClick = onClick
    ) {
        Column(
            modifier = modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserHeader(
                portrait = info.author.userPortrait,
                name = info.author.name ?: "",
                nameShow = info.author.nameShow,
                onClick = onUserClick,
            )
            val title = remember(info, hasUpdate) {
                buildAnnotatedString {
                    append(info.title)
                    if (hasUpdate) {
                        append(" ")
                        appendInlineContent("Update", info.postNo)
                    }
                }
            }
            val updateTip = stringResource(
                id = R.string.tip_thread_store_update,
                info.postNo
            )
            val result = remember {
                textMeasurer.measure(
                    AnnotatedString(updateTip),
                    style = UpdateTipTextStyle
                ).size
            }
            val width = result.width.pxToSp() + 12F.dpToPxFloat().pxToSp() * 2 + 1
            val height = result.height.pxToSp() + 4F.dpToPxFloat().pxToSp() * 2
            Text(
                text = title,
                fontSize = 15.sp,
                color = if (isDeleted) textColor.copy(ContentAlpha.disabled) else textColor,
                inlineContent = mapOf(
                    "Update" to InlineTextContent(
                        placeholder = Placeholder(
                            width = width.sp,
                            height = height.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        ),
                        children = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = ExtendedTheme.colors.chip,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .padding(vertical = 4.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = updateTip,
                                    style = UpdateTipTextStyle,
                                    color = ExtendedTheme.colors.onChip,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    )
                )
            )
            if (isDeleted) {
                Text(
                    text = stringResource(id = R.string.tip_thread_store_deleted),
                    fontSize = 12.sp,
                    color = textColor.copy(ContentAlpha.disabled)
                )
            }
        }
    }
}