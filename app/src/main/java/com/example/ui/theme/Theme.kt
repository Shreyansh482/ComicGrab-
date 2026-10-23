package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = Color.White,
    primaryContainer = DarkGreen,
    onPrimaryContainer = PaleCream,
    secondary = MutedGreen,
    onSecondary = DeepForestDarker,
    secondaryContainer = CardForest,
    onSecondaryContainer = PaleCream,
    tertiary = SageSurface,
    onTertiary = DeepForestDarker,
    background = DeepForestBg,
    onBackground = PaleCream,
    surface = CardForest,
    onSurface = PaleCream,
    surfaceVariant = DarkGreen,
    onSurfaceVariant = SageSurface,
    outline = CardForestBorder,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    onPrimary = PaleCream,
    primaryContainer = SageSurface,
    onPrimaryContainer = DeepForestDarker,
    secondary = MutedGreen,
    onSecondary = Color.White,
    secondaryContainer = PaleCream,
    onSecondaryContainer = DeepForestDarker,
    tertiary = TealAccent,
    onTertiary = Color.White,
    background = PaleCream,
    onBackground = DeepForestDarker,
    surface = Color(0xFFF2F7EC),
    onSurface = DeepForestDarker,
    surfaceVariant = SageSurface,
    onSurfaceVariant = DarkGreen,
    outline = MutedGreen,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ComicGrabTheme(
    darkTheme: Boolean = true, // Default to the signature dark forest theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
