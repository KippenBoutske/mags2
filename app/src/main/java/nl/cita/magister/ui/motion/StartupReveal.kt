package nl.cita.magister.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.LocalMagsHaptics
import nl.cita.magister.ui.components.ExpressiveScallopShape

/**
 * Expressive Material 3 startup intro:
 * 1. Bouncy scalloped flower pops in with rotation + rising haptic
 * 2. Concentric tonal ripples bloom + bold "Mags" wordmark rises with tick
 * 3. Petal iris zoom-out reveal with landing pop
 */
@Composable
fun MagsStartupReveal(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalMagsHaptics.current
    val flowerScale = remember { Animatable(0.2f) }
    val flowerRot = remember { Animatable(-54f) }
    val ring1 = remember { Animatable(0f) }
    val ring2 = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordShift = remember { Animatable(28f) }
    val overlayAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Stage 1: bouncy flower pop + rotation + rising haptic
        haptics.play(HapticEvent.SheetOpen)
        launch {
            flowerScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 380f,
                ),
            )
        }
        launch {
            flowerRot.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 260f,
                ),
            )
        }
        // Stage 2: concentric ripples + bold wordmark
        delay(150)
        haptics.play(HapticEvent.ScrollTick)
        launch {
            ring1.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
        launch {
            delay(80)
            ring2.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
        launch {
            wordAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        }
        launch {
            wordShift.animateTo(
                0f,
                spring(dampingRatio = 0.68f, stiffness = 420f),
            )
        }

        // Stage 3: expressive petal iris reveal + landing pop
        delay(440)
        haptics.play(HapticEvent.CardPress)
        launch {
            wordAlpha.animateTo(0f, tween(160))
        }
        launch {
            flowerRot.animateTo(42f, tween(340, easing = FastOutSlowInEasing))
        }
        launch {
            flowerScale.animateTo(6.2f, tween(340, easing = FastOutSlowInEasing))
        }
        overlayAlpha.animateTo(0f, tween(300, delayMillis = 60, easing = FastOutSlowInEasing))
        onFinished()
    }

    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val onBg = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(50f)
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val maxR = size.minDimension * 0.62f
            if (ring1.value > 0f) {
                drawCircle(
                    color = primaryContainer.copy(alpha = (1f - ring1.value) * 0.65f),
                    radius = maxR * (0.25f + 0.75f * ring1.value),
                    center = center,
                )
            }
            if (ring2.value > 0f) {
                drawCircle(
                    color = tertiaryContainer.copy(alpha = (1f - ring2.value) * 0.55f),
                    radius = maxR * (0.18f + 0.62f * ring2.value),
                    center = center,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .graphicsLayer {
                        scaleX = flowerScale.value
                        scaleY = flowerScale.value
                        rotationZ = flowerRot.value
                    }
                    .clip(ExpressiveScallopShape(lobes = 10, depth = 0.14f))
                    .background(primary),
            )
            Text(
                text = "Mags",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 46.sp,
                    letterSpacing = (-1.6).sp,
                ),
                color = onBg,
                modifier = Modifier.graphicsLayer {
                    alpha = wordAlpha.value
                    translationY = wordShift.value
                },
            )
        }
    }
}
