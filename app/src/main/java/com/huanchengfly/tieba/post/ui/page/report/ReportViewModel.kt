package com.huanchengfly.tieba.post.ui.page.report

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.ui.page.report.ReportViewModel.Companion.ReportVmFactory
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
@HiltViewModel(assistedFactory = ReportVmFactory::class)
class ReportViewModel @AssistedInject constructor(
    @Assisted val postId: Long,
    private val threadRepo: PbPageRepository,
): BaseStateViewModel<ReportUiState>() {

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

    companion object {
        @AssistedFactory
        interface ReportVmFactory{
            fun create(postId: Long): ReportViewModel
        }
    }
}