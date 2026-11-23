package nl.cita.magister.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.ui.AppClock
import nl.cita.magister.ui.components.CircleCheck
import nl.cita.magister.ui.components.CookieFlower
import nl.cita.magister.ui.components.EmptyState
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.components.TodaySkeleton
import nl.cita.magister.ui.formatGrade
import nl.cita.magister.ui.grades.LatestGradesStrip
import nl.cita.magister.ui.localDateOf
import nl.cita.magister.ui.motion.LivePulseDot
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.expressiveSheen
import nl.cita.magister.ui.motion.pageFadeSpring
import nl.cita.magister.ui.motion.staggerEnter
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
    val hero = if (showUpcomingLesson(current, next, now)) next else current ?: next
    val heroIsCurrent = current != null && hero?.id == current.id
    val openHw = remember(homework) { homework.filter { !it.isDone }.sortedBy { it.dueIso } }
    val latest = remember(grades) { GradeCalculator.latestEarnedGrades(grades, 5) }
    val overallAvg = remember(averages) {
        averages.mapNotNull { it.average }.takeIf { it.isNotEmpty() }?.average()
    }
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
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("offline-banner"),
                        ) {
                            Text(
                                "Offline · toont cache",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
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
                        ExpressiveHeroLessonCard(
                            hero = hero,
                            heroIsCurrent = heroIsCurrent,
                            today = today,
                            now = now,
                            onClick = { onOpenPage(MagsPage.Rooster) },
                            modifier = Modifier.staggerEnter(0, key = "hero"),
                        )
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
                                val selected = lesson.id == hero?.id
                                FilterChip(
                                    selected = selected,
                                    onClick = { onOpenPage(MagsPage.Rooster) },
                                    shape = RoundedCornerShape(if (selected) 16.dp else 24.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                    border = null,
                                    modifier = Modifier.testTag("today-lesson-chip"),
                                    label = {
                                        Text(
                                            lessonChipLabel(lesson),
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggerEnter(1, key = "bento"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PressScaleCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 30.dp,
                            pressedCornerRadius = 18.dp,
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.tertiaryContainer),
                            onClick = { onOpenPage(MagsPage.Cijfers) },
                            haptic = HapticEvent.GradeOpen,
                        ) {
                            Column(
                                Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    if (overallAvg != null) "GEMIDDELDE" else "LAATSTE CIJFER",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                )
                                Text(
                                    when {
                                        overallAvg != null -> formatGrade(overallAvg)
                                        latest.isNotEmpty() -> formatGrade(latest.first().numericValue, latest.first().value)
                                        else -> "–"
                                    },
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    if (averages.isNotEmpty()) "${averages.size} vakken" else "Cijfers bekijken",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        PressScaleCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 30.dp,
                            pressedCornerRadius = 18.dp,
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
                            onClick = { onOpenPage(MagsPage.Huiswerk) },
                            haptic = HapticEvent.TabSwitch,
                        ) {
                            Column(
                                Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    "HUISWERK",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                )
                                Text(
                                    openHw.size.toString(),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    if (openHw.isEmpty()) "Alles af!" else "openstaand",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Laatste cijfers", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { onOpenPage(MagsPage.Cijfers) }) { Text("Alles") }
                    }
                }
                gradesError?.let { item { SectionError(it) } }
                item {
                    LatestGradesStrip(grades = latest, onOpen = { _ -> onOpenPage(MagsPage.Cijfers) })
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Huiswerk", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { onOpenPage(MagsPage.Huiswerk) }) { Text("Alles") }
                    }
                }
                item {
                    if (openHw.isEmpty()) {
                        EmptyState(
                            title = "Alles af",
                            tag = "today-homework",
                            modifier = Modifier.staggerEnter(2, key = "hw"),
                        )
                    } else {
                        PressScaleCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggerEnter(2, key = "hw")
                                .testTag("today-homework"),
                            cornerRadius = 32.dp,
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                            onClick = { onOpenPage(MagsPage.Huiswerk) },
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                .staggerEnter(3, key = "mail"),
                            cornerRadius = 32.dp,
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.tertiaryContainer),
                            onClick = { onOpenPage(MagsPage.Berichten) },
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "${unread.size} ongelezen",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                unread.take(2).forEach {
                                    Text(
                                        "· ${it.sender} — ${it.subject}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
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
private fun ExpressiveHeroLessonCard(
    hero: Appointment,
    heroIsCurrent: Boolean,
    today: LocalDate,
    now: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressScaleCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 38.dp,
        pressedCornerRadius = 22.dp,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
        onClick = onClick,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .expressiveSheen(alpha = 0.12f)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CookieFlower(
                    size = 92.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    hero.periodFrom?.let { "${it}e" } ?: "•",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (heroIsCurrent) {
                        LivePulseDot(
                            color = MaterialTheme.colorScheme.primary,
                            size = 7.dp,
                        )
                    }
                    Text(
                        if (heroIsCurrent) "NU BEZIG" else "VOLGENDE LES",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                AnimatedContent(
                    targetState = hero.displaySubject(),
                    transitionSpec = {
                        (fadeIn(pageFadeSpring()) + scaleIn(initialScale = 0.94f, animationSpec = pageFadeSpring())) togetherWith fadeOut(pageFadeSpring())
                    },
                    label = "today-next-lesson",
                ) { subject ->
                    Text(
                        subject,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("today-hero-subject"),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Text(
                        hero.classroom ?: hero.location ?: "–",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        heroTimingLabel(
                            current = heroIsCurrent,
                            startIso = hero.startIso,
                            endIso = hero.endIso,
                            nowMillis = AppClock(today, now).millis(),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("today-hero-timing"),
                    )
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
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                item.displaySubject(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                homeworkBody(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    dueChipLabel(item.dueIso, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

internal fun showUpcomingLesson(
    current: Appointment?,
    next: Appointment?,
    now: LocalTime,
): Boolean = next != null && current != null && !now.isBefore(next.startLocalTime().minusMinutes(5))

private fun Appointment.startLocalTime(): LocalTime =
    parseInstant(startIso)?.atZone(AppClock.zone)?.toLocalTime() ?: LocalTime.MIDNIGHT

private fun lessonStart(item: Appointment): LocalTime = lessonLocalTime(item.startIso)

private fun lessonEnd(item: Appointment): LocalTime = lessonLocalTime(item.endIso)

private fun lessonLocalTime(iso: String): LocalTime =
    parseInstant(iso)?.atZone(AppClock.zone)?.toLocalTime() ?: LocalTime.MIDNIGHT
