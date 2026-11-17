package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PaddyForest,
    onPrimary = OnPaddyForest,
    primaryContainer = PaddySageContainer,
    onPrimaryContainer = OnPaddySageContainer,
    secondary = HarvestGold,
    onSecondary = OnHarvestGold,
    secondaryContainer = HarvestGoldContainer,
    onSecondaryContainer = OnHarvestGoldContainer,
    tertiary = TerracottaSoil,
    onTertiary = OnTerracottaSoil,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = RicePaperBackground,
    onBackground = OnRicePaper,
    surface = SageSurface,
    onSurface = OnRicePaper,
    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = OnSageSurfaceVariant,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color.White,
    surfaceContainerLow = SageSurface,
    surfaceContainer = ColorTokens.LightContainer,
    surfaceContainerHigh = SageSurfaceVariant,
    outline = SageOutline,
    outlineVariant = SageOutlineVariant,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = PaddyMintDark,
    onPrimary = OnPaddyMintDark,
    primaryContainer = PaddyForestContainerDark,
    onPrimaryContainer = OnPaddyForestContainerDark,
    secondary = HarvestAmberDark,
    onSecondary = OnHarvestAmberDark,
    secondaryContainer = HarvestContainerDark,
    onSecondaryContainer = OnHarvestContainerDark,
    tertiary = TerracottaDark,
    onTertiary = OnTerracottaDark,
    tertiaryContainer = TerracottaContainerDark,
    onTertiaryContainer = OnTerracottaContainerDark,
    background = NightFieldBackground,
    onBackground = OnNightField,
    surface = NightCanopySurface,
    onSurface = OnNightField,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = OnNightSurfaceVariant,
    surfaceContainerLow = NightCanopySurface,
    surfaceContainer = NightSurfaceVariant,
    surfaceContainerHigh = ColorTokens.DarkContainerHigh,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant
)

private object ColorTokens {
    val LightContainer = androidx.compose.ui.graphics.Color(0xFFECF1E8)
    val DarkContainerHigh = androidx.compose.ui.graphics.Color(0xFF212D26)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RiceLoggerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
