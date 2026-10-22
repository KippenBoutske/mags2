package nl.cita.magister.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.hapticOnDown

fun <T> magsSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMediumLow,
) = spring<T>(dampingRatio = dampingRatio, stiffness = stiffness)

fun magsFloatSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMediumLow,
) = spring<Float>(dampingRatio = dampingRatio, stiffness = stiffness)

/** Fade-through / shared-axis: ~250 ms settle, geen bounce. */
fun pageFadeSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** Navbar-pill: LowBouncy + MediumLow, geen layout-jump. */
fun dockIndicatorSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

fun pressSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

class StaggerGate {
    private val seen = HashSet<Any>()
    fun first(key: Any): Boolean = seen.add(key)
}

val LocalStaggerGate = staticCompositionLocalOf<StaggerGate?> { null }

@Composable
fun StaggerScope(content: @Composable () -> Unit) {
    val gate = remember { StaggerGate() }
    CompositionLocalProvider(LocalStaggerGate provides gate, content = content)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun magsSharedBounds(
    key: String,
    scope: SharedTransitionScope?,
    visibility: AnimatedVisibilityScope?,
): Modifier {
    if (scope == null || visibility == null) return Modifier
    with(scope) {
        return Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> magsSpring() },
        )
    }
}

@Composable
fun Modifier.staggerEnter(
    index: Int,
    key: Any? = null,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    val gate = LocalStaggerGate.current
    val id = key ?: index
    val play = remember(id) { gate?.first(id) ?: true }
    val progress = remember { Animatable(if (play) 0f else 1f) }
    val shiftPx = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(play, id) {
        if (!play) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        delay(index.coerceIn(0, 8) * 36L)
        progress.animateTo(1f, magsFloatSpring())
    }
    return graphicsLayer {
        val t = progress.value
        alpha = t
        translationY = (1f - t) * shiftPx
    }
}

@Composable
fun PressScaleCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    onClick: (() -> Unit)? = null,
    haptic: HapticEvent? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = pressSpring(),
        label = "press-scale",
    )
    val shape = MaterialTheme.shapes.large
    val cardModifier = modifier
        .then(if (haptic != null) Modifier.hapticOnDown(haptic) else Modifier)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    Card(
        onClick = onClick ?: {},
        modifier = cardModifier,
        colors = colors,
        shape = shape,
        interactionSource = interaction,
    ) { content() }
}
