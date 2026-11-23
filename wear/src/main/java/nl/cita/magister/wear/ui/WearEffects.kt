package nl.cita.magister.wear.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class WearScallopShape(
    private val lobes: Int = 10,
    private val depthFraction: Float = 0.11f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = min(cx, cy) * (1f - depthFraction)
        val amp = min(cx, cy) * depthFraction
        val steps = lobes * 12
        val path = Path()
        for (i in 0..steps) {
            val theta = (2.0 * PI * i) / steps
            val r = baseR + amp * cos(lobes * theta).toFloat()
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun WearAmbientAura(modifier: Modifier = Modifier) {
    val palette = LocalWearPalette.current
    val transition = rememberInfiniteTransition(label = "wear-aura")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wear-aura-drift",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(palette.background)
        val w = size.width
        val h = size.height
        val radius = maxOf(w, h) * 0.72f
        val topCenter = Offset(
            x = w * (0.5f + 0.16f * cos(drift)),
            y = h * (0.14f + 0.08f * sin(drift)),
        )
        val bottomCenter = Offset(
            x = w * (0.5f - 0.18f * cos(drift)),
            y = h * (0.88f - 0.07f * sin(drift)),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.primary.copy(alpha = 0.24f),
                    palette.primaryContainer.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                center = topCenter,
                radius = radius,
            ),
            radius = radius,
            center = topCenter,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.secondary.copy(alpha = 0.16f),
                    Color.Transparent,
                ),
                center = bottomCenter,
                radius = radius * 0.85f,
            ),
            radius = radius * 0.85f,
            center = bottomCenter,
        )
    }
}

@Composable
fun WearLivePulseDot(
    color: Color,
    size: Dp = 7.dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "wear-live-pulse")
    val ringScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 2.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wear-live-scale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wear-live-alpha",
    )
    Box(modifier = modifier.size(size * 2.2f), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                    alpha = ringAlpha
                }
                .clip(CircleShape)
                .background(color),
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}

fun Modifier.wearStaggerEnter(
    index: Int,
    key: Any = Unit,
): Modifier = composed {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay((index.coerceAtMost(8) * 32L))
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 18f
        val s = 0.92f + 0.08f * p
        scaleX = s
        scaleY = s
    }
}

@Composable
fun WearStartupSplash(onFinished: () -> Unit) {
    val palette = LocalWearPalette.current
    val haptics = LocalWearHaptics.current
    val badgeScale = remember { Animatable(0.35f) }
    val badgeRotation = remember { Animatable(-28f) }
    val textAlpha = remember { Animatable(0f) }
    val ripple = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        haptics?.play(WearHapticEvent.Sync)
        launch {
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.56f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            badgeRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        launch {
            ripple.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            )
        }
        delay(120L)
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260),
            )
        }
        delay(620L)
        launch {
            exitScale.animateTo(
                targetValue = 1.12f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            )
        }
        exitAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exitAlpha.value
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished,
            ),
        contentAlignment = Alignment.Center,
    ) {
        WearAmbientAura()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = min(size.width, size.height) * (0.18f + 0.42f * ripple.value)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = (1f - ripple.value) * 0.42f),
                        Color.Transparent,
                    ),
                    radius = r.coerceAtLeast(1f),
                ),
                radius = r.coerceAtLeast(1f),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = badgeScale.value
                        scaleY = badgeScale.value
                        rotationZ = badgeRotation.value
                    }
                    .clip(WearScallopShape(lobes = 10, depthFraction = 0.10f))
                    .background(palette.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "M",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.onPrimary,
                )
            }
            Text(
                text = "Mags",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.onSurface,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = (1f - textAlpha.value) * 8f
                },
            )
        }
    }
}
