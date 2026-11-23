package nl.cita.magister.wear.ui

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

data class WearPalette(
    val primary: Color = Color(0xFFD8835D),
    val primaryContainer: Color = Color(0xFF3A2B22),
    val onPrimary: Color = Color(0xFF17191D),
    val secondary: Color = Color(0xFF7E8B6A),
    val secondaryContainer: Color = Color(0xFF2F3326),
    val background: Color = Color(0xFF000000),
    val surface: Color = Color(0xFF1C2025),
    val surfaceHigh: Color = Color(0xFF252A30),
    val onSurface: Color = Color(0xFFF1F0EB),
    val muted: Color = Color(0xFFC7C5BD),
    val error: Color = Color(0xFFF0B8AA),
    val errorContainer: Color = Color(0xFF5C2B25),
) {
    fun primaryArgb(): Int = primary.toArgb()
    fun primaryContainerArgb(): Int = primaryContainer.toArgb()
    fun onPrimaryArgb(): Int = onPrimary.toArgb()
    fun surfaceArgb(): Int = surface.toArgb()
    fun surfaceHighArgb(): Int = surfaceHigh.toArgb()
    fun onSurfaceArgb(): Int = onSurface.toArgb()
    fun mutedArgb(): Int = muted.toArgb()
    fun backgroundArgb(): Int = background.toArgb()
}

val DefaultWearPalette = WearPalette()

val LocalWearPalette = staticCompositionLocalOf { DefaultWearPalette }

fun resolveWearPalette(context: Context): WearPalette {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return DefaultWearPalette
    return runCatching {
        WearPalette(
            primary = Color(context.getColor(android.R.color.system_accent1_200)),
            primaryContainer = Color(context.getColor(android.R.color.system_accent1_800)),
            onPrimary = Color(context.getColor(android.R.color.system_accent1_900)),
            secondary = Color(context.getColor(android.R.color.system_accent3_200)),
            secondaryContainer = Color(context.getColor(android.R.color.system_accent3_800)),
            background = Color(0xFF000000),
            surface = Color(context.getColor(android.R.color.system_neutral1_900)),
            surfaceHigh = Color(context.getColor(android.R.color.system_neutral1_800)),
            onSurface = Color(context.getColor(android.R.color.system_neutral1_50)),
            muted = Color(context.getColor(android.R.color.system_neutral2_200)),
            error = Color(0xFFF2B8B5),
            errorContainer = Color(0xFF601410),
        )
    }.getOrDefault(DefaultWearPalette)
}

@Composable
fun MagsWearTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val palette = remember(context) { resolveWearPalette(context) }
    val colors = remember(palette) {
        Colors(
            primary = palette.primary,
            primaryVariant = palette.primaryContainer,
            secondary = palette.secondary,
            secondaryVariant = palette.secondaryContainer,
            error = palette.error,
            onPrimary = palette.onPrimary,
            onSecondary = palette.onPrimary,
            onError = palette.onPrimary,
            background = palette.background,
            onBackground = palette.onSurface,
            surface = palette.surface,
            onSurface = palette.onSurface,
            onSurfaceVariant = palette.muted,
        )
    }
    CompositionLocalProvider(LocalWearPalette provides palette) {
        MaterialTheme(
            colors = colors,
            content = content,
        )
    }
}
