package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF041812),
    primaryContainer = Color(0xFF063B2E),
    onPrimaryContainer = EmeraldLight,
    secondary = GoldAccent,
    onSecondary = Color(0xFF1E1400),
    secondaryContainer = Color(0xFF3B2D05),
    onSecondaryContainer = Color(0xFFFFDF88),
    tertiary = ColorTransfer,
    background = ObsidianBg,
    onBackground = Color(0xFFE6EDF5),
    surface = ObsidianSurface,
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = ObsidianBorder,
    error = ColorExpense
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F8EE),
    onPrimaryContainer = Color(0xFF003D2E),
    secondary = GoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3D4),
    onSecondaryContainer = Color(0xFF422E00),
    tertiary = ColorTransfer,
    background = AlabasterBg,
    onBackground = Color(0xFF0F172A),
    surface = AlabasterSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = AlabasterSurfaceElevated,
    onSurfaceVariant = Color(0xFF64748B),
    outline = AlabasterBorder,
    error = ColorExpense
)

@Composable
fun SinaFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SinaFinanceTheme(darkTheme = darkTheme, content = content)
}
