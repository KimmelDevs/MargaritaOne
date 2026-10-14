package com.kimmeldevs.margaritaone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = OnBrandRed,
    primaryContainer = BrandRedContainer,
    onPrimaryContainer = OnBrandRedContainer,
    secondary = BrandBlue,
    onSecondary = OnBrandBlue,
    secondaryContainer = BrandBlueContainer,
    onSecondaryContainer = OnBrandBlueContainer,
    tertiary = BrandSlate,
    onTertiary = OnBrandSlate,
    tertiaryContainer = BrandSlateContainer,
    onTertiaryContainer = OnBrandSlateContainer,
    background = LightBackground,
    surface = LightBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandRedDark,
    onPrimary = OnBrandRedDark,
    primaryContainer = BrandRedContainerDark,
    onPrimaryContainer = OnBrandRedContainerDark,
    secondary = BrandBlueDark,
    onSecondary = OnBrandBlueDark,
    secondaryContainer = BrandBlueContainerDark,
    onSecondaryContainer = OnBrandBlueContainerDark,
    tertiary = BrandSlateDark,
    onTertiary = OnBrandSlateDark,
    tertiaryContainer = BrandSlateContainerDark,
    onTertiaryContainer = OnBrandSlateContainerDark,
    background = DarkBackground,
    surface = DarkBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant
)

/** Brand colours only (no Material You wallpaper colours) so the app always looks like MargaritaOne. */
@Composable
fun MargaritaOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
