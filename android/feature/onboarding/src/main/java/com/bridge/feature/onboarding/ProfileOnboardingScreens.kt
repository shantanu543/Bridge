package com.bridge.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bridge.core.network.ProfileDto
import com.bridge.core.network.EducationDto
import com.bridge.core.network.PortfolioLinkDto
import com.bridge.core.network.WorkExperienceDto
import com.bridge.core.designsystem.BridgePalette

@Composable
fun ProfileSourcesScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onContinueManually: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        FlowStepHeader(
            step = 3,
            title = "Profile import & analysis",
            description = "Bring your professional story together. Imports are optional; you can continue with a private manual profile.",
        )
        Spacer(Modifier.height(18.dp))
        Text("Choose a profile source", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Connect only through approved integrations or add details yourself.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        SourceStatusCard(
            title = "LinkedIn",
            status = "Official API access is not configured",
            body = "Bridge does not scrape LinkedIn. Manual entry is available now; user-provided LinkedIn export/PDF import is pending the private document-processing setup.",
        )
        Spacer(Modifier.height(12.dp))
        SourceStatusCard(
            title = "GitHub",
            status = "OAuth setup is pending",
            body = "Public profile import is not enabled yet. It will use only approved official API access and your explicit consent; private repositories will not be analyzed.",
        )
        Spacer(Modifier.height(12.dp))
        SourceStatusCard(
            title = "Resume",
            status = "Private upload storage is pending",
            body = "PDF/DOCX upload and parsing are not enabled yet. They require private storage, file scanning, a parser worker, and an approved retention policy.",
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onContinueManually,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator() else Text("Continue with manual profile")
        }
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out")
        }
    }
}

