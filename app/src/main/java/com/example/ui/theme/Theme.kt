package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = SlateSecondary,
    tertiary = GoldLight,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceCard,
    onPrimary = Color(0xFF131B22),
    onSecondary = Color.White,
    onTertiary = Color(0xFF131B22),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    error = RedAccent
)

private val LightColorScheme = lightColorScheme(
    primary = SlateSecondary,
    secondary = GoldPrimary,
    tertiary = SlateDark,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceCard,
    onPrimary = Color.White,
    onSecondary = SlateDark,
    onTertiary = Color.White,
    onBackground = SlateDark,
    onSurface = SlateDark,
    error = RedAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
