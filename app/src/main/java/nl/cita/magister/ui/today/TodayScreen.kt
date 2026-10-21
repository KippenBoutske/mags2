package nl.cita.magister.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.ui.formatGrade
import nl.cita.magister.ui.formatTime
import nl.cita.magister.ui.localDateOf
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.staggerEnter
import nl.cita.magister.ui.nav.MagsPage
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zone = ZoneId.of("Europe/Amsterdam")
private val nl = Locale("nl", "NL")
private val longDay = DateTimeFormatter.ofPattern("EEEE d MMMM", nl)

@Composable
fun TodayScreen(
    profile: StudentProfile?,
    appointments: List<Appointment>,
    homework: List<HomeworkItem>,
    averages: List<SubjectAverage>,
    messages: List<MessagePreview>,
    contentPadding: PaddingValues,
    onOpenPage: (MagsPage) -> Unit,
) {
    val today = remember { LocalDate.now(zone) }
    val now = remember { LocalTime.now(zone) }
    val ofDay = remember(appointments, today) {
        appointments
            .filter { localDateOf(it.startIso) == today && !it.isCancelled }
            .sortedBy { it.startIso }
    }
    val next = ofDay.firstOrNull { lessonStart(it) >= now } ?: ofDay.lastOrNull()
    val openHw = remember(homework) { homework.filter { !it.isDone }.sortedBy { it.dueIso } }
    val weak = remember(averages) { averages.filter { (it.average ?: 10.0) < 6.0 } }
    val unread = remember(messages) { messages.filter { !it.isRead } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    profile?.firstName?.let { "Hoi $it" } ?: "Vandaag",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    today.format(longDay).replaceFirstChar { it.titlecase(nl) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            PressScaleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerEnter(0),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
                onClick = { onOpenPage(MagsPage.Rooster) },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Volgende les", style = MaterialTheme.typography.labelLarge)
                    if (next == null) {
                        Text("Geen lessen meer vandaag.", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(next.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            listOfNotNull(
                                next.periodFrom?.let { "uur $it" },
                                formatTime(next.startIso),
                                formatTime(next.endIso),
                                next.classroom ?: next.location,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (ofDay.isNotEmpty()) {
            item {
                Text("Rooster", style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(ofDay, key = { _, it -> it.id }) { index, item ->
                PressScaleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggerEnter(index + 1),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                    onClick = { onOpenPage(MagsPage.Rooster) },
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(item.classroom, item.teachers.firstOrNull()).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            listOfNotNull(formatTime(item.startIso), formatTime(item.endIso)).joinToString("–"),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
        item {
            PressScaleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerEnter(ofDay.size + 1),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                onClick = { onOpenPage(MagsPage.Huiswerk) },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Huiswerk", style = MaterialTheme.typography.titleMedium)
                    if (openHw.isEmpty()) {
                        Text("Niets open. Lekker bezig.")
                    } else {
                        Text("${openHw.size} open")
                        openHw.take(3).forEach { hw ->
                            Text("· ${hw.subject} — ${hw.title}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        if (weak.isNotEmpty()) {
            item {
                PressScaleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggerEnter(ofDay.size + 2),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.tertiaryContainer),
                    onClick = { onOpenPage(MagsPage.Cijfers) },
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Aandacht", style = MaterialTheme.typography.titleMedium)
                        weak.take(4).forEach {
                            Text("${it.subjectName}  ${formatGrade(it.average)}")
                        }
                    }
                }
            }
        }
        item {
            PressScaleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerEnter(ofDay.size + 3),
                colors = CardDefaults.cardColors(
                    if (unread.isNotEmpty()) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                onClick = { onOpenPage(MagsPage.Berichten) },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Berichten", style = MaterialTheme.typography.titleMedium)
                    if (unread.isEmpty()) {
                        Text("Geen ongelezen berichten.")
                    } else {
                        Text("${unread.size} ongelezen")
                        unread.take(2).forEach {
                            Text("· ${it.sender} — ${it.subject}", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private fun lessonStart(item: Appointment): LocalTime {
    val iso = item.startIso
    return runCatching { java.time.OffsetDateTime.parse(iso).toLocalTime() }.getOrNull()
        ?: runCatching { java.time.Instant.parse(iso).atZone(zone).toLocalTime() }.getOrNull()
        ?: LocalTime.MIDNIGHT
}
