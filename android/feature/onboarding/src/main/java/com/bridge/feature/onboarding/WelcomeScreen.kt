package com.bridge.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    SetDarkSystemBars()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BridgePalette.navy, BridgePalette.ink, ColorForWelcome.purple),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        BottomWaveArtwork(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BridgeLogoMark(Modifier.size(width = 112.dp, height = 90.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Bridge",
                color = BridgePalette.white,
                fontSize = 46.sp,
                lineHeight = 52.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = (-1.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "RIGHT PEOPLE.\nREAL INTENT.\nNO NOISE.",
                color = BridgePalette.white,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(34.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Text("Get started")
            }
            if (onLocalDemo != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onLocalDemo,
                    enabled = !demoLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BridgePalette.white.copy(alpha = 0.58f)),
                ) {
                    if (demoLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Explore local demo", color = BridgePalette.white)
                }
                Text(
                    "DEV BUILD  ·  SYNTHETIC DATA",
                    color = BridgePalette.white.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                demoError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
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
