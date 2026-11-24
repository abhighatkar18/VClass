package com.example.vclass.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E6E6),
    onPrimaryContainer = Color.Black,
    inversePrimary = Color.White,
    secondary = Color(0xFF4A4A4A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E6E6),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF565656),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6E6E6),
    onTertiaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF424242),
    surfaceTint = Color.Black,
    inverseSurface = Color(0xFF222222),
    inverseOnSurface = Color(0xFFF5F5F5),
    outline = Color(0xFF686868),
    outlineVariant = Color(0xFFC7C7C7),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDADADA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEEEEEE),
    surfaceContainerHighest = Color(0xFFE6E6E6)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF383838),
    onPrimaryContainer = Color.White,
    inversePrimary = Color.Black,
    secondary = Color(0xFFC7C7C7),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF383838),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFB3B3B3),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF383838),
    onTertiaryContainer = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF252525),
    onSurfaceVariant = Color(0xFFC8C8C8),
    surfaceTint = Color.White,
    inverseSurface = Color(0xFFF0F0F0),
    inverseOnSurface = Color(0xFF222222),
    outline = Color(0xFF929292),
    outlineVariant = Color(0xFF525252),
    surfaceBright = Color(0xFF3A3A3A),
    surfaceDim = Color(0xFF121212),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF191919),
    surfaceContainer = Color(0xFF202020),
    surfaceContainerHigh = Color(0xFF2B2B2B),
    surfaceContainerHighest = Color(0xFF353535)
)

@Composable
fun VclassTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
