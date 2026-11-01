package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.ThemePreset

fun createLauncherColorScheme(preset: ThemePreset): ColorScheme {
    val primary = Color(preset.primaryHex)
    val secondary = Color(preset.secondaryHex)
    val surface = Color(preset.surfaceHex)
    val background = Color(preset.backgroundHex)

    return darkColorScheme(
        primary = primary,
        onPrimary = if (preset == ThemePreset.MONOCHROME_INK) Color.Black else Color.Black,
        primaryContainer = primary.copy(alpha = 0.2f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.Black,
        secondaryContainer = secondary.copy(alpha = 0.2f),
        onSecondaryContainer = secondary,
        background = background,
        onBackground = Color(0xFFF0F6FC),
        surface = surface,
        onSurface = Color(0xFFF0F6FC),
        surfaceVariant = surface.copy(alpha = 0.7f),
        onSurfaceVariant = Color(0xFF8B949E),
        outline = Color(0xFF30363D),
        outlineVariant = Color(0xFF21262D)
    )
}

@Composable
fun AuraLauncherTheme(
    preset: ThemePreset = ThemePreset.AMOLED_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = createLauncherColorScheme(preset)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
