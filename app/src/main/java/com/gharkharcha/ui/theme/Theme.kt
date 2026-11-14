package com.gharkharcha.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Slate40,
    secondaryContainer = Slate90,
    tertiary = Brick40,
    tertiaryContainer = Brick90,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal10,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Slate80,
    tertiary = Brick80,
)

/** Colors not covered by Material roles. Negative money uses colorScheme.error. */
@Immutable
data class ExtendedColors(val positive: Color)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors(positive = Positive40) }

@Composable
fun GharKharchaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // minSdk 35, so dynamic color is always available.
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val extended = ExtendedColors(positive = if (darkTheme) Positive80 else Positive40)
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
    }
}
