package com.bridge.app.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.bridge.app.BuildConfig
import com.bridge.core.network.BridgeAuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)

    @Provides
    @Named("googleServerClientId")
    fun provideGoogleServerClientId(@ApplicationContext context: Context): String =
        context.getString(com.bridge.app.R.string.google_server_client_id)

    @Provides
    @Singleton
    fun provideBridgeAuthApi(): BridgeAuthApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BridgeAuthApi::class.java)
    }
}
