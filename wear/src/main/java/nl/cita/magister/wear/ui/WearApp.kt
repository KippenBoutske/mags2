package nl.cita.magister.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import kotlinx.coroutines.delay
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.parse.stripHtml
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.wear.WearLessonFocus
import nl.cita.magister.wear.WearScheduleHelper
import nl.cita.magister.wear.WearSyncPayload
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private object WearRoute {
    const val Home = "home"
    const val Schedule = "schedule"
    const val Grades = "grades"
    const val Homework = "homework"
}

private val nlLocale = Locale("nl", "NL")
private val shortDateFmt = DateTimeFormatter.ofPattern("EEE d MMM", nlLocale)

@Composable
fun MagsWearApp(
    payload: WearSyncPayload?,
    syncing: Boolean,
    onRequestSync: () -> Unit,
    onLoadDemo: () -> Unit,
) {
    MagsWearTheme {
        ProvideWearHaptics {
            var now by remember { mutableStateOf(Instant.now()) }
            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(30_000L)
                    now = Instant.now()
                }
            }
            val navController = rememberSwipeDismissableNavController()
            Box(modifier = Modifier.fillMaxSize()) {
                WearAmbientAura()
                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = WearRoute.Home,
                ) {
                    composable(WearRoute.Home) {
                        WearHomeScreen(
                            payload = payload,
                            now = now,
                            syncing = syncing,
                            onOpenSchedule = { navController.navigate(WearRoute.Schedule) },
                            onOpenGrades = { navController.navigate(WearRoute.Grades) },
                            onOpenHomework = { navController.navigate(WearRoute.Homework) },
                            onRequestSync = onRequestSync,
                            onLoadDemo = onLoadDemo,
                        )
                    }
                    composable(WearRoute.Schedule) {
                        WearScheduleScreen(
                            appointments = payload?.appointments.orEmpty(),
                            now = now,
                        )
                    }
                    composable(WearRoute.Grades) {
                        WearGradesScreen(
                            latestGrades = payload?.latestGrades.orEmpty(),
                            averages = payload?.averages.orEmpty(),
                        )
                    }
                    composable(WearRoute.Homework) {
                        WearHomeworkScreen(
                            homework = payload?.openHomework.orEmpty(),
                        )
                    }
                }
                if (showSplash) {
                    WearStartupSplash(onFinished = { showSplash = false })
                }
            }
        }
    }
}

