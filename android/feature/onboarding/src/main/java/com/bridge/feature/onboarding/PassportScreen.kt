package com.bridge.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
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
        modifier = Modifier.fillMaxSize().background(BridgePalette.paper).navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Your Bridge Score", style = MaterialTheme.typography.headlineMedium, color = BridgePalette.navy)
        Spacer(Modifier.height(6.dp))
        Text("A signal of your professional profile strength.", style = MaterialTheme.typography.bodyMedium, color = BridgePalette.muted)
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
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
                    border = BorderStroke(1.dp, BridgePalette.line),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(56.dp), shape = CircleShape, color = BridgePalette.lavender) {
                                Box(contentAlignment = Alignment.Center) {
                                    val initials = (passport.fullName ?: "B").split(' ').mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                                    Text(initials, color = BridgePalette.violet, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(passport.fullName ?: "Bridge member", style = MaterialTheme.typography.titleLarge, color = BridgePalette.navy)
                                passport.headline?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = BridgePalette.muted) }
                            }
                        }
                        val roleLine = listOfNotNull(passport.currentRole, passport.currentCompany).joinToString(" · ")
                        if (roleLine.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(roleLine, style = MaterialTheme.typography.bodyMedium, color = BridgePalette.ink)
                        }
                        passport.summary?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = BridgePalette.muted)
                        }
                    }
                }
                passport.skills.takeIf { it.isNotEmpty() }?.let { skills ->
                    SkillCard(skills)
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
                Text(
                    "Bridge Score is an indicative profile signal, not a hiring decision.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BridgePalette.muted,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
        TextButton(onClick = onSignOut) { Text("Sign out") }
        Button(onClick = onExplore, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text("Explore Bridge") }
    }
}

private fun formatScoreTime(raw: String): String {
    return runCatching {
        val zoned = java.time.Instant.parse(raw).atZone(java.time.ZoneId.systemDefault())
        java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a").format(zoned)
    }.getOrDefault(raw)
}

@Composable
private fun ScoreCard(passport: PassportDto) {
    val score = passport.score
    val trackColor = BridgePalette.lavenderStrong
    val scoreColor = BridgePalette.violet
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = BridgePalette.white),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Your Bridge Score", style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$scoreValue", style = MaterialTheme.typography.displayMedium, color = BridgePalette.navy)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (scoreValue >= 70) "A strong, verified professional profile." else "Evidence completeness in your Bridge profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BridgePalette.ink,
                )
                score.calculatedAt?.let { Text("Updated ${formatScoreTime(it)}", style = MaterialTheme.typography.bodySmall, color = BridgePalette.muted) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillCard(skills: List<String>) {
    Spacer(Modifier.height(12.dp))
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
        border = BorderStroke(1.dp, BridgePalette.line),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Skills", style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                skills.take(12).forEach { skill ->
                    Surface(shape = CircleShape, color = BridgePalette.lavender) {
                        Text(skill, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = BridgePalette.violet, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, body: String) {
    Spacer(Modifier.height(12.dp))
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
        border = BorderStroke(1.dp, BridgePalette.line),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
