package nl.cita.magister.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import nl.cita.magister.platform.platformDynamicColorScheme

val LocalSafeMode = staticCompositionLocalOf { false }
val LocalDynamicColor = staticCompositionLocalOf { false }

/** Concept 1 — Donker warm */
val WarmSand = Color(0xFFE8D3A0)
val WarmInk = Color(0xFF1A1712)
val WarmBg = Color(0xFF16130F)
val WarmSurface = Color(0xFF221C16)
val WarmSurfaceHigh = Color(0xFF2C241C)
val WarmSurfaceHighest = Color(0xFF352C22)
val WarmOn = Color(0xFFF4E8D0)
val WarmMuted = Color(0xFFD8C9AE)
val GradeGold = Color(0xFFC4A36A)
val GradeSage = Color(0xFF7E8B6A)
val GradeSlate = Color(0xFF6B7C96)
val WarmOnTile = Color(0xFF1A1712)
val ToetsAmber = Color(0xFFC47A4A)
val ToetsOnAmber = Color(0xFF1A1712)

private val LightPaper = Color(0xFFF4EDE3)
private val LightSurface = Color(0xFFFAF6EF)
private val LightSurfaceLow = Color(0xFFF7F1E8)
private val LightSurfaceContainer = Color(0xFFEDE4D4)
private val LightSurfaceHigh = Color(0xFFE8DCC8)
private val LightSurfaceHighest = Color(0xFFDFD2BC)
private val LightMuted = Color(0xFF4F473E)
private val LightPrimary = Color(0xFF5C4A24)
private val LightOnPrimary = Color(0xFFFFF8EC)
private val LightTertiary = Color(0xFF3F4A32)
private val LightError = Color(0xFF9B3B32)
private val LightOutline = Color(0xFF6F6558)

val WarmDarkScheme = darkColorScheme(
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

/** Licht-warm: papierbeige, inkt, AA op meta-tekst en dock-labels. */
val WarmLightScheme = lightColorScheme(
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

private val WarmTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Light,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = 0.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Light,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
)

@Composable
fun CitaTheme(
    safeMode: Boolean = false,
    dynamicColor: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamic = if (dynamicColor) platformDynamicColorScheme(darkTheme) else null
    val scheme = (dynamic ?: if (darkTheme) WarmDarkScheme else WarmLightScheme)
        .withReadableContrast(darkTheme)
    CompositionLocalProvider(
        LocalSafeMode provides safeMode,
        LocalDynamicColor provides dynamicColor,
    ) {
        if (safeMode) {
            MaterialTheme(colorScheme = scheme, typography = WarmTypography, content = content)
        } else {
            MagsExpressiveTheme(
                colorScheme = scheme,
                typography = WarmTypography,
                content = content,
            )
        }
    }
}

/** WCAG AA: 4.5:1 voor tekst, 3:1 voor outline. Material You kan te licht/te donker vallen. */
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
): Color {
    if (contrastRatio(foreground, background) >= minRatio) return foreground
    return fallback
}

private fun contrastRatio(a: Color, b: Color): Float {
    val l1 = a.luminance()
    val l2 = b.luminance()
    val hi = max(l1, l2)
    val lo = min(l1, l2)
    return (hi + 0.05f) / (lo + 0.05f)
}
