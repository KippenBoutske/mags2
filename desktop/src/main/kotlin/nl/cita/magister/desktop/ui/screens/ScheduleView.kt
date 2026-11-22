package nl.cita.magister.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.cita.magister.desktop.ui.DesktopDaySlot
import nl.cita.magister.desktop.ui.DesktopFormat
import nl.cita.magister.desktop.ui.DesktopPageTitle
import nl.cita.magister.desktop.ui.LivePulseDot
import nl.cita.magister.desktop.ui.MagsDesktopController
import nl.cita.magister.model.Appointment
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.subject.teacherCode
import java.time.Instant
import java.time.LocalDate

@Composable
fun ScheduleView(controller: MagsDesktopController) {
    val appointments by controller.appointments.collectAsState()
    val today = remember { LocalDate.now(DesktopFormat.zone) }
    var weekMonday by remember { mutableStateOf(DesktopFormat.mondayOf(today)) }
    var selectedLesson by remember { mutableStateOf<Appointment?>(null) }

    LaunchedEffect(weekMonday) {
        controller.prefetchWeek(weekMonday)
    }

    val visibleDays = remember(weekMonday, appointments) {
        DesktopFormat.weekVisibleDays(weekMonday, appointments)
    }
    val byDate = remember(appointments) {
        appointments.groupBy { DesktopFormat.localDateOf(it.startIso) }
    }
    val nowInstant = remember { Instant.now() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Header + week navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val endDay = visibleDays.lastOrNull() ?: weekMonday.plusDays(4)
            DesktopPageTitle(
                title = "Rooster",
                subtitle = "${DesktopFormat.formatDayMonth(weekMonday)} – ${DesktopFormat.formatDayMonth(endDay)}",
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { weekMonday = weekMonday.minusWeeks(1) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Vorige week")
                }
                OutlinedButton(
                    onClick = { weekMonday = DesktopFormat.mondayOf(today) },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Vandaag")
                }
                IconButton(onClick = { weekMonday = weekMonday.plusWeeks(1) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Volgende week")
                }
            }
        }

        // Main week grid + optional side detail panel
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                visibleDays.forEach { day ->
                    val isToday = day == today
                    val lessons = byDate[day].orEmpty()
                    val slots = remember(lessons) { DesktopFormat.buildDaySlots(lessons) }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Day header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                DesktopFormat.formatDayOfWeekShort(day),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            )
                            Text(
                                day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            )
                        }

                        if (slots.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.EventBusy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Text(
                                        "Vrij",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                slots.forEach { slot ->
                                    when (slot) {
                                        is DesktopDaySlot.Gap -> {
                                            if (slot.isFreePeriod) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        Text(
                                                            "Tussenuur",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                        Text(
                                                            "${DesktopFormat.formatTime(slot.fromIso)}–${DesktopFormat.formatTime(slot.toIso)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                            } else {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    modifier = Modifier.padding(vertical = 2.dp),
                                                )
                                            }
                                        }

                                        is DesktopDaySlot.Lesson -> {
                                            val lesson = slot.item
                                            val start = DesktopFormat.parseInstant(lesson.startIso)
                                            val end = DesktopFormat.parseInstant(lesson.endIso)
                                            val isCurrent = !lesson.isCancelled &&
                                                start != null && end != null &&
                                                !nowInstant.isBefore(start) && nowInstant.isBefore(end)
                                            val isSelected = selectedLesson?.id == lesson.id

                                            LessonCard(
                                                lesson = lesson,
                                                isCurrent = isCurrent,
                                                isSelected = isSelected,
                                                onClick = { selectedLesson = lesson },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Detail side panel when a lesson is selected
            selectedLesson?.let { lesson ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.width(300.dp).fillMaxHeight(),
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                lesson.displaySubject(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { selectedLesson = null },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Sluiten")
                            }
                        }

                        val dateText = DesktopFormat.localDateOf(lesson.startIso)?.let { DesktopFormat.formatDay(it) }
                        val timeText = "${DesktopFormat.formatTime(lesson.startIso)} – ${DesktopFormat.formatTime(lesson.endIso)}"
                        if (dateText != null) {
                            Text(dateText, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            timeText + (lesson.periodFrom?.let { " · uur $it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (lesson.isCancelled) {
                            StatusBadge("Uitval", isError = true)
                        } else if (lesson.isChanged) {
                            StatusBadge("Gewijzigd", isError = false)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        val room = lesson.classroom?.takeIf { it.isNotBlank() } ?: lesson.location
                        if (!room.isNullOrBlank()) {
                            DetailRow("Lokaal", room)
                        }
                        if (lesson.teachers.isNotEmpty()) {
                            DetailRow("Docent", lesson.teachers.joinToString(", "))
                        }
                        if (lesson.title.isNotBlank() && lesson.title != lesson.displaySubject()) {
                            DetailRow("Titel", lesson.title)
                        }
                        val body = DesktopFormat.stripHtml(lesson.content)
                        if (body.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Text(
                                text = if (DesktopFormat.isToets(lesson.infoType)) "Toetsstof" else "Huiswerk / info",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Appointment,
    isCurrent: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        lesson.isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val fg = when {
        lesson.isCancelled -> MaterialTheme.colorScheme.onErrorContainer
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = bg,
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lesson.periodFrom?.let { hour ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            hour.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(
                    text = lesson.displaySubject(),
                    style = MaterialTheme.typography.titleMedium,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (lesson.isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f),
                )
                if (isCurrent) {
                    LivePulseDot(color = fg, size = 6.dp)
                }
            }
            val room = lesson.classroom?.takeIf { it.isNotBlank() } ?: lesson.location?.takeIf { it.isNotBlank() }
            val meta = listOfNotNull(
                lesson.teacherCode(),
                room,
                "${DesktopFormat.formatTime(lesson.startIso)}–${DesktopFormat.formatTime(lesson.endIso)}",
            ).joinToString(" · ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = fg.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (lesson.isCancelled) {
                Text(
                    "Uitval",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            } else if (lesson.isChanged) {
                Text(
                    "Gewijzigd",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            } else if (!lesson.content.isNullOrBlank()) {
                Text(
                    DesktopFormat.stripHtml(lesson.content),
                    style = MaterialTheme.typography.bodySmall,
                    color = fg.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, isError: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