@Composable
private fun SourceStatusCard(title: String, status: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Not connected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FlowStepHeader(step: Int, title: String, description: String) {
    Column {
        Text(
            "STEP $step OF 6",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { step / 6f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = BridgePalette.lavenderStrong,
        )
    }
}

@Composable
fun ProfileReviewScreen(
    profile: ProfileDto?,
    isLoading: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    noticeMessage: String?,
    onProfileChanged: (ProfileDto) -> Unit,
    onSaveDraft: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    if (profile == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (isLoading) CircularProgressIndicator()
            else Text(errorMessage ?: "Your profile could not be loaded.")
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }

    val requiredFieldsComplete = !profile.fullName.isNullOrBlank() && !profile.headline.isNullOrBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back to sources") }
        Text("REVIEW & COMPLETE PROFILE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text("Review your profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your Bridge profile stays private until you choose what to share. Confirmed details are not employment verification.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(20.dp))
        Text("Profile visibility", style = MaterialTheme.typography.titleLarge)
        Text(
            "This is your chosen audience if your profile is published later. Publishing is not enabled yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf("private" to "Only me", "connections" to "Accepted connections", "members" to "Bridge members")
            .forEach { (value, label) ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(
                        selected = profile.visibility == value,
                        onClick = { onProfileChanged(profile.copy(visibility = value)) },
                        enabled = !isSaving,
                    )
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = BridgePalette.lavender),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Profile preview · private", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = BridgePalette.lavenderStrong,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                profile.fullName?.trim()?.firstOrNull()?.uppercase() ?: "B",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(profile.fullName?.ifBlank { "Your name" } ?: "Your name", style = MaterialTheme.typography.titleMedium)
                        Text(
                            profile.headline?.ifBlank { "Professional headline" } ?: "Professional headline",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                val currentRole = listOfNotNull(profile.currentRole, profile.currentCompany)
                    .filter(String::isNotBlank)
                    .joinToString(" at ")
                if (currentRole.isNotBlank()) Text(currentRole)
                profile.location?.takeIf(String::isNotBlank)?.let { Text(it) }
                profile.summary?.takeIf(String::isNotBlank)?.let { summary ->
                    Spacer(Modifier.height(6.dp))
                    Text(summary)
                }
                if (profile.skills.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(profile.skills.take(8).joinToString(" · "))
                }
                Text(
                    "Private until publishing is available and you choose to publish.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ProfileField("Full name", profile.fullName.orEmpty(), "full_name", profile, enabled = !isSaving) {
            onProfileChanged(profile.copy(fullName = it))
        }
        ProfileField("Professional headline", profile.headline.orEmpty(), "headline", profile, enabled = !isSaving) {
            onProfileChanged(profile.copy(headline = it))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileField(
                label = "Current role",
                value = profile.currentRole.orEmpty(),
                fieldKey = "current_role",
                profile = profile,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
            ) { onProfileChanged(profile.copy(currentRole = it)) }
            ProfileField(
                label = "Company",
                value = profile.currentCompany.orEmpty(),
                fieldKey = "current_company",
                profile = profile,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
            ) { onProfileChanged(profile.copy(currentCompany = it)) }
        }
        ProfileField("Professional summary", profile.summary.orEmpty(), "summary", profile, multiline = true, enabled = !isSaving) {
            onProfileChanged(profile.copy(summary = it))
        }
        ProfileField("Location (optional)", profile.location.orEmpty(), "location", profile, enabled = !isSaving) {
            onProfileChanged(profile.copy(location = it))
        }
        OutlinedTextField(
            value = profile.yearsExperience?.toString().orEmpty(),
            onValueChange = { value ->
                val years = value.filter(Char::isDigit).take(2).toIntOrNull()?.coerceAtMost(70)
                onProfileChanged(profile.copy(yearsExperience = years))
            },
            label = { Text("Years of experience (optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        Text("Skills", style = MaterialTheme.typography.titleLarge)
        Text("Separate skills with commas.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = profile.skills.joinToString(", "),
            onValueChange = { value ->
                onProfileChanged(
                    profile.copy(skills = value.split(",").map(String::trim).filter(String::isNotBlank).take(100)),
                )
            },
            label = { Text("Skills") },
            minLines = 2,
            maxLines = 4,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        )
        Text("Work history", style = MaterialTheme.typography.titleLarge)
        profile.workHistory.forEachIndexed { index, item ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = item.role,
                        onValueChange = { value ->
                            onProfileChanged(profile.copy(workHistory = profile.workHistory.updated(index) { it.copy(role = value) }))
                        },
                        label = { Text("Role") },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = item.company,
                        onValueChange = { value ->
                            onProfileChanged(profile.copy(workHistory = profile.workHistory.updated(index) { it.copy(company = value) }))
                        },
                        label = { Text("Company") },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = item.startDate.orEmpty(),
                        onValueChange = { value ->
                            onProfileChanged(profile.copy(workHistory = profile.workHistory.updated(index) { it.copy(startDate = value) }))
                        },
                        label = { Text("Start date (optional)") },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = item.endDate.orEmpty(),
                        onValueChange = { value ->
                            onProfileChanged(profile.copy(workHistory = profile.workHistory.updated(index) { it.copy(endDate = value) }))
                        },
                        label = { Text("End date (optional)") },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = item.description.orEmpty(),
                        onValueChange = { value ->
                            onProfileChanged(profile.copy(workHistory = profile.workHistory.updated(index) { it.copy(description = value) }))
                        },
                        label = { Text("Description (optional)") },
                        minLines = 2,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = { onProfileChanged(profile.copy(workHistory = profile.workHistory.filterIndexed { i, _ -> i != index })) },
                        enabled = !isSaving,
                    ) { Text("Remove this role") }
                }
            }
        }
        TextButton(
            onClick = { onProfileChanged(profile.copy(workHistory = profile.workHistory + WorkExperienceDto(role = "", company = ""))) },
            enabled = !isSaving,
        ) { Text("Add work experience") }

        Text("Education", style = MaterialTheme.typography.titleLarge)
        profile.education.forEachIndexed { index, item ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = item.institution,
                        onValueChange = { value ->
                            onProfileChanged(profile.copy(education = profile.education.updated(index) { it.copy(institution = value) }))
                        },
                        label = { Text("Institution") },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = item.qualification.orEmpty(),
                        onValueChange = { value ->
                            onProfileChanged(profile.copy(education = profile.education.updated(index) { it.copy(qualification = value) }))
                        },
                        label = { Text("Qualification") },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = { onProfileChanged(profile.copy(education = profile.education.filterIndexed { i, _ -> i != index })) },
                        enabled = !isSaving,
                    ) { Text("Remove this education") }
                }
            }
        }
        TextButton(
            onClick = { onProfileChanged(profile.copy(education = profile.education + EducationDto(institution = ""))) },
            enabled = !isSaving,
        ) { Text("Add education") }

        ProfileListTextField(
            title = "Projects",
            label = "One project per line",
            entries = profile.projects,
            entryKey = "name",
            enabled = !isSaving,
            onEntriesChanged = { onProfileChanged(profile.copy(projects = it)) },
        )
        ProfileListTextField(
            title = "Achievements",
            label = "One achievement per line",
            entries = profile.achievements,
            entryKey = "description",
            enabled = !isSaving,
            onEntriesChanged = { onProfileChanged(profile.copy(achievements = it)) },
        )
        ProfileListTextField(
            title = "Certifications",
            label = "One certification per line",
            entries = profile.certifications,
            entryKey = "name",
            enabled = !isSaving,
            onEntriesChanged = { onProfileChanged(profile.copy(certifications = it)) },
        )
        PortfolioLinksField(
            links = profile.portfolioLinks,
            enabled = !isSaving,
            onLinksChanged = { onProfileChanged(profile.copy(portfolioLinks = it)) },
        )
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        noticeMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = onSaveDraft,
            enabled = !isSaving && requiredFieldsComplete,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) CircularProgressIndicator() else Text("Save private draft")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onConfirm,
            enabled = !isSaving && requiredFieldsComplete && profile.confirmedAt == null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Confirm profile review") }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    fieldKey: String,
    profile: ProfileDto,
    modifier: Modifier = Modifier.fillMaxWidth(),
    multiline: Boolean = false,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    val source = profile.provenance.firstOrNull { it.fieldKey == fieldKey }
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            minLines = if (multiline) 4 else 1,
            maxLines = if (multiline) 6 else 1,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        if (source != null) {
            Text(
                if (source.sourceType == "MANUAL") "Added by you" else "Imported from ${source.sourceType}",
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ProfileListTextField(
    title: String,
    label: String,
    entries: List<Map<String, String>>,
    entryKey: String,
    enabled: Boolean,
    onEntriesChanged: (List<Map<String, String>>) -> Unit,
) {
    val lines = entries.mapNotNull { it[entryKey]?.takeIf(String::isNotBlank) }.joinToString("\n")
    Text(title, style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = lines,
        onValueChange = { input ->
            onEntriesChanged(
                input.lines()
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .take(50)
                    .map { mapOf(entryKey to it) },
            )
        },
        label = { Text(label) },
        minLines = 2,
        maxLines = 8,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    )
}

@Composable
private fun PortfolioLinksField(
    links: List<PortfolioLinkDto>,
    enabled: Boolean,
    onLinksChanged: (List<PortfolioLinkDto>) -> Unit,
) {
    val text = links.joinToString("\n") { "${it.label} | ${it.url}" }
    Text("Portfolio links", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            onLinksChanged(
                input.lines().mapNotNull { line ->
                    val parts = line.split("|", limit = 2)
                    if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) null
                    else PortfolioLinkDto(parts[0].trim(), parts[1].trim())
                }.take(30),
            )
        },
        label = { Text("One link per line: Label | https://example.com") },
        minLines = 2,
        maxLines = 6,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    )
}

private inline fun <T> List<T>.updated(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { currentIndex, item -> if (currentIndex == index) transform(item) else item }
