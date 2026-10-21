package nl.cita.magister.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LocalSafeMode = staticCompositionLocalOf { false }

private val CitadelNavy = Color(0xFF1B2A4A)
private val CitadelGold = Color(0xFFF4C36A)
private val CitadelInk = Color(0xFF101828)

private val DarkScheme = darkColorScheme(
    primary = CitadelGold,
    onPrimary = CitadelInk,
    secondary = Color(0xFF9BB4E0),
    tertiary = Color(0xFFFFB4A2),
    background = Color(0xFF0E1626),
    surface = Color(0xFF152036),
    onBackground = Color(0xFFF4F1EA),
    onSurface = Color(0xFFF4F1EA),
)

private val LightFallback = lightColorScheme(
    primary = CitadelNavy,
    onPrimary = Color.White,
    secondary = CitadelGold,
    onSecondary = CitadelInk,
    background = Color(0xFFF4F1EA),
    surface = Color.White,
    onBackground = CitadelInk,
    onSurface = CitadelInk,
)

@Composable
fun CitaTheme(safeMode: Boolean = false, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    // Pixel 10 / Android 16+ (API 36+): skip Expressive beta (graphics-path + new motion)
    // until we have a device log. Crash-safe-mode does the same.
    val stableUi = safeMode || Build.VERSION.SDK_INT >= 36
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !stableUi -> dynamicLightColorScheme(context)
        dark -> DarkScheme
        stableUi -> LightFallback
        else -> runCatching { expressiveLightColorScheme() }.getOrDefault(LightFallback)
    }
    CompositionLocalProvider(LocalSafeMode provides stableUi) {
        if (stableUi) {
            MaterialTheme(colorScheme = scheme, content = content)
        } else {
            val shapes = Shapes(
                extraSmall = RoundedCornerShape(10.dp),
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(36.dp),
                largeIncreased = RoundedCornerShape(36.dp),
            )
            MaterialExpressiveTheme(
                colorScheme = scheme,
                motionScheme = MotionScheme.expressive(),
                shapes = shapes,
                content = content,
            )
        }
    }
}
