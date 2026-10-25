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
import androidx.compose.foundation.border
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
import androidx.compose.material3.contentColorFor
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.hapticOnDown
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.ui.today.subjectChipLabel
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
    code: String = "",
    onClick: () -> Unit = {},
) {
    val label = subjectChipLabel(subject, code)
    Column(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .semantics { contentDescription = "$value $subject" }
            .testTag("grade-tile"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val onTile = MaterialTheme.colorScheme.contentColorFor(color)
        Text(
            value,
            color = onTile,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
        )
        Box(
            Modifier
                .clip(CircleShape)
                .background(onTile.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                label,
                color = onTile,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
            )
        }
    }
}

@Composable
fun CircleCheck(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = if (checked) "Klaar" else "Open",
) {
    val next = !checked
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "hw-check",
    )
    val ring = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.primary,
        progress,
    )
    Box(
        modifier
            .size(48.dp)
            .hapticOnDown(if (next) HapticEvent.HomeworkDone else HapticEvent.HomeworkUndo)
            .clickable { onCheckedChange(next) }
            .testTag("hw-checkbox")
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, ring, CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = progress), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (progress > 0.15f) {
                Text(
                    "✓",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = progress),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.6f + 0.4f * progress
                        scaleY = 0.6f + 0.4f * progress
                    },
                )
            }
        }
    }
}

/** Cookie/flower — follows the active scheme (Material You primary). */
@Composable
fun CookieFlower(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 148.dp,
) {
    val spin by rememberInfiniteTransition(label = "cookie").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Restart),
        label = "cookie-spin",
    )
    Canvas(modifier.size(size)) {
        rotate(spin) {
            drawPath(cookiePath(this.size.minDimension / 2f, center), color)
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
