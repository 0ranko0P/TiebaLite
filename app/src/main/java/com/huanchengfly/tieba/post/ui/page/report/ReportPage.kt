package com.huanchengfly.tieba.post.ui.page.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.report.ReportViewModel.Companion.ReportVmFactory
import com.huanchengfly.tieba.post.ui.page.webview.WebViewPage
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ReportPage(
    postId: Long,
    onBack: () -> Unit,
    onNavigate: (Destination) -> Unit,
    viewModel: ReportViewModel = hiltViewModel<ReportViewModel, ReportVmFactory> { factory ->
        factory.create(postId)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StateScreen(
        isLoading = uiState.reportUrl == null,
        error = uiState.error,
        onReload = viewModel::onRefresh,
    ) {
        val reportUrl = uiState.reportUrl ?: return@StateScreen
        WebViewPage(initialUrl = reportUrl, customClient = true, onBack, onNavigate)
    }
}