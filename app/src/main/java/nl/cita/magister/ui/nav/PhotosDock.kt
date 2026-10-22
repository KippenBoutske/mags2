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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.LocalMagsHaptics
import nl.cita.magister.ui.motion.dockIndicatorSpring
import kotlin.math.roundToInt

/**
 * Photos-stijl dock: precies één indicator, geen per-item ripple/selected-laag.
 *
 * Ghost pill (0.1.9): `clip(CircleShape)` + `ripple(bounded)` tekende een
 * persistente state-layer achter Vandaag, én de overlay-indicator kon op een
 * stale offset (x=0) blijven staan. Tabs hebben `indication = null` (pressed
 * is alleen schaal). De indicator leeft in de geselecteerde tab via
 * `matchParentSize` — die kan niet op een andere tab hinterblijven — en een
 * optionele overlay animeert alleen als we een geldige doel-rect hebben.
 * De pill wrapt haar inhoud (geen `weight(1f)`).
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
    val pillColor = MaterialTheme.colorScheme.surfaceContainer
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DockPill(
            tabs = tabs,
            selected = selected,
            pillColor = pillColor,
            onPill = onPill,
            indicatorColor = indicatorColor,
            onIndicator = onIndicator,
            onSelect = onSelect,
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
) {
    val haptics = LocalMagsHaptics.current
    val density = LocalDensity.current
    val tabBounds = remember { mutableStateMapOf<MagsPage, Rect>() }
    val indicatorX = remember { Animatable(0f) }
    val indicatorW = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    var pillCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val target = selected?.let { tabBounds[it] }
    val canSlide = target != null && target.width > 0f &&
        (selected == tabs.firstOrNull()?.page || target.left > 1f)

    LaunchedEffect(selected, target?.left, target?.width, canSlide) {
        val dest = target ?: return@LaunchedEffect
        if (!canSlide) return@LaunchedEffect
        if (!placed) {
            indicatorX.snapTo(dest.left)
            indicatorW.snapTo(dest.width)
            placed = true
            return@LaunchedEffect
        }
        launch { indicatorX.animateTo(dest.left, dockIndicatorSpring()) }
        launch { indicatorW.animateTo(dest.width, dockIndicatorSpring()) }
    }

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .height(MagsChrome.PillHeight)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.34f))
            .clip(CircleShape)
            .background(pillColor)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .onGloballyPositioned { pillCoords = it }
            .testTag("dock-pill"),
    ) {
        val sliding = placed && canSlide && indicatorW.value > 0.5f
        Box(Modifier.matchParentSize()) {
            if (sliding) {
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
        }
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.page == selected
                val interaction = remember(tab.page) { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .fillMaxHeight()
                        .onGloballyPositioned { coords ->
                            val pill = pillCoords ?: return@onGloballyPositioned
                            if (!pill.isAttached || !coords.isAttached) return@onGloballyPositioned
                            val origin = pill.localPositionOf(coords, Offset.Zero)
                            val next = Rect(
                                origin.x,
                                origin.y,
                                origin.x + coords.size.width.toFloat(),
                                origin.y + coords.size.height.toFloat(),
                            )
                            val prev = tabBounds[tab.page]
                            if (prev == null ||
                                kotlin.math.abs(next.left - prev.left) > 0.5f ||
                                kotlin.math.abs(next.width - prev.width) > 0.5f
                            ) {
                                tabBounds[tab.page] = next
                            }
                        }
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                        ) {
                            if (tab.page != selected) haptics.play(HapticEvent.TabSwitch)
                            onSelect(tab.page)
                        }
                        .graphicsLayer {
                            val scale = if (pressed) 0.94f else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .testTag("dock-tab-${tab.page.name}")
                        .semantics {
                            contentDescription = tab.label
                            this.selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected && !sliding) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .background(indicatorColor)
                                .testTag("dock-indicator"),
                        )
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
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
}

@Composable
private fun SearchFab(
    size: androidx.compose.ui.unit.Dp,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    val haptics = LocalMagsHaptics.current
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
                haptics.play(HapticEvent.TabSwitch)
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
