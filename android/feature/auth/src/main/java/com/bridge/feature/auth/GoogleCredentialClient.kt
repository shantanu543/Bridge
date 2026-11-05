package com.bridge.feature.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GoogleCredentialClient @Inject constructor(
    private val credentialManager: CredentialManager,
    @Named("googleServerClientId") private val serverClientId: String,
) {
    suspend fun requestIdToken(activity: Activity, nonce: String): String {
        check(serverClientId.isNotBlank()) { "Google sign-in is not configured for this build." }
        val googleOption = GetSignInWithGoogleOption.Builder(serverClientId)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()
        val response = credentialManager.getCredential(activity, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Google did not return an ID token credential.")
    }
}
