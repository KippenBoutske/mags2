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
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

fun <T> magsSpring(
    dampingRatio: Float = 0.78f,
    stiffness: Float = Spring.StiffnessMediumLow,
) = spring<T>(dampingRatio = dampingRatio, stiffness = stiffness)

fun magsFloatSpring(
    dampingRatio: Float = 0.78f,
    stiffness: Float = Spring.StiffnessMediumLow,
) = spring<Float>(dampingRatio = dampingRatio, stiffness = stiffness)

/** Photos-achtige indicator: zacht, een tikje overshoot. */
fun dockIndicatorSpring() = spring<Float>(
    dampingRatio = 0.72f,
    stiffness = 380f,
)

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
fun Modifier.staggerEnter(index: Int, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(index) {
        delay(index.coerceIn(0, 10) * 28L)
        progress.animateTo(1f, magsFloatSpring(dampingRatio = 0.86f, stiffness = 420f))
    }
    return graphicsLayer {
        val t = progress.value
        alpha = t
        translationY = (1f - t) * 16f
    }
}

@Composable
fun PressScaleCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = magsFloatSpring(dampingRatio = 0.65f, stiffness = 500f),
        label = "press-scale",
    )
    val body: @Composable () -> Unit = {
        Box(Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }) { content() }
    }
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = colors,
            shape = shape,
            interactionSource = interaction,
        ) { body() }
    } else {
        Card(modifier = modifier, colors = colors, shape = shape) { body() }
    }
}
