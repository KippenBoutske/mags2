package nl.cita.magister.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive scalloped flower/cookie shape from feat/ui-grades-live-wear.
 */
class ExpressiveScallopShape(
    private val lobes: Int = 10,
    private val depth: Float = 0.12f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val path = Path()
        val steps = lobes * 18
        for (i in 0..steps) {
            val t = i / steps.toFloat() * 2f * PI.toFloat()
            val wobble = depth * cos(lobes * t)
            val r = radius * (1f - depth + wobble)
            val x = center.x + r * cos(t)
            val y = center.y + r * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Material 3 Expressive soft 4-lobe clover squircle shape. */
val ExpressiveCloverShape: Shape = ExpressiveScallopShape(lobes = 4, depth = 0.08f)

/**
 * Bold geometric page title + pill subtitle matching feat/ui-grades-live-wear's PageTitle.
 */
@Composable
fun DesktopPageTitle(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!subtitle.isNullOrBlank()) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Soft, slow-drifting ambient Material You aura behind app screens.
 */
@Composable
fun ExpressiveAuraBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aura")
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "aura-p1",
    )
    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "aura-p2",
    )
    val c1 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
    val c2 = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.16f)
    val c3 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.13f)

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val o1 = Offset(
            x = w * (0.22f + 0.10f * cos(phase1)),
            y = h * (0.16f + 0.06f * sin(phase1)),
        )
        val o2 = Offset(
            x = w * (0.80f + 0.08f * sin(phase2)),
            y = h * (0.34f + 0.08f * cos(phase2)),
        )
        val o3 = Offset(
            x = w * (0.48f + 0.12f * cos(phase2 + 1.2f)),
            y = h * (0.80f + 0.06f * sin(phase1)),
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(c1, Color.Transparent), center = o1, radius = w * 0.55f),
            center = o1,
            radius = w * 0.55f,
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(c2, Color.Transparent), center = o2, radius = w * 0.50f),
            center = o2,
            radius = w * 0.50f,
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(c3, Color.Transparent), center = o3, radius = w * 0.45f),
            center = o3,
            radius = w * 0.45f,
        )
    }
}

/**
 * Pulsing sonar-ring live indicator dot for active lessons.
 */
@Composable
fun LivePulseDot(
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "live-pulse-ring",
    )
    Box(
        modifier = modifier.size(size * 2.2f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val maxR = this.size.minDimension / 2f
            drawCircle(
                color = color.copy(alpha = (1f - pulse) * 0.45f),
                radius = maxR * (0.35f + 0.65f * pulse),
                center = center,
            )
        }
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/**
 * Subtle glossy diagonal sheen sweeping across hero cards.
 */
@Composable
fun Modifier.expressiveSheen(
    color: Color = Color.White,
    alpha: Float = 0.14f,
    durationMs: Int = 4_800,
): Modifier {
    val transition = rememberInfiniteTransition(label = "sheen")
    val progress by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheen-pos",
    )
    return drawWithContent {
        drawContent()
        val x = size.width * progress
        val band = size.width * 0.42f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent),
                start = Offset(x - band, 0f),
                end = Offset(x + band, size.height),
            ),
        )
    }
}

/**
 * Expressive confetti/sparkle burst for high grades (>= 7.5).
 */
@Composable
fun GradeConfettiBurst(
    triggerKey: Any,
    modifier: Modifier = Modifier,
) {
    val progress = remember(triggerKey) { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
    }
    if (progress.value >= 0.99f) return

    val p1 = MaterialTheme.colorScheme.primary
    val p2 = MaterialTheme.colorScheme.tertiary
    val p3 = MaterialTheme.colorScheme.secondary
    val p4 = MaterialTheme.colorScheme.primaryContainer
    val particles = remember(triggerKey) {
        List(22) { i ->
            val angle = (i / 22f) * (2f * PI.toFloat()) + (i % 3) * 0.14f
            val speed = 0.55f + (i % 5) * 0.14f
            val sizeFactor = 0.7f + (i % 4) * 0.25f
            Triple(angle, speed, sizeFactor)
        }
    }
    val palette = listOf(p1, p2, p3, p4)

    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        val fade = (1f - t).coerceIn(0f, 1f)
        val origin = Offset(size.width * 0.5f, size.height * 0.25f)
        val maxDist = size.minDimension * 0.48f
        val gravity = size.height * 0.18f * t * t

        particles.forEachIndexed { i, (angle, speed, sf) ->
            val dist = maxDist * speed * t
            val cx = origin.x + cos(angle) * dist
            val cy = origin.y + sin(angle) * dist + gravity
            val col = palette[i % palette.size].copy(alpha = fade)
            val s = 9f * sf * (1f - 0.35f * t)
            rotate(degrees = t * 240f * if (i % 2 == 0) 1f else -1f, pivot = Offset(cx, cy)) {
                if (i % 2 == 0) {
                    drawRoundRect(
                        color = col,
                        topLeft = Offset(cx - s, cy - s * 0.45f),
                        size = Size(s * 2f, s * 0.9f),
                        cornerRadius = CornerRadius(s * 0.45f, s * 0.45f),
                    )
                } else {
                    drawCircle(color = col, radius = s * 0.65f, center = Offset(cx, cy))
                }
            }
        }
    }
}
