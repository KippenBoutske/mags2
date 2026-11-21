package nl.cita.magister.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

val LocalSafeMode = staticCompositionLocalOf { false }
val LocalDynamicColor = staticCompositionLocalOf { false }

/** Night ink, warm paper, and a clay accent — Cita's visual backbone. */
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
val WarmOnTile = Color(0xFF17191D)
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

/** Licht-warm: papierbeige, inkt, AA op meta-tekst en dock-labels. */
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

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(26.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
    largeIncreased = RoundedCornerShape(40.dp),
)

private val WarmTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.2).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
)

@Composable
fun CitaTheme(
    safeMode: Boolean = false,
    dynamicColor: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WarmDarkScheme
        else -> WarmLightScheme
    }.withReadableContrast(darkTheme)
    CompositionLocalProvider(
        LocalSafeMode provides safeMode,
        LocalDynamicColor provides dynamicColor,
    ) {
        if (safeMode) {
            MaterialTheme(colorScheme = scheme, typography = WarmTypography, content = content)
        } else {
            MaterialExpressiveTheme(
                colorScheme = scheme,
                motionScheme = MotionScheme.expressive(),
                shapes = ExpressiveShapes,
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
