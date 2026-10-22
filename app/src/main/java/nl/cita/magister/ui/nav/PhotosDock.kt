package nl.cita.magister.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import nl.cita.magister.ui.motion.dockIndicatorSpring
import kotlin.math.roundToInt

/**
 * Photos-stijl dock: één indicator, label alleen op de geselecteerde tab.
 * 16 dp zijmarge zit op de parent; de zoekknop mag nooit van het scherm af.
 */
@Composable
fun PhotosDock(
    selected: MagsPage?,
    expansion: Float,
    onSelect: (MagsPage) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<DockTab> = defaultDockTabs(),
) {
    val pillColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f)
    val onPill = MaterialTheme.colorScheme.onSurface
    val indicatorColor = MaterialTheme.colorScheme.secondaryContainer
    val onIndicator = MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MagsChrome.DockSide)
            .passHorizontalSwipes()
            .testTag("dock-row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MagsChrome.DockGap),
    ) {
        DockPill(
            tabs = tabs,
            selected = selected,
            pillColor = pillColor,
            onPill = onPill,
            indicatorColor = indicatorColor,
            onIndicator = onIndicator,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        SearchFab(
            size = MagsChrome.PillHeight,
            container = pillColor,
            content = onPill,
            onClick = onSearch,
        )
    }
    @Suppress("UNUSED_VARIABLE")
    val unused = expansion
}

@Composable
private fun DockPill(
    tabs: List<DockTab>,
    selected: MagsPage?,
    pillColor: Color,
    onPill: Color,
    indicatorColor: Color,
    onIndicator: Color,
    onSelect: (MagsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var selectedBounds by remember { mutableStateOf(Rect.Zero) }
    val indicatorX = remember { Animatable(0f) }
    val indicatorW = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }

    LaunchedEffect(selected, selectedBounds) {
        val width = selectedBounds.width
        if (selected == null || width <= 0f) {
            if (placed) launch { indicatorW.animateTo(0f, dockIndicatorSpring()) }
            return@LaunchedEffect
        }
        if (!placed) {
            indicatorX.snapTo(selectedBounds.left)
            indicatorW.snapTo(width)
            placed = true
            return@LaunchedEffect
        }
        launch { indicatorX.animateTo(selectedBounds.left, dockIndicatorSpring()) }
        launch { indicatorW.animateTo(width, dockIndicatorSpring()) }
    }

    Box(
        modifier = modifier
            .height(MagsChrome.PillHeight)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.34f))
            .clip(CircleShape)
            .background(pillColor)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("dock-pill"),
    ) {
        if (indicatorW.value > 0.5f) {
            Box(
                Modifier
                    .offset { IntOffset(indicatorX.value.roundToInt(), 0) }
                    .width(with(density) { indicatorW.value.toDp() })
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(indicatorColor)
                    .testTag("dock-indicator"),
            )
        }
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.page == selected
                val interaction = remember(tab.page) { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .fillMaxHeight()
                        .onGloballyPositioned { coords ->
                            if (!isSelected) return@onGloballyPositioned
                            val parent = coords.parentCoordinates ?: return@onGloballyPositioned
                            val origin = parent.localPositionOf(coords, Offset.Zero)
                            val next = Rect(
                                origin.x,
                                origin.y,
                                origin.x + coords.size.width.toFloat(),
                                origin.y + coords.size.height.toFloat(),
                            )
                            if ((next.left - selectedBounds.left).let { kotlin.math.abs(it) } > 0.5f ||
                                kotlin.math.abs(next.width - selectedBounds.width) > 0.5f
                            ) {
                                selectedBounds = next
                            }
                        }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interaction,
                            indication = ripple(bounded = true),
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onSelect(tab.page)
                        }
                        .padding(horizontal = 8.dp)
                        .testTag("dock-tab-${tab.page.name}")
                        .semantics {
                            contentDescription = tab.label
                            this.selected = isSelected
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) onIndicator else onPill.copy(alpha = 0.88f),
                    )
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
                    ) {
                        Text(
                            tab.label,
                            color = onIndicator,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFab(
    size: androidx.compose.ui.unit.Dp,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.34f))
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, radius = size / 2),
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            }
            .testTag("dock-search")
            .semantics { contentDescription = "Zoeken" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}

private fun Modifier.passHorizontalSwipes(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: continue
            val dx = kotlin.math.abs(change.position.x - change.previousPosition.x)
            val dy = kotlin.math.abs(change.position.y - change.previousPosition.y)
            if (dx > dy && dx > viewConfiguration.touchSlop * 0.4f) {
                event.changes.forEach { it.consume() }
            }
        }
    }
}
