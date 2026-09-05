package com.phonesec.broke.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object BrokeColors {
    val Money = Color(0xFF2ECC71)
    val Danger = Color(0xFFE74C3C)
    val Warning = Color(0xFFF39C12)
    val Ink = Color(0xFF0E1116)
    val Surface = Color(0xFF171C24)
    val SurfaceHigh = Color(0xFF212936)
}

private val DarkScheme = darkColorScheme(
    primary = BrokeColors.Money,
    onPrimary = Color(0xFF06210F),
    secondary = BrokeColors.Warning,
    error = BrokeColors.Danger,
    background = BrokeColors.Ink,
    surface = BrokeColors.Surface,
    surfaceVariant = BrokeColors.SurfaceHigh,
    onBackground = Color(0xFFEDF1F7),
    onSurface = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFFA9B4C4),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1B8A4B),
    secondary = Color(0xFFB4740A),
    error = BrokeColors.Danger,
)

@Composable
fun BrokeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}
