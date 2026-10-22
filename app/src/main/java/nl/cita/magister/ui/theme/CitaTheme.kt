package nl.cita.magister.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

private val WarmDarkScheme = darkColorScheme(
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

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
    largeIncreased = RoundedCornerShape(36.dp),
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
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (androidx.compose.foundation.isSystemInDarkTheme()) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        else -> WarmDarkScheme
    }
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
