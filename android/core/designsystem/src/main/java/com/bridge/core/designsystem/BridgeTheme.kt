package com.bridge.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BridgePalette.violet,
    onPrimary = BridgePalette.white,
    secondary = BridgePalette.cyan,
    background = BridgePalette.paper,
    onBackground = BridgePalette.ink,
    surface = BridgePalette.white,
    onSurface = BridgePalette.ink,
    surfaceVariant = BridgePalette.lavender,
    onSurfaceVariant = BridgePalette.muted,
    outline = BridgePalette.line,
    tertiary = BridgePalette.success,
    error = Color(0xFFD64055),
)

private val DarkColors = darkColorScheme(
    primary = BridgePalette.lilac,
    onPrimary = BridgePalette.navy,
    secondary = BridgePalette.cyan,
    background = BridgePalette.navy,
    onBackground = BridgePalette.white,
    surface = BridgePalette.ink,
    onSurface = BridgePalette.white,
    surfaceVariant = BridgePalette.ink,
    onSurfaceVariant = BridgePalette.lilac,
)

private val BridgeShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun BridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BridgeTypography,
        shapes = BridgeShapes,
        content = content,
    )
}
