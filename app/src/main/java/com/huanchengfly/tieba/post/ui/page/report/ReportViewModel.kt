package com.huanchengfly.tieba.post.ui.page.report

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Report Page
 */
@Immutable
data class ReportUiState(
    val reportUrl: String? = null,
    val error: Throwable? = null,
) : UiState {

    fun isLoading(): Boolean = reportUrl == null
}

@Stable
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val threadRepo: PbPageRepository,
    savedStateHandle: SavedStateHandle
): BaseStateViewModel<ReportUiState>() {

    val postId = savedStateHandle.toRoute<Destination.Report>().postId

    init {
        onRefresh()
    }

    override fun createInitialState(): ReportUiState = ReportUiState()

    fun onRefresh() {
        _uiState.set { createInitialState() }

        viewModelScope.launch {
            runCatching { threadRepo.loadReportPostURL(postId) }
                .onFailure { e ->
                    _uiState.update { ReportUiState(reportUrl = null, error = e) }
                }
                .onSuccess { reportUrl ->
                    _uiState.update { ReportUiState(reportUrl = reportUrl) }
                }
        }
    }
}