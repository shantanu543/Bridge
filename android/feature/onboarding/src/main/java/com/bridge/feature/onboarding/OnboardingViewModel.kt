package com.bridge.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bridge.core.network.ConsentPolicyDto
import com.bridge.core.network.OnboardingStatusDto
import com.bridge.core.network.ProfileDto
import com.bridge.core.network.PassportDto
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val profile: ProfileDto? = null,
    val passport: PassportDto? = null,
    val status: OnboardingStatusDto? = null,
    val policies: List<ConsentPolicyDto> = emptyList(),
    val policiesUnavailable: Boolean = false,
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
)

private data class OnboardingLoadResult(
    val profile: ProfileDto,
    val status: OnboardingStatusDto,
    val policies: List<ConsentPolicyDto>,
    val policiesUnavailable: Boolean,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(OnboardingUiState())
    val state = mutableState.asStateFlow()

    fun load() {
        if (mutableState.value.isLoading || mutableState.value.profile != null) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val profile = repository.loadProfile()
                val status = repository.loadStatus()
                val policiesResult = runCatching { repository.loadPolicies() }
                val policyError = policiesResult.exceptionOrNull()
                if (policyError != null && (policyError !is HttpException || policyError.code() != 503)) {
                    throw policyError
                }
                OnboardingLoadResult(profile, status, policiesResult.getOrDefault(emptyList()), policyError != null)
            }.onSuccess { (profile, status, policies, policiesUnavailable) ->
                mutableState.value = OnboardingUiState(
                    profile = profile,
                    status = status,
                    policies = policies,
                    policiesUnavailable = policiesUnavailable,
                )
            }.onFailure { error -> fail(error) }
        }
    }

    fun retryLoad() {
        mutableState.value = mutableState.value.copy(
            profile = null,
            status = null,
            policies = emptyList(),
            policiesUnavailable = false,
            errorMessage = null,
        )
        load()
    }

    fun updateProfile(updated: ProfileDto) {
        mutableState.value = mutableState.value.copy(profile = updated, errorMessage = null)
    }

    fun saveProfile() {
        val profile = mutableState.value.profile ?: return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSaving = true, errorMessage = null)
            runCatching { repository.saveProfile(profile) }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        isSaving = false,
                        profile = it,
                        noticeMessage = "Draft saved privately.",
                    )
                }
                .onFailure(::fail)
        }
    }

    fun confirmProfile() {
        val profile = mutableState.value.profile ?: return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val saved = repository.saveProfile(profile)
                mutableState.value = mutableState.value.copy(profile = saved)
                repository.confirmProfile()
            }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        isSaving = false,
                        profile = it,
                        status = mutableState.value.status?.copy(profileConfirmed = true),
                        noticeMessage = "Profile review confirmed. Your profile is still private.",
                    )
                }
                .onFailure(::fail)
        }
    }

    fun loadPassport() {
        if (mutableState.value.isLoading || mutableState.value.passport != null) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.loadPassport() }
                .onSuccess { passport ->
                    mutableState.value = mutableState.value.copy(isLoading = false, passport = passport)
                }
                .onFailure(::fail)
        }
    }

    fun recordImportConsents(providers: List<String>, resumeProcessing: Boolean) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                repository.recordConsents(
                    acceptTerms = true,
                    acceptPrivacy = true,
                    providers = providers,
                    resumeProcessing = resumeProcessing,
                )
            }.onSuccess {
                mutableState.value = mutableState.value.copy(
                    isSaving = false,
                    status = mutableState.value.status?.copy(requiredConsentsRecorded = true),
                    noticeMessage = "Your consent choices were saved.",
                )
            }.onFailure(::fail)
        }
    }

    private fun fail(error: Throwable) {
        val message = when (error) {
            is HttpException -> when (error.code()) {
                401 -> "Your session expired. Sign in again."
                403 -> "Accept the required policies before importing profile data."
                503 -> "This option is not configured in this environment yet."
                else -> "Bridge could not save this change. Please try again."
            }
            is IOException -> "Check your connection and try again."
            else -> error.message ?: "Bridge could not save this change. Please try again."
        }
        mutableState.value = mutableState.value.copy(
            isLoading = false,
            isSaving = false,
            errorMessage = message,
        )
    }
}
