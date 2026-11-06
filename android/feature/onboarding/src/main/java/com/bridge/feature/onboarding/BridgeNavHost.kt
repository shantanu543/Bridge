package com.bridge.feature.onboarding

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import android.view.View
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bridge.feature.auth.AuthScreen
import com.bridge.feature.auth.AuthViewModel
import com.bridge.feature.auth.EmailAuthScreen
import com.bridge.feature.auth.EmailVerificationScreen
import com.bridge.feature.auth.ExternalIdentityProvider
import com.bridge.feature.auth.PasswordResetScreen
import com.bridge.core.designsystem.BridgeLogoMark
import com.bridge.core.designsystem.BridgePalette

@Composable
fun BridgeNavHost(authViewModel: AuthViewModel, allowLocalDemo: Boolean = false) {
    val navController = rememberNavController()
    val authState by authViewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState.verificationEmail) {
        if (authState.verificationEmail != null) {
            navController.navigate("verify-email")
        }
    }
    LaunchedEffect(authState.session?.userId, authState.session?.onboardingComplete) {
        authState.session?.let { session ->
            navController.navigate(if (session.onboardingComplete) "home" else "signed-in") {
                popUpTo("welcome") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(authState.signOutComplete) {
        if (authState.signOutComplete) {
            navController.navigate("auth") {
                popUpTo("welcome") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("welcome") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
            )
        }
        composable("welcome") {
            WelcomeScreen(
                onContinue = { navController.navigate("auth") },
                onLocalDemo = if (allowLocalDemo) authViewModel::signInLocalDemo else null,
                demoLoading = authState.isLoading,
                demoError = authState.errorMessage,
            )
        }
        composable("auth") {
            AuthScreen(
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                onExternalSignIn = { provider ->
                    when (provider) {
                        ExternalIdentityProvider.GOOGLE -> {
                            val activity = context as? Activity
                            if (activity != null) authViewModel.signInGoogle(activity)
                            else authViewModel.showError("Google sign-in is unavailable in this window.")
                        }
                        ExternalIdentityProvider.GITHUB -> authViewModel.showError(
                            "GitHub sign-in requires its approved OAuth app configuration.",
                        )
                        ExternalIdentityProvider.LINKEDIN -> authViewModel.showError(
                            "LinkedIn sign-in requires its approved OAuth app configuration.",
                        )
                    }
                },
                onEmailSignIn = {
                    authViewModel.clearError()
                    navController.navigate("email")
                },
            )
        }
        composable("email") {
            EmailAuthScreen(
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                onSubmit = authViewModel::submitEmail,
                onForgotPassword = {
                    authViewModel.clearError()
                    navController.navigate("password-reset")
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable("verify-email") {
            EmailVerificationScreen(
                email = authState.verificationEmail.orEmpty(),
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                noticeMessage = authState.noticeMessage,
                onVerify = { code ->
                    authState.verificationEmail?.let { authViewModel.verifyEmail(it, code) }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable("password-reset") {
            PasswordResetScreen(
                isLoading = authState.isLoading,
                codeSent = authState.passwordResetCodeSent,
                errorMessage = authState.errorMessage,
                noticeMessage = authState.noticeMessage,
                onRequestCode = authViewModel::requestPasswordReset,
                onConfirmReset = authViewModel::confirmPasswordReset,
                onBack = { navController.popBackStack() },
            )
        }
        composable("signed-in") {
            SignedInScreen(
                email = authState.verificationEmail,
                onContinue = { navController.navigate("consent") },
                onSignOut = {
                    authViewModel.signOut()
                },
            )
        }
        composable("consent") {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val onboardingState by onboardingViewModel.state.collectAsState()
            LaunchedEffect(Unit) { onboardingViewModel.load() }
            LaunchedEffect(onboardingState.status?.requiredConsentsRecorded) {
                if (onboardingState.status?.requiredConsentsRecorded == true) {
                    navController.navigate("profile-sources") {
                        popUpTo("consent") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            ConsentScreen(
                policies = onboardingState.policies,
                policiesUnavailable = onboardingState.policiesUnavailable,
                isLoading = onboardingState.isLoading,
                isSaving = onboardingState.isSaving,
                errorMessage = onboardingState.errorMessage,
                onAccept = { onboardingViewModel.recordImportConsents(emptyList(), false) },
                onContinueManually = { navController.navigate("profile") },
                onRetry = onboardingViewModel::retryLoad,
            )
        }
        composable("profile-sources") {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val onboardingState by onboardingViewModel.state.collectAsState()
            LaunchedEffect(Unit) { onboardingViewModel.load() }
            ProfileSourcesScreen(
                isLoading = onboardingState.isLoading,
                errorMessage = onboardingState.errorMessage,
                onContinueManually = { navController.navigate("profile") },
                onSignOut = {
                    authViewModel.signOut()
                },
            )
        }
        composable("profile") {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val onboardingState by onboardingViewModel.state.collectAsState()
            LaunchedEffect(Unit) { onboardingViewModel.load() }
            LaunchedEffect(onboardingState.status?.profileConfirmed) {
                if (onboardingState.status?.profileConfirmed == true) {
                    navController.navigate("passport") {
                        popUpTo("profile") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            ProfileReviewScreen(
                profile = onboardingState.profile,
                isLoading = onboardingState.isLoading,
                isSaving = onboardingState.isSaving,
                errorMessage = onboardingState.errorMessage,
                noticeMessage = onboardingState.noticeMessage,
                onProfileChanged = onboardingViewModel::updateProfile,
                onSaveDraft = onboardingViewModel::saveProfile,
                onConfirm = onboardingViewModel::confirmProfile,
                onBack = { navController.popBackStack() },
            )
        }
        composable("passport") {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val onboardingState by onboardingViewModel.state.collectAsState()
            LaunchedEffect(Unit) { onboardingViewModel.loadPassport() }
            PassportScreen(
                passport = onboardingState.passport,
                isLoading = onboardingState.isLoading,
                errorMessage = onboardingState.errorMessage,
                onRetry = onboardingViewModel::loadPassport,
                onExplore = { navController.navigate("home") },
                onSignOut = authViewModel::signOut,
            )
        }
        composable("home") {
            val communityViewModel: CommunityViewModel = hiltViewModel()
            BridgeHomeScreen(
                viewModel = communityViewModel,
                isLocalDemo = authState.session?.localDemo == true,
                onPassport = { navController.navigate("passport") },
                onSignOut = authViewModel::signOut,
            )
        }
    }
}

@Composable
private fun SignedInScreen(
    email: String?,
    onContinue: () -> Unit,
    onSignOut: () -> Unit,
) {
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            window.statusBarColor = android.graphics.Color.rgb(246, 247, 252)
            window.navigationBarColor = android.graphics.Color.rgb(246, 247, 252)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BridgePalette.paper)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        BridgeLogoMark(Modifier.size(width = 76.dp, height = 62.dp))
        Spacer(Modifier.height(14.dp))
        Text("Bridge", style = MaterialTheme.typography.headlineMedium, color = BridgePalette.navy, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text("You’re signed in", style = MaterialTheme.typography.headlineSmall, color = BridgePalette.ink)
        Spacer(Modifier.height(8.dp))
        Text("Next, build your professional Bridge Passport.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        email?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = BridgePalette.violet)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Text("Build your Bridge profile")
        }
        TextButton(onClick = onSignOut) {
            Text("Sign out")
        }
    }
}

