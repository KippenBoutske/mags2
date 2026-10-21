package nl.cita.magister.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.cita.magister.model.Appointment
import nl.cita.magister.ui.formatDay
import nl.cita.magister.ui.formatTime
import nl.cita.magister.ui.localDateOf
import nl.cita.magister.ui.theme.CitaAction
import nl.cita.magister.ui.theme.CitaActionRow
import java.time.LocalDate

@Composable
fun ScheduleScreen(
    appointments: List<Appointment>,
    contentPadding: PaddingValues,
) {
    val days = remember(appointments) {
        appointments.mapNotNull { localDateOf(it.startIso) }.distinct().sorted()
    }
    var selected by remember(days) { mutableStateOf(days.firstOrNull() ?: LocalDate.now()) }
    val ofDay = appointments
        .filter { localDateOf(it.startIso) == selected }
        .sortedBy { it.startIso }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Rooster", style = MaterialTheme.typography.headlineMedium)
        }
        if (days.isNotEmpty()) {
            item {
                CitaActionRow(
                    actions = days.take(7).map { day ->
                        val label = day.dayOfWeek.name.take(2).lowercase().replaceFirstChar { it.titlecase() } +
                            " ${day.dayOfMonth}"
                        CitaAction(label) { selected = day }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (ofDay.isEmpty()) {
            item { Text("Geen lessen op deze dag.") }
        }
        items(ofDay, key = { it.id }) { item ->
            val colors = when {
                item.isCancelled -> CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)
                else -> CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)
            }
            Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            listOfNotNull(item.periodFrom?.let { "uur $it" }, formatTime(item.startIso), formatTime(item.endIso))
                                .joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if (item.isCancelled) Text("Uitval", color = MaterialTheme.colorScheme.error)
                    }
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(item.classroom, item.location, item.teachers.firstOrNull()).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    item.content?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        item { Text(formatDay(ofDay.firstOrNull()?.startIso), style = MaterialTheme.typography.labelMedium) }
    }
}
