package com.bridge.feature.auth

import android.app.Activity
import com.bridge.core.datastore.SecureSessionStore
import com.bridge.core.datastore.StoredBridgeSession
import com.bridge.core.network.BridgeAuthApi
import com.bridge.core.network.EmailLoginDto
import com.bridge.core.network.EmailRegistrationDto
import com.bridge.core.network.EmailVerificationDto
import com.bridge.core.network.GoogleExchangeRequest
import com.bridge.core.network.PasswordResetConfirmDto
import com.bridge.core.network.PasswordResetDto
import com.bridge.core.network.RefreshDto
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import retrofit2.HttpException

@Singleton
class AuthRepository @Inject constructor(
    private val api: BridgeAuthApi,
    private val googleCredentialClient: GoogleCredentialClient,
    private val sessionStore: SecureSessionStore,
) {
    suspend fun signInLocalDemo(): StoredBridgeSession = persistSession(api.createDevDemoSession())

    suspend fun signInWithGoogle(activity: Activity): StoredBridgeSession {
        val challenge = api.googleChallenge()
        val idToken = googleCredentialClient.requestIdToken(activity, challenge.nonce)
        val response = api.googleExchange(
            GoogleExchangeRequest(
                challengeId = challenge.challengeId,
                idToken = idToken,
                deviceLabel = android.os.Build.MODEL.take(120),
            ),
        )
        return persistSession(response)
    }

    suspend fun registerWithEmail(email: String, password: String) =
        api.registerEmail(EmailRegistrationDto(email = email, password = password))

    suspend fun verifyEmail(email: String, code: String): StoredBridgeSession {
        val response = api.verifyEmail(
            EmailVerificationDto(
                email = email,
                code = code,
                deviceLabel = android.os.Build.MODEL.take(120),
            ),
        )
        return persistSession(response)
    }

    suspend fun signInWithEmail(email: String, password: String): StoredBridgeSession {
        val response = api.loginEmail(
            EmailLoginDto(
                email = email,
                password = password,
                deviceLabel = android.os.Build.MODEL.take(120),
            ),
        )
        return persistSession(response)
    }

    suspend fun requestPasswordReset(email: String) =
        api.requestPasswordReset(PasswordResetDto(email))

    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String) =
        api.confirmPasswordReset(PasswordResetConfirmDto(email, code, newPassword))

    suspend fun refreshSession(): StoredBridgeSession? {
        val current = sessionStore.read() ?: return null
        return try {
            persistSession(api.refresh(RefreshDto(current.refreshToken)))
        } catch (error: Throwable) {
            if (error is HttpException && error.code() == 401) sessionStore.clear()
            throw error
        }
    }

    suspend fun restoreSession(): StoredBridgeSession? {
        val current = sessionStore.read() ?: return null
        if (current.accessExpiresAtEpochSeconds > Instant.now().epochSecond + 30) return current
        return refreshSession()
    }

    suspend fun signOut() {
        val current = sessionStore.read()
        try {
            if (current != null) api.logout(RefreshDto(current.refreshToken))
        } finally {
            sessionStore.clear()
        }
    }

    suspend fun currentUser() = sessionStore.read()?.let { current ->
        api.currentUser("Bearer ${current.accessToken}")
    }

    private suspend fun persistSession(session: com.bridge.core.network.SessionDto): StoredBridgeSession {
        sessionStore.save(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            expiresInSeconds = session.expiresIn,
            userId = session.userId,
            onboardingComplete = session.onboardingComplete,
            localDemo = session.localDemo,
        )
        return sessionStore.read() ?: error("Could not persist the Bridge session securely.")
    }
}
