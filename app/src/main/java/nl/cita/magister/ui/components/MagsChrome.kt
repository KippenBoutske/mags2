package nl.cita.magister.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.ui.theme.WarmOnTile
import nl.cita.magister.ui.theme.WarmSand
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PageTitle(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.displayMedium)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SectionError(error: ApiFailure?, modifier: Modifier = Modifier) {
    if (error == null) return
    var open by remember(error) { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(error.friendly(), style = MaterialTheme.typography.bodyMedium)
        TextButton(
            onClick = { open = !open },
            modifier = Modifier.testTag("section-error-details"),
        ) {
            Text(if (open) "Verberg details" else "Details")
        }
        AnimatedVisibility(open) {
            Text(
                "${error.summary()}\n${error.details()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun GradeTile(
    value: String,
    subject: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            color = WarmOnTile,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
        )
        Box(
            Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.28f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(subject, color = WarmOnTile, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

/** Sand-colored cookie/flower — no holes. Slow rotation. */
@Composable
fun CookieFlower(modifier: Modifier = Modifier, color: Color = WarmSand) {
    val spin by rememberInfiniteTransition(label = "cookie").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Restart),
        label = "cookie-spin",
    )
    Canvas(modifier.size(148.dp)) {
        rotate(spin) {
            drawPath(cookiePath(size.minDimension / 2f, center), color)
        }
    }
}

internal fun cookiePath(radius: Float, center: Offset, lobes: Int = 8): Path {
    val path = Path()
    val steps = lobes * 16
    for (i in 0..steps) {
        val t = i / steps.toFloat() * 2f * PI.toFloat()
        val wobble = 0.18f * cos(lobes * t)
        val r = radius * (0.82f + wobble)
        val x = center.x + r * cos(t)
        val y = center.y + r * sin(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

fun minutesUntil(startIso: String?, nowMillis: Long = System.currentTimeMillis()): Int? {
    val instant = nl.cita.magister.ui.parseInstant(startIso) ?: return null
    val delta = ((instant.toEpochMilli() - nowMillis) / 60_000L).toInt()
    return delta
}

fun overLabel(minutes: Int?): String = when {
    minutes == null -> ""
    minutes <= 0 -> "nu"
    minutes < 60 -> "over $minutes min"
    else -> "over ${minutes / 60} u"
}
