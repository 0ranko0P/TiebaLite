package com.huanchengfly.tieba.post.ui.page.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.ui.page.webview.WebViewPage
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ReportPage(
    navigator: NavController,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StateScreen(
        isLoading = uiState.reportUrl == null,
        error = uiState.error,
        onReload = viewModel::onRefresh,
    ) {
        val reportUrl = uiState.reportUrl ?: return@StateScreen
        WebViewPage(initialUrl = reportUrl, customClient = true, navigator)
    }
}