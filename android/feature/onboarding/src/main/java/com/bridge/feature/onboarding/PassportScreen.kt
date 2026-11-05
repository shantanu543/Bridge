package com.bridge.feature.onboarding

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bridge.core.network.PassportDto
import com.bridge.core.designsystem.BridgePalette

@Composable
fun PassportScreen(
    passport: PassportDto?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onExplore: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Your Bridge Passport", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("A private preview of your professional profile", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(22.dp))
        when {
            isLoading -> CircularProgressIndicator()
            passport == null -> {
                Text(errorMessage ?: "Your Passport is unavailable right now.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("Try again") }
            }
            else -> {
                ScoreCard(passport)
                Spacer(Modifier.height(14.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(passport.fullName ?: "Bridge member", style = MaterialTheme.typography.titleLarge)
                        passport.headline?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                        val roleLine = listOfNotNull(passport.currentRole, passport.currentCompany).joinToString(" · ")
                        if (roleLine.isNotBlank()) Text(roleLine, style = MaterialTheme.typography.bodyMedium)
                        passport.summary?.let { Spacer(Modifier.height(12.dp)); Text(it) }
                    }
                }
                passport.skills.takeIf { it.isNotEmpty() }?.let { skills ->
                    SectionCard("Skills", skills.joinToString("  ·  "))
                }
                passport.workHistory.takeIf { it.isNotEmpty() }?.let { history ->
                    SectionCard("Experience", history.joinToString("\n\n") { "${it.role} · ${it.company}" })
                }
                passport.education.takeIf { it.isNotEmpty() }?.let { education ->
                    SectionCard("Education", education.joinToString("\n") { listOfNotNull(it.qualification, it.institution).joinToString(" · ") })
                }
                if (passport.projects.isNotEmpty() || passport.achievements.isNotEmpty()) {
                    val entries = (passport.projects + passport.achievements).mapNotNull { it["title"] ?: it["name"] }
                    if (entries.isNotEmpty()) SectionCard("Projects and achievements", entries.joinToString("\n"))
                }
                Spacer(Modifier.height(10.dp))
                Text("Your Passport is private. Sharing will be available after privacy controls are completed.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
            }
        }
        TextButton(onClick = onSignOut) { Text("Sign out") }
        Button(onClick = onExplore) { Text("Explore Bridge") }
    }
}

@Composable
private fun ScoreCard(passport: PassportDto) {
    val score = passport.score
    val trackColor = BridgePalette.mint
    val scoreColor = BridgePalette.success
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Bridge Score", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            if (score.state == "CALCULATED" && score.score != null) {
                val scoreValue = score.score ?: 0
                Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 11.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        )
                        drawArc(
                            color = scoreColor,
                            startAngle = -90f,
                            sweepAngle = scoreValue * 3.6f,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        )
                    }
                    Text("$scoreValue", style = MaterialTheme.typography.headlineLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text("Evidence completeness in your Bridge profile", style = MaterialTheme.typography.bodyMedium)
                score.calculatedAt?.let { Text("Updated $it", style = MaterialTheme.typography.bodySmall) }
                score.suggestions.forEach { suggestion ->
                    Spacer(Modifier.height(8.dp))
                    Text("• $suggestion", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text("Add and confirm more professional evidence to calculate your score.", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Verification")
                Text(score.verificationState.replace('_', ' '))
            }
            Text("Verification is separate from your score.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionCard(title: String, body: String) {
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
