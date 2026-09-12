package com.huanchengfly.tieba.post.ui.page.user.edit

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.components.SessionManager
import com.huanchengfly.tieba.post.core.database.model.Account
import com.huanchengfly.tieba.post.core.network.exception.getErrorCode
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.user.EditProfile
import com.huanchengfly.tieba.post.utils.StringUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import java.io.File
import javax.inject.Inject

@Stable
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val userProfileRepo: UserProfileRepository,
) :
    BaseViewModel<EditProfileIntent, EditProfilePartialChange, EditProfileState, EditProfileEvent>() {
    override fun createInitialState(): EditProfileState = EditProfileState()

    override fun createPartialChangeProducer(): PartialChangeProducer<EditProfileIntent, EditProfilePartialChange, EditProfileState> =
        EditProfilePartialChangeProducer()

    private inner class EditProfilePartialChangeProducer : PartialChangeProducer<EditProfileIntent, EditProfilePartialChange, EditProfileState> {

        override fun toPartialChangeFlow(intentFlow: Flow<EditProfileIntent>): Flow<EditProfilePartialChange> =
            merge(
                intentFlow.filterIsInstance<EditProfileIntent.Init>()
                    .flatMapConcat { it.toPartialChangeFlow() },
                intentFlow.filterIsInstance<EditProfileIntent.Submit>()
                    .flatMapConcat { it.toPartialChangeFlow() },
                intentFlow.filterIsInstance<EditProfileIntent.UploadPortrait>()
                    .flatMapConcat { it.toPartialChangeFlow() },
                intentFlow.filterIsInstance<EditProfileIntent.UploadPortraitStart>()
                    .flatMapConcat { it.toPartialChangeFlow() }
            )

        private fun EditProfileIntent.Init.toPartialChangeFlow(): Flow<EditProfilePartialChange.Init> {
            return flow<EditProfilePartialChange.Init> {
                val updated = sessionManager.refreshCurrent(force = true)
                emit(EditProfilePartialChange.Init.Success(account = updated))
            }
            .onStart { emit(EditProfilePartialChange.Init.Loading) }
            .catch { emit(EditProfilePartialChange.Init.Fail(it)) }
        }

        private fun EditProfileIntent.Submit.toPartialChangeFlow() =
            flow { emit(userProfileRepo.requestProfileUpdate(edit)) }
                .map<Unit, EditProfilePartialChange.Submit> {
                    EditProfilePartialChange.Submit.Success
                }
                .onStart {
                    emit(EditProfilePartialChange.Submit.Submitting(edit))
                }
                .catch { emit(EditProfilePartialChange.Submit.Fail(it.getErrorMessage())) }

        private fun EditProfileIntent.UploadPortraitStart.toPartialChangeFlow() =
            flow { emit(EditProfilePartialChange.UploadPortrait.Start) }

        private fun EditProfileIntent.UploadPortrait.toPartialChangeFlow(): Flow<EditProfilePartialChange.UploadPortrait> =
            flow { emit(userProfileRepo.uploadPortrait(file)) }
                .map<String, EditProfilePartialChange.UploadPortrait> { message ->
                    EditProfilePartialChange.UploadPortrait.Success(message)
                }
                .onStart { emit(EditProfilePartialChange.UploadPortrait.Uploading) }
                .catch {
                    if (it.getErrorCode() == 300003) {
                        emit(EditProfilePartialChange.UploadPortrait.Success(it.getErrorMessage()))
                    } else {
                        emit(EditProfilePartialChange.UploadPortrait.Fail(it.getErrorMessage()))
                    }
                }
    }

    override fun dispatchEvent(partialChange: EditProfilePartialChange): UiEvent? {
        return when (partialChange) {
            is EditProfilePartialChange.Submit.Fail -> EditProfileEvent.Submit.Result(
                false,
                message = partialChange.error
            )

            EditProfilePartialChange.Submit.Success -> EditProfileEvent.Submit.Result(
                true,
                changed = false
            )

            EditProfilePartialChange.Submit.SuccessWithoutChange -> EditProfileEvent.Submit.Result(
                true
            )

            EditProfilePartialChange.UploadPortrait.Start -> EditProfileEvent.UploadPortrait.Pick
            is EditProfilePartialChange.UploadPortrait.Success -> EditProfileEvent.UploadPortrait.Success(
                partialChange.message
            )

            is EditProfilePartialChange.UploadPortrait.Fail -> EditProfileEvent.UploadPortrait.Fail(
                partialChange.error
            )

            else -> null
        }
    }

    fun onSubmitProfile(newProfile: EditProfile) {
        send(intent = EditProfileIntent.Submit(newProfile))
    }
}

sealed interface EditProfileEvent : UiEvent {

    sealed interface Submit : EditProfileEvent {
        data class Result(
            val success: Boolean,
            val changed: Boolean = success,
            val message: String = "",
        ) : Submit
    }

    sealed interface UploadPortrait : EditProfileEvent {
        data object Pick : UploadPortrait
        data class Success(val message: String) : UploadPortrait
        data class Fail(val error: String) : UploadPortrait
    }
}

sealed interface EditProfileIntent : UiIntent {
    object Init : EditProfileIntent

    data class Submit(val edit: EditProfile) : EditProfileIntent

    data class UploadPortrait(val file: File) : EditProfileIntent

    data object UploadPortraitStart : EditProfileIntent
}

sealed class EditProfilePartialChange : PartialChange<EditProfileState> {
    sealed class UploadPortrait : EditProfilePartialChange() {
        override fun reduce(oldState: EditProfileState): EditProfileState =
            when (this) {
                Uploading -> oldState
                Start -> oldState
                is Success -> oldState
                is Fail -> oldState
            }

        data object Uploading : UploadPortrait()
        data object Start : UploadPortrait()
        data class Success(val message: String) : UploadPortrait()
        data class Fail(val error: String) : UploadPortrait()

    }

    sealed class Init : EditProfilePartialChange() {
        override fun reduce(oldState: EditProfileState): EditProfileState =
            when (this) {
                is Loading -> oldState.copy(isLoading = true, error = null)
                is Success -> oldState.copy(
                    avatarUrl = StringUtil.getAvatarUrl(account.portrait),
                    name = account.name,
                    tbAge = account.tbAge.toString(),
                    edit = oldState.edit ?: EditProfile(
                        nickName = account.nickname ?: account.name,
                        sex = account.sex,
                        birthdayShowStatus = account.birthdayShow,
                        birthdayTime = account.birthdayTime * 1000L,
                        intro = account.intro,
                    ),
                    isLoading = false,
                    error = null
                )

                is Fail -> oldState.copy(isLoading = false, error = error)
            }

        data object Loading : Init()
        data class Success(val account: Account) : Init()
        data class Fail(val error: Throwable) : Init()
    }

    sealed class Submit : EditProfilePartialChange() {
        override fun reduce(oldState: EditProfileState): EditProfileState =
            when (this) {
                is Submitting -> oldState.copy(
                    isSubmitting = true,
                    edit = newProfile,
                    error = null
                )

                Success -> oldState.copy(isSubmitting = false)
                SuccessWithoutChange -> oldState.copy(isSubmitting = false)
                is Fail -> oldState.copy(isSubmitting = false)
            }

        data class Submitting(val newProfile: EditProfile) : Submit()

        data object Success : Submit()
        data object SuccessWithoutChange : Submit()
        data class Fail(val error: String) : Submit()
    }
}

data class EditProfileState(
    val avatarUrl: String? = null,
    val name: String = "",
    val tbAge: String = "0",
    val edit: EditProfile? = null,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: Throwable? = null
) : UiState