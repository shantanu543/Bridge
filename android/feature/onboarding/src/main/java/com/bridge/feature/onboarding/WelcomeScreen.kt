package com.bridge.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import com.bridge.core.designsystem.BridgePalette
import com.bridge.core.designsystem.BridgeLogoMark
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    SetDarkSystemBars()
    LaunchedEffect(Unit) {
        delay(1_800)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ColorForWelcome.splashNavy, BridgePalette.navy, ColorForWelcome.purple),
                ),
            ),
    ) {
        BottomWaveArtwork(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BridgeLogoMark(Modifier.size(width = 116.dp, height = 104.dp))
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Bridge",
                color = BridgePalette.white,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(25.dp))
            Text(
                text = "RIGHT PEOPLE.\nREAL INTENT.\nNO NOISE.",
                color = BridgePalette.white,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                letterSpacing = 2.6.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            Text(
                text = "A PROFESSIONAL NETWORK WITH PURPOSE",
                color = BridgePalette.white.copy(alpha = 0.72f),
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onLocalDemo: (() -> Unit)? = null,
    demoLoading: Boolean = false,
    demoError: String? = null,
) {
    SetLightSystemBars(BridgePalette.paper)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BridgePalette.paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            "WELCOME TO BRIDGE",
            color = BridgePalette.violet,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(18.dp))
        BridgeLogoMark(Modifier.size(width = 56.dp, height = 48.dp))
        Spacer(Modifier.height(18.dp))
        Text(
            "Build a circle that\nmoves you forward.",
            color = BridgePalette.navy,
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Meet verified professionals who share your ambition, craft and intent.",
            color = BridgePalette.muted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(22.dp))
        WelcomeValue("01", "Signal over noise", "Quality connections, not vanity metrics.")
        Spacer(Modifier.height(10.dp))
        WelcomeValue("02", "Built on proof", "A profile grounded in real professional work.")
        Spacer(Modifier.height(10.dp))
        WelcomeValue("03", "Made for momentum", "Turn conversations into opportunities.")
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = BridgePalette.violet, contentColor = BridgePalette.white),
        ) {
            Text("Get started", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "By continuing, you agree to our Terms and Privacy Policy.",
            color = BridgePalette.muted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (onLocalDemo != null) {
            TextButton(onClick = onLocalDemo, enabled = !demoLoading, modifier = Modifier.fillMaxWidth()) {
                if (demoLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BridgePalette.violet)
                else Text("Explore local demo", color = BridgePalette.violet)
            }
            demoError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun WelcomeValue(index: String, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = BridgePalette.white,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BridgePalette.line),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(36.dp), shape = CircleShape, color = BridgePalette.lavender) {
                Box(contentAlignment = Alignment.Center) {
                    Text(index, color = BridgePalette.violet, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, color = BridgePalette.navy, style = MaterialTheme.typography.titleSmall)
                Text(body, color = BridgePalette.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SetLightSystemBars(color: androidx.compose.ui.graphics.Color) {
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            val packed = android.graphics.Color.rgb(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
            )
            window.statusBarColor = packed
            window.navigationBarColor = packed
            window.decorView.systemUiVisibility =
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
}

@Composable
private fun SetDarkSystemBars() {
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            window.statusBarColor = android.graphics.Color.rgb(8, 12, 58)
            window.navigationBarColor = android.graphics.Color.rgb(8, 12, 58)
            window.decorView.systemUiVisibility = 0
        }
    }
}

@Composable
private fun BottomWaveArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val wave = Path().apply {
            moveTo(0f, size.height * 0.93f)
            cubicTo(
                size.width * 0.22f, size.height * 0.82f,
                size.width * 0.31f, size.height * 0.88f,
                size.width * 0.47f, size.height * 0.93f,
            )
            cubicTo(
                size.width * 0.66f, size.height * 0.99f,
                size.width * 0.78f, size.height * 0.82f,
                size.width, size.height * 0.85f,
            )
        }
        drawPath(
            path = wave,
            color = ColorForWelcome.blue.copy(alpha = 0.22f),
            style = Stroke(width = size.width * 0.045f, cap = StrokeCap.Round),
        )
        drawPath(
            path = wave,
            brush = Brush.horizontalGradient(
                listOf(ColorForWelcome.pink, ColorForWelcome.blue, BridgePalette.cyan, ColorForWelcome.purpleBlue),
            ),
            style = Stroke(width = size.width * 0.007f, cap = StrokeCap.Round),
        )
        val lowWave = Path().apply {
            moveTo(0f, size.height * 0.96f)
            cubicTo(
                size.width * 0.20f, size.height * 0.91f,
                size.width * 0.30f, size.height * 0.98f,
                size.width * 0.47f, size.height * 0.97f,
            )
            cubicTo(
                size.width * 0.68f, size.height * 0.95f,
                size.width * 0.77f, size.height * 0.91f,
                size.width, size.height * 0.87f,
            )
        }
        drawPath(
            path = lowWave,
            color = ColorForWelcome.purpleBlue.copy(alpha = 0.25f),
            style = Stroke(width = size.width * 0.028f, cap = StrokeCap.Round),
        )
    }
}

private object ColorForWelcome {
    val splashNavy = Color(0xFF050A32)
    val purple = androidx.compose.ui.graphics.Color(0xFF25105C)
    val blue = androidx.compose.ui.graphics.Color(0xFF487AF8)
    val pink = androidx.compose.ui.graphics.Color(0xFFC041F2)
    val purpleBlue = Color(0xFF682CF0)
}
