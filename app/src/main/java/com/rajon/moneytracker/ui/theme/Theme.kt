package com.rajon.moneytracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = MintLight,
    onPrimaryContainer = TealDark,
    secondary = MintAccent,
    onSecondary = Color.White,
    background = BackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    error = RedError
)

private val DarkColorScheme = darkColorScheme(
    primary = TealLight,
    onPrimary = TealDark,
    primaryContainer = TealDark,
    onPrimaryContainer = MintLight,
    secondary = MintAccent,
    onSecondary = Color.Black,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    error = RedError
)

@Composable
fun MoneyTrackerTheme(
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
