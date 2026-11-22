package nl.cita.magister.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.cita.magister.desktop.data.ThemeMode
import kotlin.math.max
import kotlin.math.min

/** Night ink, warm paper, and a clay accent — Mags' visual backbone. */
val WarmSand = Color(0xFFD8835D)
val WarmInk = Color(0xFF17191D)
val WarmBg = Color(0xFF14171B)
val WarmSurface = Color(0xFF1C2025)
val WarmSurfaceHigh = Color(0xFF252A30)
val WarmSurfaceHighest = Color(0xFF2F353D)
val WarmOn = Color(0xFFF1F0EB)
val WarmMuted = Color(0xFFC7C5BD)
val GradeGold = Color(0xFFC4A36A)
val GradeSage = Color(0xFF7E8B6A)
val GradeSlate = Color(0xFF6B7C96)
val ToetsAmber = Color(0xFFC47A4A)
val ToetsOnAmber = Color(0xFF17191D)

private val LightPaper = Color(0xFFF3EFE7)
private val LightSurface = Color(0xFFFBF9F4)
private val LightSurfaceLow = Color(0xFFF6F3EC)
private val LightSurfaceContainer = Color(0xFFEAE5DB)
private val LightSurfaceHigh = Color(0xFFE3DDD2)
private val LightSurfaceHighest = Color(0xFFDAD3C7)
private val LightMuted = Color(0xFF504A43)
private val LightPrimary = Color(0xFF9E432B)
private val LightOnPrimary = Color(0xFFFFF8F3)
private val LightTertiary = Color(0xFF365649)
private val LightError = Color(0xFF9B3B32)
private val LightOutline = Color(0xFF6F6558)

internal val WarmDarkScheme = darkColorScheme(
    primary = WarmSand,
    onPrimary = WarmInk,
    primaryContainer = Color(0xFF3A3226),
    onPrimaryContainer = WarmSand,
    secondary = Color(0xFFDCCDB0),
    onSecondary = WarmInk,
    secondaryContainer = Color(0xFF3F362B),
    onSecondaryContainer = WarmSand,
    tertiary = GradeSage,
    onTertiary = WarmInk,
    tertiaryContainer = Color(0xFF2F3326),
    onTertiaryContainer = Color(0xFFD7E0C4),
    background = WarmBg,
    onBackground = WarmOn,
    surface = WarmSurface,
    onSurface = WarmOn,
    surfaceVariant = WarmSurfaceHigh,
    onSurfaceVariant = WarmMuted,
    surfaceContainerLowest = Color(0xFF100E0B),
    surfaceContainerLow = Color(0xFF1C1813),
    surfaceContainer = WarmSurface,
    surfaceContainerHigh = WarmSurfaceHigh,
    surfaceContainerHighest = WarmSurfaceHighest,
    outline = Color(0xFF8A7B64),
    error = Color(0xFFF0B8AA),
    onError = WarmInk,
    errorContainer = Color(0xFF7A3A32),
    onErrorContainer = Color(0xFFFFEDEB),
)

internal val WarmLightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = WarmSand,
    onPrimaryContainer = WarmInk,
    secondary = LightMuted,
    onSecondary = LightOnPrimary,
    secondaryContainer = Color(0xFFD9C9AE),
    onSecondaryContainer = WarmInk,
    tertiary = LightTertiary,
    onTertiary = LightOnPrimary,
    tertiaryContainer = Color(0xFFD7E0C4),
    onTertiaryContainer = Color(0xFF2A3320),
    background = LightPaper,
    onBackground = WarmInk,
    surface = LightSurface,
    onSurface = WarmInk,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightMuted,
    surfaceContainerLowest = Color(0xFFFFFCF7),
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightOutline,
    error = LightError,
    onError = Color(0xFFFFF8F6),
    errorContainer = Color(0xFFF5D0C8),
    onErrorContainer = Color(0xFF3F1210),
)

private val DesktopShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val DesktopTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Light,
        fontSize = 44.sp,
        lineHeight = 50.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        lineHeight = 42.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun MagsDesktopTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val scheme = (if (darkTheme) WarmDarkScheme else WarmLightScheme).withReadableContrast(darkTheme)
    MaterialTheme(
        colorScheme = scheme,
        shapes = DesktopShapes,
        typography = DesktopTypography,
        content = content,
    )
}

internal fun ColorScheme.withReadableContrast(dark: Boolean): ColorScheme {
    val ink = if (dark) WarmOn else WarmInk
    val muted = if (dark) WarmMuted else LightMuted
    val line = if (dark) Color(0xFF8A7B64) else LightOutline
    val onPill = if (dark) WarmSand else WarmInk
    val card = surfaceContainerHigh
    return copy(
        onBackground = readableOn(onBackground, background, ink),
        onSurface = readableOn(onSurface, card, ink),
        onSurfaceVariant = readableOn(onSurfaceVariant, card, muted, minRatio = 4.5f),
        outline = readableOn(outline, background, line, minRatio = 3f),
        onSecondaryContainer = readableOn(onSecondaryContainer, secondaryContainer, onPill),
        onPrimary = readableOn(onPrimary, primary, if (dark) WarmInk else LightOnPrimary),
        onPrimaryContainer = readableOn(onPrimaryContainer, primaryContainer, ink),
    )
}

private fun readableOn(
    foreground: Color,
    background: Color,
    fallback: Color,
    minRatio: Float = 4.5f,
): Color = if (contrastRatio(foreground, background) >= minRatio) foreground else fallback

private fun contrastRatio(a: Color, b: Color): Float {
    val l1 = a.luminance()
    val l2 = b.luminance()
    val hi = max(l1, l2)
    val lo = min(l1, l2)
    return (hi + 0.05f) / (lo + 0.05f)
}
