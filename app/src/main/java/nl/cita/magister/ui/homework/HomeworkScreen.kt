package nl.cita.magister.ui.homework

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.ui.AppClock
import nl.cita.magister.ui.components.CircleCheck
import nl.cita.magister.ui.components.ListSkeleton
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.formatWhen
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.staggerEnter
import nl.cita.magister.ui.nav.MagsChrome
import nl.cita.magister.ui.theme.CitaAction
import nl.cita.magister.ui.theme.CitaActionRow
import nl.cita.magister.ui.theme.ToetsAmber
import nl.cita.magister.ui.theme.ToetsOnAmber
import nl.cita.magister.ui.today.homeworkBody
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeworkScreen(
    items: List<HomeworkItem>,
    contentPadding: PaddingValues,
    onToggle: (HomeworkItem, Boolean) -> Unit,
    agendaError: ApiFailure? = null,
    loading: Boolean = false,
    clockDate: LocalDate = LocalDate.now(AppClock.zone),
    snackbarHost: SnackbarHostState? = null,
) {
    var filter by remember { mutableStateOf(HwFilter.Open) }
    var overdueOpen by remember { mutableStateOf(false) }
    val buckets = remember(items, filter, clockDate) { groupHomework(items, clockDate, filter) }
    val scope = rememberCoroutineScope()
    val openCount = items.count { !it.isDone }

    fun markDone(item: HomeworkItem) {
        onToggle(item, true)
        val host = snackbarHost ?: return
        scope.launch {
            val result = host.showSnackbar(
                message = "Klaar",
                actionLabel = "Herstel",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onToggle(item, false)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("huiswerk-lijst"),
        contentPadding = PaddingValues(
            start = MagsChrome.PageGutter,
            end = MagsChrome.PageGutter,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PageTitle("Huiswerk", "$openCount open")
        }
        agendaError?.let { item { SectionError(it) } }
        item {
            CitaActionRow(
                actions = listOf(
                    CitaAction("Open", selected = filter == HwFilter.Open) { filter = HwFilter.Open },
                    CitaAction("Alles", selected = filter == HwFilter.All) { filter = HwFilter.All },
                    CitaAction("Klaar", selected = filter == HwFilter.Done) { filter = HwFilter.Done },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (loading && items.isEmpty()) {
            item { ListSkeleton(rows = 3, contentDescription = "Huiswerk laden") }
        }
        if (buckets.isEmpty() && !loading) {
            item {
                Text(if (agendaError != null) "Geen huiswerk." else "Niets open.")
            }
        }
        buckets.forEach { bucket ->
            when (bucket) {
                is HomeworkBucket.Overdue -> {
                    stickyHeader(key = "verlopen") {
                        OverdueHeader(
                            count = bucket.items.size,
                            expanded = overdueOpen,
                            onToggle = { overdueOpen = !overdueOpen },
                        )
                    }
                    if (overdueOpen) {
                        itemsIndexed(bucket.items, key = { _, it -> "over-${it.id}" }) { index, item ->
                            HomeworkCard(
                                item = item,
                                index = index,
                                onToggle = onToggle,
                                onSwipedDone = { markDone(item) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
                is HomeworkBucket.Day -> {
                    stickyHeader(key = "dag-${bucket.date}") {
                        Text(
                            bucket.label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(top = 8.dp, bottom = 4.dp)
                                .testTag("hw-groep-${bucket.label.lowercase()}"),
                        )
                    }
                    itemsIndexed(bucket.items, key = { _, it -> it.id }) { index, item ->
                        HomeworkCard(
                            item = item,
                            index = index,
                            onToggle = onToggle,
                            onSwipedDone = { markDone(item) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onToggle)
            .padding(top = 8.dp, bottom = 4.dp)
            .testTag("hw-groep-verlopen"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Verlopen ($count)", style = MaterialTheme.typography.titleMedium)
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Inklappen" else "Uitklappen",
        )
    }
}

@Composable
private fun HomeworkCard(
    item: HomeworkItem,
    index: Int,
    onToggle: (HomeworkItem, Boolean) -> Unit,
    onSwipedDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toets = isToets(item.infoType)
    val tone = when {
        toets && !item.isDone -> ToetsAmber.copy(alpha = 0.28f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val dismiss = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !item.isDone) {
                onSwipedDone()
            }
            false
        },
    )
    val peek by animateFloatAsState(
        targetValue = if (dismiss.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0f,
        label = "hw-swipe",
    )
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !item.isDone,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(ToetsAmber.copy(alpha = 0.35f + 0.4f * peek))
                    .padding(end = 22.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ToetsOnAmber,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.7f + 0.4f * peek
                        scaleY = 0.7f + 0.4f * peek
                    },
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .staggerEnter(index)
            .testTag("hw-kaart"),
    ) {
        PressScaleCard(colors = CardDefaults.cardColors(tone), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    CircleCheck(checked = item.isDone, onCheckedChange = { onToggle(item, it) })
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                item.subject,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (toets) {
                                Text(
                                    "Toets",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ToetsOnAmber,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(ToetsAmber)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .testTag("hw-toets"),
                                )
                            }
                        }
                        Text(
                            homeworkBody(item),
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOfNotNull(infoLabel(item), formatWhen(item.dueIso)).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun infoLabel(item: HomeworkItem): String? = when {
    isToets(item.infoType) -> null
    else -> when (item.infoType) {
        nl.cita.magister.model.InfoType.Homework -> "Huiswerk"
        else -> "Taak"
    }
}
