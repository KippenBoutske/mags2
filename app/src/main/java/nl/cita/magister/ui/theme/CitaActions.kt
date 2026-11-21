package nl.cita.magister.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

data class CitaAction(val label: String, val selected: Boolean = false, val onClick: () -> Unit)

/**
 * Expressive Material 3 filter/action pills with animated corner morphing and tonal color fill.
 */
@Composable
fun CitaActionRow(
    actions: List<CitaAction>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.forEach { action ->
                val interaction = remember(action.label) { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.94f else 1f,
                    animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
                    label = "action-scale",
                )
                val radius by animateDpAsState(
                    targetValue = when {
                        pressed -> 14.dp
                        action.selected -> 16.dp
                        else -> 24.dp
                    },
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
                    label = "action-radius",
                )
                val bg by animateColorAsState(
                    targetValue = if (action.selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    label = "action-bg",
                )
                val fg by animateColorAsState(
                    targetValue = if (action.selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    label = "action-fg",
                )
                Box(
                    modifier = Modifier
                        .widthIn(min = 48.dp)
                        .heightIn(min = 48.dp)
                        .defaultMinSize(minHeight = 48.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(radius))
                        .background(bg)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = action.onClick,
                        )
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .semantics { this.selected = action.selected },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        action.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = fg,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
