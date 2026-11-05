package com.bridge.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.bridge.core.network.ConsentPolicyDto

@Composable
fun ConsentScreen(
    policies: List<ConsentPolicyDto>,
    policiesUnavailable: Boolean,
    isLoading: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onAccept: () -> Unit,
    onContinueManually: () -> Unit,
    onRetry: () -> Unit,
) {
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var acceptedPrivacy by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val terms = policies.firstOrNull { it.policyType == "TERMS" }
    val privacy = policies.firstOrNull { it.policyType == "PRIVACY" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text("Your privacy comes first", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Review Bridge’s current policies before sharing profile data with connected sources. " +
                "You can continue with a private manual profile without connecting an account.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(20.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (policiesUnavailable) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Policies are not configured", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Bridge has not supplied approved Terms and Privacy documents in this environment. " +
                            "Source imports are unavailable. You may continue by entering a private profile manually.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else if (terms != null && privacy != null) {
            PolicyAcceptanceRow(
                label = "I agree to the Terms of Service (version ${terms.version}).",
                accepted = acceptedTerms,
                enabled = !isSaving,
                onAccepted = { acceptedTerms = it },
                onOpen = { uriHandler.openUri(terms.url) },
                linkLabel = "Read Terms",
            )
            PolicyAcceptanceRow(
                label = "I have read the Privacy Policy (version ${privacy.version}).",
                accepted = acceptedPrivacy,
                enabled = !isSaving,
                onAccepted = { acceptedPrivacy = it },
                onOpen = { uriHandler.openUri(privacy.url) },
                linkLabel = "Read Privacy Policy",
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Importing from GitHub, LinkedIn, or a resume will require a separate, source-specific choice.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Policy details could not be loaded. Retry to continue.", color = MaterialTheme.colorScheme.error)
        }

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))

        if (terms != null && privacy != null) {
            Button(
                onClick = onAccept,
                enabled = !isSaving && acceptedTerms && acceptedPrivacy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) CircularProgressIndicator() else Text("Accept and continue")
            }
        } else if (policiesUnavailable) {
            Button(
                onClick = onContinueManually,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue with private manual profile") }
        } else if (!isLoading) {
            TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
        }
    }
}

@Composable
private fun PolicyAcceptanceRow(
    label: String,
    accepted: Boolean,
    enabled: Boolean,
    onAccepted: (Boolean) -> Unit,
    onOpen: () -> Unit,
    linkLabel: String,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = accepted, onCheckedChange = onAccepted, enabled = enabled)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            linkLabel,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 52.dp).clickable(enabled = enabled, onClick = onOpen),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
