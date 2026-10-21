package nl.cita.magister.ui.homework

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import nl.cita.magister.ui.formatWhen

private enum class HwFilter { Open, Done, All }

@Composable
fun HomeworkScreen(
    items: List<HomeworkItem>,
    contentPadding: PaddingValues,
    onToggle: (HomeworkItem, Boolean) -> Unit,
) {
    var filter by remember { mutableStateOf(HwFilter.Open) }
    val visible = when (filter) {
        HwFilter.Open -> items.filter { !it.isDone }
        HwFilter.Done -> items.filter { it.isDone }
        HwFilter.All -> items
    }.sortedBy { it.dueIso }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Huiswerk", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${items.count { !it.isDone }} open · tik aan als je klaar bent",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (visible.isEmpty()) {
                item { Text("Niets in deze lijst. Lekker bezig, of trek naar beneden om te vernieuwen.") }
            }
            items(visible, key = { it.id }) { item ->
                val tone = when (item.infoType) {
                    InfoType.Test, InfoType.Written, InfoType.Oral -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                Card(colors = CardDefaults.cardColors(tone), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.isDone, onCheckedChange = { onToggle(item, it) })
                            Column(Modifier.weight(1f)) {
                                Text(item.subject, style = MaterialTheme.typography.titleMedium)
                                Text(item.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    listOfNotNull(item.infoType.label(), formatWhen(item.dueIso)).joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        item.description?.let {
                            Text(it, modifier = Modifier.padding(start = 48.dp, top = 4.dp))
                        }
                    }
                }
            }
        }
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + 12.dp)
                .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                .zIndex(1f),
            leadingContent = {
                IconButton(onClick = { filter = HwFilter.Open }) {
                    Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = "Open")
                }
            },
            trailingContent = {
                IconButton(onClick = { filter = HwFilter.Done }) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Klaar")
                }
            },
        ) {
            TextButton(onClick = { filter = HwFilter.All }) { Text("Alles") }
            Text(
                when (filter) {
                    HwFilter.Open -> "Open"
                    HwFilter.Done -> "Klaar"
                    HwFilter.All -> "Alles"
                },
            )
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
