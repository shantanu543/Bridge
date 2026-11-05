package com.bridge.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bridge.core.designsystem.BridgeTheme
import com.bridge.feature.auth.AuthViewModel
import com.bridge.feature.onboarding.BridgeNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BridgeActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BridgeTheme {
                BridgeNavHost(authViewModel, allowLocalDemo = BuildConfig.ALLOW_LOCAL_DEMO)
            }
        }
    }
}
