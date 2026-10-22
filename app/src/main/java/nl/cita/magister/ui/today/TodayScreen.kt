package nl.cita.magister.ui.today

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.ui.components.CircleCheck
import nl.cita.magister.ui.components.CookieFlower
import nl.cita.magister.ui.components.EmptyState
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.components.TodaySkeleton
import nl.cita.magister.ui.grades.LatestGradesStrip
import nl.cita.magister.ui.localDateOf
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.pageFadeSpring
import nl.cita.magister.ui.motion.staggerEnter
import nl.cita.magister.ui.AppClock
import nl.cita.magister.ui.nav.MagsChrome
import nl.cita.magister.ui.nav.MagsPage
import nl.cita.magister.ui.parseInstant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val nl = Locale("nl", "NL")
private val longDay = DateTimeFormatter.ofPattern("EEEE d MMMM", nl)

@Composable
fun TodayScreen(
    profile: StudentProfile?,
    appointments: List<Appointment>,
    homework: List<HomeworkItem>,
    averages: List<SubjectAverage> = emptyList(),
    grades: List<Grade> = emptyList(),
    messages: List<MessagePreview>,
    contentPadding: PaddingValues,
    onOpenPage: (MagsPage) -> Unit,
    onToggleHomework: (HomeworkItem, Boolean) -> Unit = { _, _ -> },
    agendaError: ApiFailure? = null,
    gradesError: ApiFailure? = null,
    messagesError: ApiFailure? = null,
    loading: Boolean = false,
    clockDate: LocalDate = LocalDate.now(AppClock.zone),
    clockTime: LocalTime = LocalTime.now(AppClock.zone),
    offline: Boolean = false,
) {
    val today = clockDate
    val now = clockTime
    val ofDay = remember(appointments, today) {
        appointments.filter { localDateOf(it.startIso) == today }.sortedBy { it.startIso }
    }
    val current = ofDay.firstOrNull { !it.isCancelled && lessonStart(it) <= now && lessonEnd(it) > now }
    val next = ofDay.firstOrNull { !it.isCancelled && lessonStart(it) > now }
    val hero = current ?: next
    val openHw = remember(homework) { homework.filter { !it.isDone }.sortedBy { it.dueIso } }
    val latest = remember(grades) { GradeCalculator.latestEarnedGrades(grades, 5) }
    val unread = remember(messages) { messages.filter { !it.isRead } }
    val showSkeleton = loading && ofDay.isEmpty() && openHw.isEmpty() && latest.isEmpty()
    val listPadding = PaddingValues(
        start = MagsChrome.PageGutter,
        end = MagsChrome.PageGutter,
        top = contentPadding.calculateTopPadding() + 4.dp,
        bottom = contentPadding.calculateBottomPadding() + MagsChrome.ListBottomGap,
    )

    Crossfade(
        targetState = showSkeleton,
        animationSpec = pageFadeSpring(),
        label = "today-load",
        modifier = Modifier.fillMaxSize(),
    ) { skeleton ->
    if (skeleton) {
        Column(
            Modifier.fillMaxSize().padding(listPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PageTitle(
                title = profile?.firstName?.takeIf { it.isNotBlank() }?.let { "Hoi $it" } ?: "Vandaag",
                subtitle = today.format(longDay).replaceFirstChar { it.titlecase(nl) },
            )
            TodaySkeleton()
        }
    } else {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageTitle(
                title = profile?.firstName?.takeIf { it.isNotBlank() }?.let { "Hoi $it" } ?: "Vandaag",
                subtitle = today.format(longDay).replaceFirstChar { it.titlecase(nl) },
            )
        }
        if (offline) {
            item {
                Text(
                    "Offline · toont cache",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("offline-banner"),
                )
            }
        }
        agendaError?.let { item { SectionError(it) } }
        if (hero == null) {
            item {
                EmptyState(
                    title = if (ofDay.isEmpty()) "Vrij" else "Klaar",
                    tag = if (ofDay.isEmpty()) "today-vrij" else "today-klaar",
                    modifier = Modifier.staggerEnter(0, key = "hero"),
                )
            }
        } else {
            item {
                PressScaleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggerEnter(0, key = "hero"),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                    onClick = { onOpenPage(MagsPage.Rooster) },
                ) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CookieFlower(
                            size = 88.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (current != null) "NU" else "VOLGENDE LES",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                hero.displaySubject(),
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Place, null, Modifier, MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(hero.classroom ?: hero.location ?: "–", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                heroTimingLabel(
                                    current = current != null,
                                    startIso = hero.startIso,
                                    endIso = hero.endIso,
                                    nowMillis = AppClock(today, now).millis(),
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("today-hero-timing"),
                            )
                        }
                    }
                }
            }
        }
        if (ofDay.isNotEmpty()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("today-lesson-chips"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ofDay.forEach { lesson ->
                        FilterChip(
                            selected = lesson.id == hero?.id,
                            onClick = { onOpenPage(MagsPage.Rooster) },
                            shape = CircleShape,
                            modifier = Modifier.testTag("today-lesson-chip"),
                            label = { Text(lessonChipLabel(lesson)) },
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Laatste cijfers", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { onOpenPage(MagsPage.Cijfers) }) { Text("Alles") }
            }
        }
        gradesError?.let { item { SectionError(it) } }
        item {
            LatestGradesStrip(grades = latest, onOpen = { _ -> onOpenPage(MagsPage.Cijfers) })
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Huiswerk", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { onOpenPage(MagsPage.Huiswerk) }) { Text("Alles") }
            }
        }
        item {
            if (openHw.isEmpty()) {
                EmptyState(
                    title = "Alles af",
                    tag = "today-homework",
                    modifier = Modifier.staggerEnter(1, key = "hw"),
                )
            } else {
                PressScaleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggerEnter(1, key = "hw")
                        .testTag("today-homework"),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                    onClick = { onOpenPage(MagsPage.Huiswerk) },
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        openHw.take(3).forEach { hw ->
                            HomeworkRow(
                                item = hw,
                                today = today,
                                onToggle = { onToggleHomework(hw, it) },
                            )
                        }
                    }
                }
            }
        }
        messagesError?.let { item { SectionError(it) } }
        if (unread.isNotEmpty()) {
            item {
                PressScaleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggerEnter(2, key = "mail"),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                    onClick = { onOpenPage(MagsPage.Berichten) },
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${unread.size} ongelezen", style = MaterialTheme.typography.titleMedium)
                        unread.take(2).forEach {
                            Text("· ${it.sender} — ${it.subject}", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
    }
    }
}

@Composable
private fun HomeworkRow(
    item: HomeworkItem,
    today: LocalDate,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today-homework-row"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircleCheck(checked = item.isDone, onCheckedChange = onToggle)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.displaySubject(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                homeworkBody(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            SuggestionChip(
                onClick = {},
                label = { Text(dueChipLabel(item.dueIso, today)) },
            )
        }
    }
}

private fun lessonStart(item: Appointment): LocalTime = lessonLocalTime(item.startIso)

private fun lessonEnd(item: Appointment): LocalTime = lessonLocalTime(item.endIso)

private fun lessonLocalTime(iso: String): LocalTime =
    parseInstant(iso)?.atZone(AppClock.zone)?.toLocalTime() ?: LocalTime.MIDNIGHT
