package nl.cita.magister.ui.homework

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.ui.components.CircleCheck
import nl.cita.magister.ui.components.ListSkeleton
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.today.homeworkBody
import nl.cita.magister.ui.formatWhen
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.staggerEnter
import nl.cita.magister.ui.nav.MagsChrome
import nl.cita.magister.ui.theme.CitaAction
import nl.cita.magister.ui.theme.CitaActionRow

private enum class HwFilter { Open, Done, All }

@Composable
fun HomeworkScreen(
    items: List<HomeworkItem>,
    contentPadding: PaddingValues,
    onToggle: (HomeworkItem, Boolean) -> Unit,
    agendaError: ApiFailure? = null,
    loading: Boolean = false,
) {
    var filter by remember { mutableStateOf(HwFilter.Open) }
    val visible = when (filter) {
        HwFilter.Open -> items.filter { !it.isDone }
        HwFilter.Done -> items.filter { it.isDone }
        HwFilter.All -> items
    }.sortedBy { it.dueIso }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MagsChrome.PageGutter,
            end = MagsChrome.PageGutter,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PageTitle("Huiswerk", "${items.count { !it.isDone }} open")
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
        if (visible.isEmpty() && !loading) {
            item {
                Text(if (agendaError != null) "Geen huiswerk." else "Niets open.")
            }
        }
        itemsIndexed(visible, key = { _, it -> it.id }) { index, item ->
            val tone = when (item.infoType) {
                InfoType.Test, InfoType.Written, InfoType.Oral -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
            PressScaleCard(
                colors = CardDefaults.cardColors(tone),
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerEnter(index)
                    .animateItem(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Top) {
                        CircleCheck(checked = item.isDone, onCheckedChange = { onToggle(item, it) })
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                item.subject,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                homeworkBody(item),
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                            )
                            Text(
                                listOfNotNull(item.infoType.label(), formatWhen(item.dueIso)).joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun InfoType.label(): String = when (this) {
    InfoType.Homework -> "Huiswerk"
    InfoType.Test -> "Proefwerk"
    InfoType.Written -> "SO / PO"
    InfoType.Oral -> "Mondeling"
    else -> "Taak"
}
