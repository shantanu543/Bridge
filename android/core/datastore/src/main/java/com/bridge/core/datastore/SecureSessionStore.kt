package com.bridge.core.datastore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.bridgeSessionDataStore by preferencesDataStore(name = "bridge-secure-session")
private val encryptedSessionKey = stringPreferencesKey("encrypted_session_v1")

@Serializable
data class StoredBridgeSession(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAtEpochSeconds: Long,
    val userId: String,
    val onboardingComplete: Boolean = false,
    val localDemo: Boolean = false,
)

@Singleton
class SecureSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun save(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        userId: String,
        onboardingComplete: Boolean = false,
        localDemo: Boolean = false,
    ): Unit = withContext(Dispatchers.IO) {
        val session = StoredBridgeSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessExpiresAtEpochSeconds = Instant.now().epochSecond + expiresInSeconds,
            userId = userId,
            onboardingComplete = onboardingComplete,
            localDemo = localDemo,
        )
        val plaintext = Json.encodeToString(StoredBridgeSession.serializer(), session)
        val encrypted = encrypt(plaintext.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        context.bridgeSessionDataStore.edit { preferences ->
            preferences[encryptedSessionKey] = encoded
        }
        Unit
    }

    suspend fun read(): StoredBridgeSession? = withContext(Dispatchers.IO) {
        val encoded = context.bridgeSessionDataStore.data.first()[encryptedSessionKey] ?: return@withContext null
        runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val plaintext = decrypt(bytes).toString(Charsets.UTF_8)
            Json.decodeFromString(StoredBridgeSession.serializer(), plaintext)
        }.getOrNull()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        context.bridgeSessionDataStore.edit { it.clear() }
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plaintext)
        val iv = cipher.iv
        return byteArrayOf(iv.size.toByte()) + iv + encrypted
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        require(payload.isNotEmpty()) { "Encrypted session is empty" }
        val ivLength = payload[0].toInt()
        require(ivLength in 12..16 && payload.size > ivLength + 1) { "Encrypted session is malformed" }
        val iv = payload.copyOfRange(1, ivLength + 1)
        val ciphertext = payload.copyOfRange(ivLength + 1, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "bridge.session.encryption.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
