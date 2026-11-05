package com.bridge.feature.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bridge.core.datastore.StoredBridgeSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
    val verificationEmail: String? = null,
    val passwordResetCodeSent: Boolean = false,
    val session: StoredBridgeSession? = null,
    val signOutComplete: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.restoreSession() }
                .onSuccess { session ->
                    if (session != null) mutableState.value = AuthUiState(session = session)
                }
        }
    }

    fun signInGoogle(activity: Activity) = launchAuth {
        val session = repository.signInWithGoogle(activity)
        mutableState.value = AuthUiState(session = session)
    }

    fun signInLocalDemo() = launchAuth {
        val session = repository.signInLocalDemo()
        mutableState.value = AuthUiState(session = session, noticeMessage = "Local demo mode — synthetic development data.")
    }

    fun submitEmail(email: String, password: String, action: EmailAuthAction) = launchAuth {
        when (action) {
            EmailAuthAction.CREATE_ACCOUNT -> {
                val notice = repository.registerWithEmail(email, password)
                mutableState.value = AuthUiState(
                    verificationEmail = email,
                    noticeMessage = notice.message,
                )
            }

            EmailAuthAction.SIGN_IN -> {
                val session = repository.signInWithEmail(email, password)
                mutableState.value = AuthUiState(session = session)
            }
        }
    }

    fun verifyEmail(email: String, code: String) = launchAuth {
        val session = repository.verifyEmail(email, code)
        mutableState.value = AuthUiState(session = session)
    }

    fun requestPasswordReset(email: String) = launchAuth {
        val notice = repository.requestPasswordReset(email)
        mutableState.value = AuthUiState(noticeMessage = notice.message, passwordResetCodeSent = true)
    }

    fun confirmPasswordReset(email: String, code: String, newPassword: String) = launchAuth {
        val notice = repository.confirmPasswordReset(email, code, newPassword)
        mutableState.value = AuthUiState(noticeMessage = notice.message)
    }

    fun showError(message: String) {
        mutableState.value = mutableState.value.copy(errorMessage = message)
    }

    fun signOut() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            val serverSignOutFailed = runCatching { repository.signOut() }.isFailure
            mutableState.value = AuthUiState(
                signOutComplete = true,
                noticeMessage = if (serverSignOutFailed) {
                    "Signed out on this device. Server revocation could not be confirmed."
                } else {
                    null
                },
            )
        }
    }

    fun clearNotice() {
        mutableState.value = mutableState.value.copy(noticeMessage = null)
    }

    private fun launchAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching { block() }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        errorMessage = when (error) {
                            is HttpException -> when (error.code()) {
                                401 -> "We could not sign you in. Check your details and try again."
                                409 -> "This email is linked to another sign-in method. Sign in with that method to link accounts."
                                429 -> "Too many attempts. Wait a little and try again."
                                else -> "Bridge could not complete that request. Please try again."
                            }
                            is IOException -> "Check your connection and try again."
                            else -> error.message ?: "Bridge could not complete that request. Please try again."
                        },
                    )
                }
                .onSuccess {
                    if (mutableState.value.isLoading) {
                        mutableState.value = mutableState.value.copy(isLoading = false)
                    }
                }
        }
    }
}
