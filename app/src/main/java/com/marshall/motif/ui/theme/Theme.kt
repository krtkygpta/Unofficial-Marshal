package com.marshall.motif.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
    secondary = Color(0xFF444444),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0xFF444444),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F1F1),
    surfaceContainerHighest = Color(0xFFE5E5E5),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFD0D0D0),
)

private val MonochromeDarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF303030),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF101010),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF202020),
    onSurfaceVariant = Color(0xFFCCCCCC),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF252525),
    outline = Color(0xFF888888),
    outlineVariant = Color(0xFF333333),
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
        ThemeMode.MONOCHROMATIC -> if (darkTheme) MonochromeDarkColors else MonochromeLightColors
        ThemeMode.CUSTOM -> if (customDark) {
            darkColorScheme(
                primary = accent,
                onPrimary = accentOn,
                primaryContainer = accent.copy(alpha = 0.28f),
                onPrimaryContainer = accent,
                secondary = accent,
                onSecondary = accentOn,
                tertiary = accent,
                onTertiary = accentOn,
                background = Color.Black,
                onBackground = Color.White,
                surface = Color(0xFF101010),
                onSurface = Color.White,
                surfaceVariant = Color(0xFF202020),
                onSurfaceVariant = Color(0xFFCCCCCC),
                surfaceContainer = Color(0xFF101010),
                surfaceContainerHigh = Color(0xFF181818),
                surfaceContainerHighest = Color(0xFF252525),
                outline = Color(0xFF777777),
                outlineVariant = Color(0xFF333333),
            )
        } else {
            lightColorScheme(
                primary = accent,
                onPrimary = accentOn,
                primaryContainer = accent.copy(alpha = 0.18f),
                onPrimaryContainer = Color.Black,
                secondary = accent,
                onSecondary = accentOn,
                tertiary = accent,
                onTertiary = accentOn,
                background = Color.White,
                onBackground = Color.Black,
                surface = Color.White,
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFF1F1F1),
                onSurfaceVariant = Color(0xFF444444),
                surfaceContainerHigh = Color(0xFFF7F7F7),
                surfaceContainerHighest = Color(0xFFE5E5E5),
                outline = Color(0xFF777777),
                outlineVariant = Color(0xFFD0D0D0),
            )
        }

        ThemeMode.DYNAMIC -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) darkColorScheme() else lightColorScheme()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MarshallTypography,
        shapes = MarshallShapes,
        content = content,
    )
}
