package nl.cita.magister.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.cita.magister.desktop.ui.DesktopFormat
import nl.cita.magister.desktop.ui.DesktopPage
import nl.cita.magister.desktop.ui.DesktopPageTitle
import nl.cita.magister.desktop.ui.ExpressiveCloverShape
import nl.cita.magister.desktop.ui.ExpressiveScallopShape
import nl.cita.magister.desktop.ui.LivePulseDot
import nl.cita.magister.desktop.ui.MagsDesktopController
import nl.cita.magister.desktop.ui.expressiveSheen
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Appointment
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.subject.teacherCode
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayView(controller: MagsDesktopController) {
    val profile by controller.profile.collectAsState()
    val appointments by controller.appointments.collectAsState()
    val averages by controller.averages.collectAsState()
    val latestGrades by controller.latestGrades.collectAsState()
    val homework by controller.homework.collectAsState()
    val messages by controller.messages.collectAsState()

    val today = remember { LocalDate.now(DesktopFormat.zone) }
    val nowMillis = remember { System.currentTimeMillis() }
    val nowInstant = remember(nowMillis) { Instant.ofEpochMilli(nowMillis) }

    val todayLessons = remember(appointments, today) {
        appointments
            .filter { DesktopFormat.localDateOf(it.startIso) == today }
            .sortedBy { it.startIso }
    }

    val activeOrNext: Pair<Appointment, Boolean>? = remember(appointments, nowInstant) {
        val current = appointments.firstOrNull { lesson ->
            if (lesson.isCancelled) return@firstOrNull false
            val start = DesktopFormat.parseInstant(lesson.startIso) ?: return@firstOrNull false
            val end = DesktopFormat.parseInstant(lesson.endIso) ?: return@firstOrNull false
            !nowInstant.isBefore(start) && nowInstant.isBefore(end)
        }
        if (current != null) {
            current to true
        } else {
            val next = appointments
                .asSequence()
                .filter { !it.isCancelled }
                .mapNotNull { lesson -> DesktopFormat.parseInstant(lesson.startIso)?.let { lesson to it } }
                .filter { (_, start) -> !start.isBefore(nowInstant) }
                .minByOrNull { it.second }
                ?.first
            next?.let { it to false }
        }
    }

    val overallAvg = remember(averages) {
        averages.mapNotNull { it.average }.takeIf { it.isNotEmpty() }?.average()
    }
    val openHomework = remember(homework) {
        homework.filter { !it.isDone }.sortedBy { it.dueIso }.take(5)
    }
    val unreadCount = remember(messages) { messages.count { !it.isRead } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        val subtitle = listOfNotNull(
            DesktopFormat.formatDay(today),
            profile?.let { DesktopFormat.profileLine(it.className, it.schoolName) }.takeIf { !it.isNullOrBlank() },
        ).joinToString(" · ")
        DesktopPageTitle(
            title = "Hoi ${profile?.firstName?.ifBlank { "daar" } ?: "daar"}",
            subtitle = subtitle,
        )

        // 2-column desktop dashboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Left column: Hero lesson + today strip + homework
            Column(
                modifier = Modifier.weight(1.25f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                HeroLessonCard(
                    activeOrNext = activeOrNext,
                    nowMillis = nowMillis,
                    onOpenSchedule = { controller.navigate(DesktopPage.Rooster) },
                )

                if (todayLessons.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        todayLessons.forEach { lesson ->
                            val isCurrent = activeOrNext?.first?.id == lesson.id && activeOrNext.second
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                modifier = Modifier.clickable { controller.navigate(DesktopPage.Rooster) },
                            ) {
                                Text(
                                    text = "${DesktopFormat.formatTime(lesson.startIso)} ${lesson.displaySubject()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }

                // Homework preview card
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Huiswerk", style = MaterialTheme.typography.titleLarge)
                            TextButton(onClick = { controller.navigate(DesktopPage.Huiswerk) }) {
                                Text("Alles")
                            }
                        }
                        if (openHomework.isEmpty()) {
                            Text(
                                "Alles af — geen openstaand huiswerk.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            openHomework.forEachIndexed { idx, item ->
                                if (idx > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    IconButton(
                                        onClick = { controller.toggleHomework(item, !item.isDone) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            if (item.isDone) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                            contentDescription = "Afvinken",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                item.displaySubject(),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            if (DesktopFormat.isToets(item.infoType)) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                ) {
                                                    Text(
                                                        "Toets",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            DesktopFormat.homeworkBody(item),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            DesktopFormat.dueChipLabel(item.dueIso, today),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right column: Latest grades + quick summary
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Laatste cijfers", style = MaterialTheme.typography.titleLarge)
                            TextButton(onClick = { controller.navigate(DesktopPage.Cijfers) }) {
                                Text("Alles")
                            }
                        }

                        if (latestGrades.isEmpty()) {
                            Text(
                                "Nog geen recente cijfers.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            latestGrades.take(6).forEachIndexed { index, grade ->
                                val num = grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value)
                                val fail = num != null && num < 5.5
                                val day = DesktopFormat.localDateOf(grade.enteredAt)?.let { DesktopFormat.formatDayShort(it) }
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { controller.navigate(DesktopPage.Cijfers) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Text(
                                        text = DesktopFormat.formatGrade(grade.numericValue, grade.value),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (fail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            grade.displaySubject(),
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        val caption = DesktopFormat.latestGradeCaption(grade)
                                        if (caption.isNotBlank()) {
                                            Text(
                                                caption,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    if (!day.isNullOrBlank()) {
                                        Text(
                                            day,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Expressive Bento counters row (overall average + open homework + unread messages)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickStatCard(
                        label = "Gemiddelde",
                        value = GradeCalculator.formatAverage(overallAvg),
                        highlight = true,
                        modifier = Modifier.weight(1f),
                        onClick = { controller.navigate(DesktopPage.Cijfers) },
                    )
                    QuickStatCard(
                        label = "Huiswerk",
                        value = homework.count { !it.isDone }.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { controller.navigate(DesktopPage.Huiswerk) },
                    )
                    QuickStatCard(
                        label = "Berichten",
                        value = unreadCount.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { controller.navigate(DesktopPage.Berichten) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroLessonCard(
    activeOrNext: Pair<Appointment, Boolean>?,
    nowMillis: Long,
    onOpenSchedule: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .expressiveSheen(alpha = 0.10f)
            .clickable(onClick = onOpenSchedule),
    ) {
        if (activeOrNext == null) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(ExpressiveScallopShape(lobes = 10, depth = 0.12f))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Vrij", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Geen lessen meer gepland op korte termijn.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val (lesson, isCurrent) = activeOrNext
            val timing = DesktopFormat.heroTimingLabel(isCurrent, lesson.startIso, lesson.endIso, nowMillis)
            val room = lesson.classroom?.takeIf { it.isNotBlank() } ?: lesson.location?.takeIf { it.isNotBlank() }
            val teacher = lesson.teacherCode()

            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(ExpressiveScallopShape(lobes = 10, depth = 0.12f))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    val period = lesson.periodFrom?.toString()
                    if (period != null) {
                        Text(
                            text = period,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isCurrent) {
                            LivePulseDot(color = MaterialTheme.colorScheme.primary, size = 7.dp)
                        }
                        Text(
                            text = if (isCurrent) "NU" else "VOLGENDE LES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Text(
                        text = lesson.displaySubject(),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = listOfNotNull(room, teacher).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (timing.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = timing,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = if (highlight) ExpressiveCloverShape else MaterialTheme.shapes.medium,
        color = if (highlight) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = if (highlight) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
