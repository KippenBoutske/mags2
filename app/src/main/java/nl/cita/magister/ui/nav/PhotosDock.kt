package nl.cita.magister.ui.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.hapticOnDown
import nl.cita.magister.ui.motion.dockIndicatorSpring
import nl.cita.magister.ui.motion.pressSpring
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Photos-stijl dock: één spring-pill (LowBouncy / MediumLow) over de tabs.
 *
 * Breedte en X gaan via Animatable + `Modifier.layout`, dus geen per-frame
 * recompose. Label/tint-alpha volgt de overlap met die pill in `graphicsLayer`.
 * FontWeight blijft Medium (geen breedte-pop). Press: bounded ripple + 0.96.
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
            size = MagsChrome.FabSize,
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
        val sliding = placed && canSlide
        Box(Modifier.matchParentSize()) {
            if (sliding) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .layout { measurable, constraints ->
                            val w = indicatorW.value.roundToInt().coerceAtLeast(0)
                            val x = indicatorX.value.roundToInt()
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = w,
                                    maxWidth = w,
                                    minHeight = constraints.maxHeight,
                                    maxHeight = constraints.maxHeight,
                                ),
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.placeRelative(x, 0)
                            }
                        }
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
                val pressScale by animateFloatAsState(
                    targetValue = if (pressed) 0.96f else 1f,
                    animationSpec = pressSpring(),
                    label = "dock-press-${tab.page}",
                )
                val bounds = tabBounds[tab.page]
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
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
                        .hapticOnDown(HapticEvent.TabSwitch, enabled = tab.page != selected)
                        .clickable(
                            interactionSource = interaction,
                            indication = ripple(bounded = true),
                        ) {
                            onSelect(tab.page)
                        }
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
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
                    Column(
                        modifier = Modifier
                            .widthIn(min = 52.dp, max = 64.dp)
                            .padding(horizontal = 4.dp)
                            .graphicsLayer {
                                val cover = if (sliding && bounds != null && bounds.width > 0f) {
                                    val left = max(bounds.left, indicatorX.value)
                                    val right = min(bounds.right, indicatorX.value + indicatorW.value)
                                    ((right - left) / bounds.width).coerceIn(0f, 1f)
                                } else {
                                    if (isSelected) 1f else 0f
                                }
                                alpha = 0.72f + 0.28f * cover
                                colorFilter = ColorFilter.tint(
                                    lerp(onPill, onIndicator, cover),
                                    BlendMode.SrcIn,
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White,
                        )
                        Text(
                            tab.label,
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Medium,
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = pressSpring(),
        label = "search-press",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.34f))
            .clip(CircleShape)
            .background(container)
            .hapticOnDown(HapticEvent.TabSwitch)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, radius = size / 2),
            ) {
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
