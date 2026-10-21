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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
 * Google Photos-stijl dock: losse pill + aparte ronde zoekknop.
 * Geen ButtonGroup, geen fillMaxWidth-weights — meet veilig op 320dp.
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
    BoxWithConstraints(modifier.wrapContentWidth(Alignment.CenterHorizontally)) {
        val compact = maxWidth < 360.dp
        val fab = if (compact) MagsChrome.CompactFabSize else MagsChrome.FabSize
        val hPad = if (compact) 8.dp else 12.dp
        val labelSize = if (compact) 12.sp else 14.sp
        val pillColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.94f)
        val onPill = MaterialTheme.colorScheme.onSurface
        val indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.96f)
        val onIndicator = MaterialTheme.colorScheme.onSecondaryContainer

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MagsChrome.DockGap),
        ) {
            DockPill(
                tabs = tabs,
                selected = selected,
                expansion = expansion.coerceIn(0f, 1f),
                compact = compact,
                hPad = hPad,
                labelSize = labelSize,
                pillColor = pillColor,
                onPill = onPill,
                indicatorColor = indicatorColor,
                onIndicator = onIndicator,
                onSelect = onSelect,
            )
            SearchFab(
                size = fab,
                container = pillColor,
                content = onPill,
                onClick = onSearch,
            )
        }
    }
}

@Composable
private fun DockPill(
    tabs: List<DockTab>,
    selected: MagsPage?,
    expansion: Float,
    compact: Boolean,
    hPad: androidx.compose.ui.unit.Dp,
    labelSize: androidx.compose.ui.unit.TextUnit,
    pillColor: Color,
    onPill: Color,
    indicatorColor: Color,
    onIndicator: Color,
    onSelect: (MagsPage) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val xs = remember { mutableStateMapOf<Int, Float>() }
    val widths = remember { mutableStateMapOf<Int, Float>() }
    val indicatorX = remember { Animatable(0f) }
    val indicatorW = remember { Animatable(0f) }
    val selectedIndex = tabs.indexOfFirst { it.page == selected }
    val expanded = expansion > 0.22f

    LaunchedEffect(selectedIndex, xs.toMap(), widths.toMap(), expanded) {
        if (selectedIndex < 0) {
            launch { indicatorW.animateTo(0f, dockIndicatorSpring()) }
            return@LaunchedEffect
        }
        val x = xs[selectedIndex]
        val w = widths[selectedIndex]
        if (x == null || w == null || w <= 0f) return@LaunchedEffect
        launch { indicatorX.animateTo(x, dockIndicatorSpring()) }
        launch { indicatorW.animateTo(w, dockIndicatorSpring()) }
    }

    Box(
        modifier = Modifier
            .height(MagsChrome.PillHeight)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.34f))
            .clip(CircleShape)
            .background(pillColor)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        if (indicatorW.value > 0f) {
            Box(
                Modifier
                    .offset { IntOffset(indicatorX.value.roundToInt(), 0) }
                    .width(with(density) { indicatorW.value.toDp() })
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(indicatorColor),
            )
        }
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = tab.page == selected
                val showTab = expanded || isSelected
                AnimatedVisibility(
                    visible = showTab,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.CenterHorizontally),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.CenterHorizontally),
                ) {
                    val interaction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val parent = coords.parentCoordinates ?: return@onGloballyPositioned
                                val origin = parent.localPositionOf(coords, Offset.Zero)
                                xs[index] = origin.x
                                widths[index] = coords.size.width.toFloat()
                            }
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interaction,
                                indication = ripple(bounded = true),
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onSelect(tab.page)
                            }
                            .padding(horizontal = hPad)
                            .semantics {
                                contentDescription = tab.label
                                this.selected = isSelected
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally(),
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                                tint = if (isSelected) onIndicator else onPill,
                            )
                        }
                        AnimatedVisibility(
                            visible = expansion > 0.45f || !isSelected,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally(),
                        ) {
                            Text(
                                tab.label,
                                color = if (isSelected) onIndicator else onPill.copy(alpha = 0.88f),
                                fontSize = labelSize,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
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
            .semantics { contentDescription = "Zoeken" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}
