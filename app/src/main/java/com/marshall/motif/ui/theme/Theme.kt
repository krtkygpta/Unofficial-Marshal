package com.marshall.motif.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

/** App color schemes. Dynamic uses Material You; monochromatic is strictly black/white. */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2C2C2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E8EA),
    onPrimaryContainer = Color(0xFF1C1C1E),
    secondary = Color(0xFF5C5C62),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8EA),
    onSecondaryContainer = Color(0xFF1C1C1E),
    tertiary = MarshallGoldDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8C8),
    onTertiaryContainer = Color(0xFF2A2000),
    background = Color(0xFFF2F2F4),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFF2F2F4),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE5E5E8),
    onSurfaceVariant = Color(0xFF636366),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F8F9),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE5E5E8),
    outline = Color(0xFFD1D1D6),
    outlineVariant = Color(0xFFE5E5E8),
    error = MarshallRed,
    onError = Color.White,
    inverseSurface = Color(0xFF2C2C2E),
    inverseOnSurface = Color(0xFFF2F2F4),
    scrim = Color.Black,
)

private val MonochromeLightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color.Black,
    inversePrimary = Color(0xFFCCCCCC),
    secondary = Color(0xFF444444),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5E5),
    onSecondaryContainer = Color.Black,
    tertiary = Color.Black,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5E5E5),
    onTertiaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0xFF444444),
    surfaceTint = Color.Black,
    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    error = Color(0xFF111111),
    onError = Color.White,
    errorContainer = Color(0xFFE5E5E5),
    onErrorContainer = Color.Black,
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFD0D0D0),
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE8E8E8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F1F1),
    surfaceContainerHighest = Color(0xFFE5E5E5),
    // M3 1.4 Fixed roles default to Material purple if omitted.
    primaryFixed = Color(0xFFE5E5E5),
    primaryFixedDim = Color(0xFFCCCCCC),
    onPrimaryFixed = Color.Black,
    onPrimaryFixedVariant = Color(0xFF222222),
    secondaryFixed = Color(0xFFE5E5E5),
    secondaryFixedDim = Color(0xFFCCCCCC),
    onSecondaryFixed = Color.Black,
    onSecondaryFixedVariant = Color(0xFF222222),
    tertiaryFixed = Color(0xFFE5E5E5),
    tertiaryFixedDim = Color(0xFFCCCCCC),
    onTertiaryFixed = Color.Black,
    onTertiaryFixedVariant = Color(0xFF222222),
)

private val MonochromeDarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF303030),
    onPrimaryContainer = Color.White,
    inversePrimary = Color(0xFF333333),
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color.White,
    tertiary = Color.White,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2A2A2A),
    onTertiaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF101010),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF202020),
    onSurfaceVariant = Color(0xFFCCCCCC),
    surfaceTint = Color.White,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    error = Color(0xFFEEEEEE),
    onError = Color.Black,
    errorContainer = Color(0xFF2A2A2A),
    onErrorContainer = Color.White,
    outline = Color(0xFF888888),
    outlineVariant = Color(0xFF333333),
    scrim = Color.Black,
    surfaceBright = Color(0xFF2A2A2A),
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF252525),
    primaryFixed = Color(0xFFE5E5E5),
    primaryFixedDim = Color(0xFFCCCCCC),
    onPrimaryFixed = Color.Black,
    onPrimaryFixedVariant = Color(0xFF222222),
    secondaryFixed = Color(0xFFE5E5E5),
    secondaryFixedDim = Color(0xFFCCCCCC),
    onSecondaryFixed = Color.Black,
    onSecondaryFixedVariant = Color(0xFF222222),
    tertiaryFixed = Color(0xFFE5E5E5),
    tertiaryFixedDim = Color(0xFFCCCCCC),
    onTertiaryFixed = Color.Black,
    onTertiaryFixedVariant = Color(0xFF222222),
)

/** Retained as the basis for the custom dark palette's neutral surfaces. */
private val DarkColors = darkColorScheme(
    primary = MarshallGold,
    onPrimary = Color(0xFF1A1400),
    primaryContainer = Color(0xFF3A3014),
    onPrimaryContainer = MarshallGoldLight,
    secondary = MarshallGoldLight,
    onSecondary = Color(0xFF1A1400),
    secondaryContainer = Color(0xFF3A3014),
    onSecondaryContainer = MarshallCream,
    tertiary = MarshallTeal,
    onTertiary = Color(0xFF00211B),
    tertiaryContainer = Color(0xFF1A3831),
    onTertiaryContainer = Color(0xFFA8F4E2),
    background = MarshallBlack,
    onBackground = MarshallCream,
    surface = MarshallBlackRaised,
    onSurface = MarshallCream,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = MarshallCreamMuted,
    surfaceContainerLowest = MarshallBlack,
    surfaceContainerLow = Color(0xFF101012),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkOutline,
    outlineVariant = Color(0xFF2C2C32),
    error = MarshallRed,
    onError = Color(0xFF2E0A0A),
    inverseSurface = MarshallCream,
    inverseOnSurface = Color(0xFF1C1C1E),
    scrim = Color.Black,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun marshallSegmentedButtonColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primary,
    activeContentColor = MaterialTheme.colorScheme.onPrimary,
    activeBorderColor = MaterialTheme.colorScheme.outline,
    inactiveContainerColor = Color.Transparent,
    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
    inactiveBorderColor = MaterialTheme.colorScheme.outline,
)

