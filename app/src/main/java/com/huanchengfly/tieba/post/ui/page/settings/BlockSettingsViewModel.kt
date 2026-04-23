package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.TransactionRunner
import com.huanchengfly.tieba.post.ui.models.settings.BlockBackupMetadata
import com.huanchengfly.tieba.post.utils.BlockRuleBackupUtil
import com.huanchengfly.tieba.post.utils.RestoreOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

data class BlockSettingsUiState(
    val loading: Boolean = false,
    val pendingRestore: Pair<BlockBackupMetadata, Uri>? = null,
    val error: Throwable? = null,
)

sealed interface BlockSettingsUiEvent: UiEvent {
    object BadBackup: BlockSettingsUiEvent

    class BackupFailed(val message: String): BlockSettingsUiEvent

    object BackupCompleted: BlockSettingsUiEvent
}

private const val TAG = "BlockSettingsViewModel"

@HiltViewModel
class BlockSettingsViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val blockDao: BlockDao,
    private val transactionRunner: TransactionRunner,
): BaseStateViewModel<BlockSettingsUiState>() {

    override fun createInitialState() = BlockSettingsUiState()

    fun onBackup(uri: Uri, timestamp: Long) {
        if (currentState.loading) return else _uiState.update { it.copy(loading = true) }
        launchInVM {
            val start = Clock.System.now()
            runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { out ->
                    BlockRuleBackupUtil.backup(blockDao, transactionRunner, timestamp, out)
                }
            }
            .onFailure { e ->
                Log.e(TAG, "onBackup", e)
                emitUiEvent(BlockSettingsUiEvent.BackupFailed(e.getErrorMessage()))
                launchInVM(Dispatchers.IO) {
                    runCatching { DocumentFile.fromSingleUri(context, uri)?.delete() }
                }
            }
            .onSuccess { count ->
                val cost = Clock.System.now() - start
                Log.i(TAG, "onBackup: $count rules exported, cost: $cost.")
                emitUiEvent(BlockSettingsUiEvent.BackupCompleted)
            }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun onRestoreFilePicked(uri: Uri) {
        if (currentState.loading) return else _uiState.update { it.copy(loading = true) }
        launchInVM(Dispatchers.IO) {
            val rec = runCatching {
                val doc = DocumentFile.fromSingleUri(context, uri)!!
                require(doc.exists() && doc.isFile)
                val metadata = context.contentResolver.openInputStream(uri)!!.use { input ->
                    BlockRuleBackupUtil.readMetadata(input)
                }
                return@runCatching metadata to uri
            }
            .onFailure { e ->
                Log.w(TAG, "onRestoreFilePicked", e)
                emitUiEvent(BlockSettingsUiEvent.BadBackup)
            }

            _uiState.update {
                it.copy(loading = false, pendingRestore = rec.getOrNull(), error = null)
            }
        }
    }

    /**
     * 恢复经过 [onRestoreFilePicked] 验证的屏蔽规则备份
     *
     * @param forum 是否恢复吧黑名单
     * @param keyword 是否恢复关键字规则
     * @param user 是否恢复用户规则
     * */
    fun onRestore(forum: Boolean, keyword: Boolean, user: Boolean) {
        val state = currentState
        if (state.loading || state.pendingRestore == null) return

        _uiState.update { it.copy(loading = true) }
        launchInVM {
            val start = Clock.System.now()
            // Convert to RestoreOption flags
            val option = (if (forum) 0 else RestoreOption.EXCLUDE_FORUM) or
                    (if (keyword) 0 else RestoreOption.EXCLUDE_KEYWORD) or
                    (if (user) 0 else RestoreOption.EXCLUDE_USER)

            runCatching {
                val (metadata, uri) = state.pendingRestore
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    BlockRuleBackupUtil.restore(blockDao, transactionRunner, input, option)
                }
                val cost = Clock.System.now() - start
                Log.w(TAG, "onRestore: Done, backup ver.${metadata.version}, opt: $option, cost: $cost")

                if (cost < 500.milliseconds) {
                    delay(1000) // Wait Dialog loading animation
                }
            }
            .onFailure { e ->
                Log.e(TAG, "onRestore", e)
                _uiState.update { it.copy(loading = false, error = e) }
            }
            .onSuccess {
                _uiState.update { it.copy(loading = false, pendingRestore = null) }
            }
        }
    }

    fun onCancelRestore() = _uiState.update { createInitialState() }
}
