package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.navigation.compose.hiltViewModel
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.workers.NewMessageWorker
import com.huanchengfly.tieba.post.workers.OKSignWorker
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkInfoPage(
    viewModel: WorkInfoViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val workers = remember {
        persistentListOf(OKSignWorker.TAG, NewMessageWorker.TAG)
    }
    val pagerState = rememberPagerState { workers.size }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = R.string.title_settings_worker,
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
                scrollBehavior = scrollBehavior
            ) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = {
                        FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
                    },
                    containerColor = Color.Transparent // Use Toolbar color
                ) {
                    workers.fastForEachIndexed { index, tag ->
                        Tab(
                            text = {
                                Text(text = tag, letterSpacing = 0.75.sp)
                            },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
            userScrollEnabled = false
        ) { i ->
            val contentModifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)

            when (workers[i]) {
                OKSignWorker.TAG -> OkSignWorkerContent(modifier = contentModifier, viewModel)

                NewMessageWorker.TAG -> NewMessageWorkerContent(modifier = contentModifier, viewModel)
            }
        }
    }
}

@Composable
private fun OkSignWorkerContent(modifier: Modifier = Modifier, vm: WorkInfoViewModel) {
    val periodic by vm.oKSignPeriodic.collectAsState()
    val expedited by vm.oKSignExpedited.collectAsState()

    Column(modifier = modifier) {
        WorkInfoItem(title = "Periodic", workInfoData = periodic)
        WorkInfoItem(title = "Expedited", workInfoData = expedited)
    }
}

@Composable
private fun NewMessageWorkerContent(modifier: Modifier = Modifier, vm: WorkInfoViewModel) {
    val periodic by vm.newMessagePeriodic.collectAsState()
    val oneShot by vm.newMessageOneShot.collectAsState()

    Column(modifier = modifier) {
        WorkInfoItem(title = "Periodic", workInfoData = periodic)
        WorkInfoItem(title = "OneShot", workInfoData = oneShot)
    }
}

@Composable
private fun WorkInfoItem(
    modifier: Modifier = Modifier,
    title: String,
    workInfoData: String
) {
    var collapse by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
    ) {
        StrongBox(
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            val rotate by animateFloatAsState(targetValue = if (collapse) 180f else 0f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )

                if (workInfoData.length > 20) {
                    IconButton(onClick = { collapse = !collapse }) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer { rotationZ = rotate }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = !collapse) {
            SelectionContainer {
                Text(
                    text = workInfoData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onNotNull(workInfoData) { horizontalScroll(rememberScrollState()) },
                    softWrap = false,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}