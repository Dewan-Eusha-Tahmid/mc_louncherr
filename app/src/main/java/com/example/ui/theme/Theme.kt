package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.models.LauncherThemeOption

private val CyberBlueColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = BedrockBlack,
    primaryContainer = Color(0xFF00384D),
    onPrimaryContainer = CyberCyan,
    secondary = LapisBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF152A50),
    tertiary = EmeraldGreen,
    background = BedrockBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = RedstoneCrimson,
    onError = Color.White
)

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = ObsidianDarkPrimary,
    onPrimary = BedrockBlack,
    primaryContainer = Color(0xFF282846),
    onPrimaryContainer = Color(0xFFC7D2FE),
    secondary = Color(0xFFA5B4FC),
    onSecondary = BedrockBlack,
    tertiary = Color(0xFF38BDF8),
    background = Color(0xFF07090E),
    onBackground = TextPrimary,
    surface = Color(0xFF0F121C),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF161A29),
    outline = Color(0xFF282F45),
    error = RedstoneCrimson
)

private val EmeraldGreenColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = BedrockBlack,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF34D399),
    onSecondary = BedrockBlack,
    tertiary = GoldIngot,
    background = Color(0xFF06100D),
    onBackground = TextPrimary,
    surface = Color(0xFF0D1E19),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF142B24),
    outline = Color(0xFF1F4238),
    error = RedstoneCrimson
)

private val RedstoneCrimsonColorScheme = darkColorScheme(
    primary = RedstoneCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5F101A),
    onPrimaryContainer = Color(0xFFFFCDD2),
    secondary = Color(0xFFFF6E40),
    onSecondary = BedrockBlack,
    tertiary = GoldIngot,
    background = Color(0xFF100709),
    onBackground = TextPrimary,
    surface = Color(0xFF1F0E12),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF2E151A),
    outline = Color(0xFF4A1F27),
    error = RedstoneCrimson
)

@Composable
fun MyApplicationTheme(
    themeOption: LauncherThemeOption = LauncherThemeOption.CYBER_BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeOption) {
        LauncherThemeOption.CYBER_BLUE -> CyberBlueColorScheme
        LauncherThemeOption.OBSIDIAN_DARK -> ObsidianDarkColorScheme
        LauncherThemeOption.EMERALD_GREEN -> EmeraldGreenColorScheme
        LauncherThemeOption.REDSTONE_CRIMSON -> RedstoneCrimsonColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
