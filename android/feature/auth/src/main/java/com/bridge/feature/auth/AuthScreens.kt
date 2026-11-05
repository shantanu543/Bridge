package com.bridge.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bridge.core.designsystem.BridgeLogoMark
import com.bridge.core.designsystem.BridgePalette

enum class ExternalIdentityProvider {
    GOOGLE,
    GITHUB,
}

enum class EmailAuthAction {
    SIGN_IN,
    CREATE_ACCOUNT,
}

@Composable
fun AuthScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onExternalSignIn: (ExternalIdentityProvider) -> Unit,
    onEmailSignIn: () -> Unit,
) {
    SetLightSystemBars()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BridgePalette.paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            BridgeLogoMark(Modifier.size(width = 64.dp, height = 52.dp), contentDescription = "Bridge logo")
        }
        Text("Bridge", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineMedium, color = BridgePalette.navy, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text("Welcome", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, color = BridgePalette.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "A professional network for people building what’s next.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        OutlinedButton(
            onClick = { onExternalSignIn(ExternalIdentityProvider.GOOGLE) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue with Google")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onExternalSignIn(ExternalIdentityProvider.GITHUB) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue with GitHub")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onEmailSignIn,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue with email")
        }
        if (isLoading) {
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator()
        }
        errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Signing in does not verify employment or professional history.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EmailAuthScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (email: String, password: String, action: EmailAuthAction) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onBack: () -> Unit,
) {
    SetLightSystemBars()
    var isCreatingAccount by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val normalizedEmail = email.trim()
    val emailLooksValid = normalizedEmail.contains('@') && normalizedEmail.substringAfter('@').contains('.')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text(
            if (isCreatingAccount) "Create your account" else "Sign in with email",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Use your email to keep your Bridge account secure.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                onSubmit(
                    normalizedEmail,
                    password,
                    if (isCreatingAccount) EmailAuthAction.CREATE_ACCOUNT else EmailAuthAction.SIGN_IN,
                )
            },
            enabled = !isLoading && emailLooksValid && password.length >= 12,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator()
            else Text(if (isCreatingAccount) "Create account" else "Sign in")
        }
        if (!isCreatingAccount) {
            TextButton(
                onClick = { onForgotPassword(normalizedEmail) },
                enabled = !isLoading && emailLooksValid,
            ) {
                Text("Forgot password?")
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = { isCreatingAccount = !isCreatingAccount },
            enabled = !isLoading,
        ) {
            Text(
                if (isCreatingAccount) "Already have an account? Sign in"
                else "New to Bridge? Create an account",
            )
        }
    }
}

@Composable
fun EmailVerificationScreen(
    email: String,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    noticeMessage: String? = null,
    onVerify: (code: String) -> Unit,
    onBack: () -> Unit,
) {
    SetLightSystemBars()
    var code by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text("Verify your email", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Enter the six-digit code sent to $email.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter(Char::isDigit).take(6) },
            label = { Text("Verification code") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        noticeMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onVerify(code) },
            enabled = !isLoading && code.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator() else Text("Verify and continue")
        }
    }
}

@Composable
fun PasswordResetScreen(
    isLoading: Boolean = false,
    codeSent: Boolean = false,
    errorMessage: String? = null,
    noticeMessage: String? = null,
    onRequestCode: (email: String) -> Unit,
    onConfirmReset: (email: String, code: String, newPassword: String) -> Unit,
    onBack: () -> Unit,
) {
    SetLightSystemBars()
    var email by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val normalizedEmail = email.trim()
    val emailLooksValid = normalizedEmail.contains('@') && normalizedEmail.substringAfter('@').contains('.')
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text("Reset your password", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("We will email a one-time code if an account matches.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        if (codeSent) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                label = { Text("Reset code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it.take(128) },
                label = { Text("New password (at least 12 characters)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        noticeMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (codeSent) onConfirmReset(normalizedEmail, code, newPassword)
                else onRequestCode(normalizedEmail)
            },
            enabled = !isLoading && emailLooksValid &&
                (!codeSent || (code.length == 6 && newPassword.length >= 12)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator()
            else Text(if (codeSent) "Reset password" else "Send reset code")
        }
    }
}

@Composable
private fun SetLightSystemBars() {
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            window.statusBarColor = android.graphics.Color.rgb(246, 247, 252)
            window.navigationBarColor = android.graphics.Color.rgb(246, 247, 252)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
}