@Composable
private fun WearHomeScreen(
    payload: WearSyncPayload?,
    now: Instant,
    syncing: Boolean,
    onOpenSchedule: () -> Unit,
    onOpenGrades: () -> Unit,
    onOpenHomework: () -> Unit,
    onRequestSync: () -> Unit,
    onLoadDemo: () -> Unit,
) {
    val palette = LocalWearPalette.current
    val haptics = LocalWearHaptics.current
    val listState = rememberScalingLazyListState()
    val focus = remember(payload, now) {
        payload?.let { WearScheduleHelper.currentOrNextLesson(it.appointments, now) }
    }
    val today = remember(now) { now.atZone(WearScheduleHelper.defaultZone).toLocalDate() }
    val todayCount = remember(payload, today) {
        payload?.let { WearScheduleHelper.lessonsForDate(it.appointments, today).size } ?: 0
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .wearScrollRatchet(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                ListHeader {
                    Text(
                        text = payload?.studentName?.let { "Hoi $it" } ?: "Mags",
                        style = MaterialTheme.typography.title3,
                        color = palette.onSurface,
                    )
                }
            }
            if (payload == null) {
                item {
                    TitleCard(
                        onClick = {
                            haptics?.play(WearHapticEvent.Sync)
                            onRequestSync()
                        },
                        title = { Text("Wachten op telefoon", color = palette.onSurface) },
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = palette.surfaceHigh,
                            endBackgroundColor = palette.surface,
                        ),
                    ) {
                        Text(
                            "Open Mags op je telefoon om rooster en cijfers te synchroniseren.",
                            style = MaterialTheme.typography.caption2,
                            color = palette.muted,
                        )
                    }
                }
                item {
                    WearNavChip(
                        label = if (syncing) "Synchroniseren…" else "Vernieuw",
                        icon = Icons.Outlined.Refresh,
                        onClick = onRequestSync,
                        haptic = WearHapticEvent.Sync,
                    )
                }
                item {
                    Chip(
                        onClick = {
                            haptics?.play(WearHapticEvent.Click)
                            onLoadDemo()
                        },
                        label = { Text("Voorbeelddata", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = ChipDefaults.secondaryChipColors(
                            backgroundColor = palette.surface,
                            contentColor = palette.onSurface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item {
                    HeroLessonCard(
                        focus = focus,
                        todayCount = todayCount,
                        onClick = onOpenSchedule,
                    )
                }
                item {
                    WearNavChip(
                        label = "Rooster",
                        secondaryLabel = if (todayCount == 1) "1 les vandaag" else "$todayCount lessen vandaag",
                        icon = Icons.Outlined.CalendarToday,
                        onClick = onOpenSchedule,
                    )
                }
                item {
                    val latest = payload.latestGrades.firstOrNull()
                    WearNavChip(
                        label = "Cijfers",
                        secondaryLabel = latest?.let {
                            "${it.displaySubject()} · ${GradeCalculator.formatGradeValue(it.numericValue, it.value)}"
                        } ?: "${payload.averages.size} vakken",
                        icon = Icons.Outlined.Grade,
                        onClick = onOpenGrades,
                    )
                }
                item {
                    val openCount = payload.openHomework.size
                    WearNavChip(
                        label = "Huiswerk",
                        secondaryLabel = if (openCount == 0) "Alles af" else "$openCount open",
                        icon = Icons.Outlined.Checklist,
                        onClick = onOpenHomework,
                    )
                }
                item {
                    WearNavChip(
                        label = if (syncing) "Synchroniseren…" else "Vernieuw",
                        icon = Icons.Outlined.Refresh,
                        onClick = onRequestSync,
                        subtle = true,
                        haptic = WearHapticEvent.Sync,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroLessonCard(
    focus: WearLessonFocus?,
    todayCount: Int,
    onClick: () -> Unit,
) {
    val palette = LocalWearPalette.current
    val haptics = LocalWearHaptics.current
    if (focus == null) {
        TitleCard(
            onClick = {
                haptics?.play(WearHapticEvent.Click)
                onClick()
            },
            title = { Text(if (todayCount == 0) "Vrij vandaag" else "Klaar voor vandaag", color = palette.onSurface) },
            backgroundPainter = CardDefaults.cardBackgroundPainter(
                startBackgroundColor = palette.surfaceHigh,
                endBackgroundColor = palette.surface,
            ),
        ) {
            Text(
                "Tik om je rooster te bekijken",
                style = MaterialTheme.typography.caption2,
                color = palette.muted,
            )
        }
        return
    }
    val room = focus.lesson.classroom ?: focus.lesson.location
    val countdown = WearScheduleHelper.countdownLabel(focus)
    TitleCard(
        onClick = {
            haptics?.play(WearHapticEvent.Click)
            onClick()
        },
        time = {
            Text(
                countdown,
                style = MaterialTheme.typography.caption2.copy(fontWeight = FontWeight.SemiBold),
                color = palette.primary,
            )
        },
        title = {
            Text(
                focus.lesson.displaySubject(),
                color = palette.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = palette.primaryContainer,
            endBackgroundColor = palette.surface,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (focus.isCurrent) {
                    WearLivePulseDot(color = palette.primary, size = 6.dp)
                }
                Text(
                    if (focus.isCurrent) "NU · tot ${focus.endLabel}" else "VOLGENDE · ${focus.startLabel}",
                    style = MaterialTheme.typography.caption3.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.primary,
                )
            }
            if (!room.isNullOrBlank()) {
                Text(
                    "Lokaal $room",
                    style = MaterialTheme.typography.caption2,
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WearScheduleScreen(
    appointments: List<Appointment>,
    now: Instant,
) {
    val palette = LocalWearPalette.current
    val listState = rememberScalingLazyListState()
    val today = remember(now) { now.atZone(WearScheduleHelper.defaultZone).toLocalDate() }
    val tomorrow = remember(today) { today.plusDays(1) }
    val todayLessons = remember(appointments, today) { WearScheduleHelper.lessonsForDate(appointments, today) }
    val tomorrowLessons = remember(appointments, tomorrow) { WearScheduleHelper.lessonsForDate(appointments, tomorrow) }
    val currentId = remember(appointments, now) {
        WearScheduleHelper.currentOrNextLesson(appointments, now)?.takeIf { it.isCurrent }?.lesson?.id
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .wearScrollRatchet(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                ListHeader {
                    Text("Vandaag", style = MaterialTheme.typography.title3, color = palette.onSurface)
                }
            }
            if (todayLessons.isEmpty()) {
                item {
                    Text(
                        "Geen lessen vandaag",
                        style = MaterialTheme.typography.caption1,
                        color = palette.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                itemsIndexed(todayLessons, key = { _, it -> "today-${it.id}" }) { index, lesson ->
                    WearLessonRow(
                        lesson = lesson,
                        isCurrent = lesson.id == currentId,
                        modifier = Modifier.wearStaggerEnter(index, key = "today-${lesson.id}"),
                    )
                }
            }
            if (tomorrowLessons.isNotEmpty()) {
                item {
                    ListHeader {
                        Text(
                            "Morgen · ${tomorrow.format(shortDateFmt)}",
                            style = MaterialTheme.typography.caption1,
                            color = palette.muted,
                        )
                    }
                }
                itemsIndexed(tomorrowLessons, key = { _, it -> "tomorrow-${it.id}" }) { index, lesson ->
                    WearLessonRow(
                        lesson = lesson,
                        isCurrent = false,
                        modifier = Modifier.wearStaggerEnter(index, key = "tomorrow-${lesson.id}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun WearLessonRow(
    lesson: Appointment,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalWearPalette.current
    val start = WearScheduleHelper.formatTime(lesson.startIso)
    val end = WearScheduleHelper.formatTime(lesson.endIso)
    val room = lesson.classroom ?: lesson.location
    val bg = when {
        lesson.isCancelled -> palette.errorContainer
        isCurrent -> palette.primaryContainer
        else -> palette.surface
    }
    val subtitle = listOfNotNull(
        "$start–$end".takeIf { start.isNotBlank() },
        room,
        if (lesson.isCancelled) "Uitval" else if (lesson.isChanged) "Gewijzigd" else null,
    ).joinToString(" · ")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(if (isCurrent) WearScallopShape(lobes = 8, depthFraction = 0.09f) else RoundedCornerShape(10.dp))
                    .background(if (isCurrent) palette.primary else palette.surfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    lesson.periodFrom?.toString() ?: "–",
                    style = MaterialTheme.typography.caption2.copy(fontWeight = FontWeight.Bold),
                    color = if (isCurrent) palette.onPrimary else palette.onSurface,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (isCurrent) {
                        WearLivePulseDot(color = palette.primary, size = 5.dp)
                    }
                    Text(
                        lesson.displaySubject(),
                        style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.SemiBold),
                        color = if (lesson.isCancelled) palette.error else palette.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.caption3,
                    color = if (lesson.isCancelled) palette.error.copy(alpha = 0.85f) else palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WearGradesScreen(
    latestGrades: List<Grade>,
    averages: List<SubjectAverage>,
) {
    val palette = LocalWearPalette.current
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .wearScrollRatchet(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                ListHeader {
                    Text("Laatste cijfers", style = MaterialTheme.typography.title3, color = palette.onSurface)
                }
            }
            if (latestGrades.isEmpty()) {
                item {
                    Text(
                        "Nog geen cijfers",
                        style = MaterialTheme.typography.caption1,
                        color = palette.muted,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                itemsIndexed(latestGrades, key = { _, it -> "g-${it.id}-${it.columnId}" }) { index, grade ->
                    WearGradeRow(
                        title = grade.displaySubject(),
                        subtitle = listOfNotNull(
                            grade.columnHeading?.takeIf { it.isNotBlank() },
                            GradeCalculator.formatWeight(grade.weight),
                        ).joinToString(" · "),
                        value = grade.numericValue,
                        label = grade.value,
                        modifier = Modifier.wearStaggerEnter(index, key = "g-${grade.id}-${grade.columnId}"),
                    )
                }
            }
            if (averages.isNotEmpty()) {
                item {
                    ListHeader {
                        Text("Per vak", style = MaterialTheme.typography.caption1, color = palette.muted)
                    }
                }
                itemsIndexed(averages, key = { _, it -> "avg-${it.subjectId}" }) { index, avg ->
                    WearGradeRow(
                        title = avg.displaySubject(),
                        subtitle = if (avg.gradesCounted == 1) "1 cijfer" else "${avg.gradesCounted} cijfers",
                        value = avg.average,
                        label = avg.averageLabel,
                        modifier = Modifier.wearStaggerEnter(index, key = "avg-${avg.subjectId}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun WearGradeRow(
    title: String,
    subtitle: String,
    value: Double?,
    label: String?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalWearPalette.current
    val numeric = value ?: GradeCalculator.parseDutchNumber(label)
    val pillBg = when {
        numeric != null && numeric < 5.5 -> palette.errorContainer
        numeric != null && numeric >= 7.0 -> palette.secondaryContainer
        else -> palette.surfaceHigh
    }
    val pillFg = when {
        numeric != null && numeric < 5.5 -> palette.error
        numeric != null && numeric >= 7.0 -> palette.secondary
        else -> palette.onSurface
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium),
                    color = palette.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.caption3,
                        color = palette.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(pillBg)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    GradeCalculator.formatGradeValue(value, label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pillFg,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun WearHomeworkScreen(homework: List<HomeworkItem>) {
    val palette = LocalWearPalette.current
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .wearScrollRatchet(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                ListHeader {
                    Text("Huiswerk", style = MaterialTheme.typography.title3, color = palette.onSurface)
                }
            }
            if (homework.isEmpty()) {
                item {
                    Text(
                        "Alles af!",
                        style = MaterialTheme.typography.caption1,
                        color = palette.secondary,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            } else {
                itemsIndexed(homework, key = { _, it -> "hw-${it.id}" }) { index, item ->
                    val due = WearScheduleHelper.localDateOf(item.dueIso)?.format(shortDateFmt).orEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearStaggerEnter(index, key = "hw-${item.id}")
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.surface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    item.displaySubject(),
                                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.SemiBold),
                                    color = palette.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (due.isNotBlank()) {
                                    Text(
                                        due,
                                        style = MaterialTheme.typography.caption3,
                                        color = palette.primary,
                                    )
                                }
                            }
                            val body = item.description?.stripHtml()?.takeIf { it.isNotBlank() }
                                ?: item.title.stripHtml()
                            Text(
                                body,
                                style = MaterialTheme.typography.caption2,
                                color = palette.muted,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun WearNavChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    secondaryLabel: String? = null,
    subtle: Boolean = false,
    haptic: WearHapticEvent = WearHapticEvent.Click,
    modifier: Modifier = Modifier,
) {
    val palette = LocalWearPalette.current
    val haptics = LocalWearHaptics.current
    Chip(
        onClick = {
            haptics?.play(haptic)
            onClick()
        },
        label = {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        secondaryLabel = secondaryLabel?.let {
            {
                Text(
                    it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.muted,
                )
            }
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (subtle) palette.muted else palette.primary,
            )
        },
        colors = ChipDefaults.secondaryChipColors(
            backgroundColor = if (subtle) palette.surface else palette.surfaceHigh,
            contentColor = palette.onSurface,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