/** All shape slots use the same radius so cards, chips, and sheets match. */
val MarshallShapes = Shapes(
    extraSmall = Radius.ShapeSmall,
    small = Radius.Shape,
    medium = Radius.Shape,
    large = Radius.Shape,
    extraLarge = Radius.Shape,
)

enum class ThemeMode { DYNAMIC, MONOCHROMATIC, CUSTOM }

enum class CustomThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Material 3 1.4 added *Fixed roles that default to the purple seed palette.
 * Any scheme we build ourselves has to overwrite them or chips, tonals, and
 * sliders keep leaking that purple into Mono / Custom.
 */
private fun ColorScheme.withoutMaterialPurple(): ColorScheme = copy(
    primaryFixed = primaryContainer,
    primaryFixedDim = primary,
    onPrimaryFixed = onPrimaryContainer,
    onPrimaryFixedVariant = onSurface,
    secondaryFixed = secondaryContainer,
    secondaryFixedDim = secondary,
    onSecondaryFixed = onSecondaryContainer,
    onSecondaryFixedVariant = onSurfaceVariant,
    tertiaryFixed = tertiaryContainer,
    tertiaryFixedDim = tertiary,
    onTertiaryFixed = onTertiaryContainer,
    onTertiaryFixedVariant = onSurfaceVariant,
    surfaceTint = primary,
)

@Composable
fun MarshallTheme(
    themeMode: ThemeMode = ThemeMode.DYNAMIC,
    customThemeMode: CustomThemeMode = CustomThemeMode.SYSTEM,
    accentColor: Int = 0xFFD4AF5A.toInt(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val customDark = when (customThemeMode) {
        CustomThemeMode.SYSTEM -> darkTheme
        CustomThemeMode.LIGHT -> false
        CustomThemeMode.DARK -> true
    }
    val accent = Color(accentColor)
    val accentOn = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val colorScheme = when (themeMode) {
        ThemeMode.MONOCHROMATIC -> {
            val base = if (darkTheme) MonochromeDarkColors else MonochromeLightColors
            base.withoutMaterialPurple()
        }
        ThemeMode.CUSTOM -> if (customDark) {
            darkColorScheme(
                primary = accent,
                onPrimary = accentOn,
                primaryContainer = accent.copy(alpha = 0.28f),
                onPrimaryContainer = accent,
                inversePrimary = accent,
                secondary = accent,
                onSecondary = accentOn,
                secondaryContainer = accent.copy(alpha = 0.22f),
                onSecondaryContainer = accent,
                tertiary = accent,
                onTertiary = accentOn,
                tertiaryContainer = accent.copy(alpha = 0.22f),
                onTertiaryContainer = accent,
                background = Color.Black,
                onBackground = Color.White,
                surface = Color(0xFF101010),
                onSurface = Color.White,
                surfaceVariant = Color(0xFF202020),
                onSurfaceVariant = Color(0xFFCCCCCC),
                surfaceTint = accent,
                inverseSurface = Color.White,
                inverseOnSurface = Color.Black,
                surfaceContainer = Color(0xFF101010),
                surfaceContainerHigh = Color(0xFF181818),
                surfaceContainerHighest = Color(0xFF252525),
                outline = Color(0xFF777777),
                outlineVariant = Color(0xFF333333),
            ).withoutMaterialPurple()
        } else {
            lightColorScheme(
                primary = accent,
                onPrimary = accentOn,
                primaryContainer = accent.copy(alpha = 0.18f),
                onPrimaryContainer = Color.Black,
                inversePrimary = accent,
                secondary = accent,
                onSecondary = accentOn,
                secondaryContainer = accent.copy(alpha = 0.16f),
                onSecondaryContainer = Color.Black,
                tertiary = accent,
                onTertiary = accentOn,
                tertiaryContainer = accent.copy(alpha = 0.16f),
                onTertiaryContainer = Color.Black,
                background = Color.White,
                onBackground = Color.Black,
                surface = Color.White,
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFF1F1F1),
                onSurfaceVariant = Color(0xFF444444),
                surfaceTint = accent,
                inverseSurface = Color.Black,
                inverseOnSurface = Color.White,
                surfaceContainerHigh = Color(0xFFF7F7F7),
                surfaceContainerHighest = Color(0xFFE5E5E5),
                outline = Color(0xFF777777),
                outlineVariant = Color(0xFFD0D0D0),
            ).withoutMaterialPurple()
        }

        ThemeMode.DYNAMIC -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) DarkColors else LightColors
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MarshallTypography,
        shapes = MarshallShapes,
        content = content,
    )
}
