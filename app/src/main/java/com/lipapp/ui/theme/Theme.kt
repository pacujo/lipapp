package com.lipapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LipCoralDark,
    onPrimary = Color.White,
    primaryContainer = LipCoralPale,
    onPrimaryContainer = Color(0xFF3F0016),
    secondary = MediumPurple,
    onSecondary = Color.White,
    secondaryContainer = PalePurple,
    onSecondaryContainer = DeepPurple,
    tertiary = Color(0xFF7D525E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF31101D),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF1E1A1D),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF1E1A1D),
    surfaceVariant = Color(0xFFF4DEE1),
    onSurfaceVariant = Color(0xFF524346),
    outline = Color(0xFF847376),
    outlineVariant = Color(0xFFD7C1C5),
)

private val DarkColorScheme = darkColorScheme(
    primary = LipCoralLight,
    onPrimary = Color(0xFF670026),
    primaryContainer = Color(0xFF8F1339),
    onPrimaryContainer = LipCoralPale,
    secondary = LightPurple,
    onSecondary = Color(0xFF342A4B),
    secondaryContainer = Color(0xFF533C65),
    onSecondaryContainer = PalePurple,
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF4A2532),
    tertiaryContainer = Color(0xFF643B49),
    onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF1A1018),
    onBackground = Color(0xFFEDE0E4),
    surface = Color(0xFF1A1018),
    onSurface = Color(0xFFEDE0E4),
    surfaceVariant = Color(0xFF524346),
    onSurfaceVariant = Color(0xFFD7C1C5),
    outline = Color(0xFF9F8C8F),
    outlineVariant = Color(0xFF524346),
)

@Composable
fun LipAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
