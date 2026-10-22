package nl.cita.magister.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import nl.cita.magister.ui.components.CookieFlower
import nl.cita.magister.ui.components.ListSkeleton
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.formatTime
import nl.cita.magister.ui.localDateOf
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.magsSharedBounds
import nl.cita.magister.ui.motion.magsSpring
import nl.cita.magister.ui.motion.staggerEnter
import nl.cita.magister.ui.nav.MagsChrome
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val zone = ZoneId.of("Europe/Amsterdam")
private val nl = Locale("nl", "NL")
private val titleDay = DateTimeFormatter.ofPattern("EEEE d MMMM", nl)
private const val WeekRadius = 40
private const val MinGapMinutes = 10L

internal sealed class DaySlot {
    data class Lesson(val item: Appointment) : DaySlot()
    data class Gap(val fromIso: String, val toIso: String) : DaySlot()
}

internal fun buildDaySlots(lessons: List<Appointment>, minGapMinutes: Long = MinGapMinutes): List<DaySlot> {
    val sorted = lessons.sortedBy { it.startIso }
    if (sorted.isEmpty()) return emptyList()
    val slots = mutableListOf<DaySlot>()
    sorted.forEachIndexed { index, item ->
        if (index > 0) {
            val previous = sorted[index - 1]
            val minutes = minutesBetween(previous.endIso, item.startIso)
            if (minutes != null && minutes >= minGapMinutes) {
                slots += DaySlot.Gap(previous.endIso, item.startIso)
            }
        }
        slots += DaySlot.Lesson(item)
    }
    return slots
}

internal fun minutesBetween(fromIso: String, toIso: String): Long? {
    val from = lessonDateTime(fromIso) ?: return null
    val to = lessonDateTime(toIso) ?: return null
    return Duration.between(from, to).toMinutes()
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ScheduleScreen(
    appointments: List<Appointment>,
    contentPadding: PaddingValues,
    agendaError: ApiFailure? = null,
    initialDate: LocalDate? = null,
    onVisibleWeek: (LocalDate) -> Unit = {},
    loading: Boolean = false,
    homework: List<HomeworkItem> = emptyList(),
    clockDate: LocalDate = LocalDate.now(zone),
    clockTime: LocalTime = LocalTime.now(zone),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val today = clockDate
    val startMonday = remember(today) { today.minusWeeks(WeekRadius.toLong()).with(DayOfWeek.MONDAY) }
    val days = remember(startMonday) {
        List(WeekRadius * 2 * 7) { startMonday.plusDays(it.toLong()) }
    }
    val weekCount = WeekRadius * 2
    val seed = initialDate ?: today
    val initialPage = remember(days, seed) { days.indexOf(seed).coerceIn(0, days.lastIndex) }
    val initialWeek = remember(startMonday, seed) {
        ChronoUnit.WEEKS.between(startMonday, seed.with(DayOfWeek.MONDAY)).toInt().coerceIn(0, weekCount - 1)
    }
    val pager = rememberPagerState(initialPage = initialPage) { days.size }
    val weekPager = rememberPagerState(initialPage = initialWeek) { weekCount }
    val scope = rememberCoroutineScope()
    val selected = days.getOrElse(pager.currentPage) { today }
    val byDate = remember(appointments) { appointments.groupBy { localDateOf(it.startIso) } }
    var openLesson by remember { mutableStateOf<Appointment?>(null) }

    LaunchedEffect(pager) {
        snapshotFlow { days.getOrNull(pager.settledPage)?.with(DayOfWeek.MONDAY) }
            .distinctUntilChanged()
            .collect { monday -> monday?.let(onVisibleWeek) }
    }

    LaunchedEffect(pager, weekPager) {
        snapshotFlow { days.getOrNull(pager.currentPage)?.with(DayOfWeek.MONDAY) }
            .distinctUntilChanged()
            .collect { monday ->
                val index = monday?.let { ChronoUnit.WEEKS.between(startMonday, it).toInt() } ?: return@collect
                if (index in 0 until weekCount && weekPager.currentPage != index && !weekPager.isScrollInProgress) {
                    weekPager.animateScrollToPage(index)
                }
            }
    }

    LaunchedEffect(weekPager, pager) {
        snapshotFlow { weekPager.settledPage to weekPager.isScrollInProgress }
            .map { it.first }
            .distinctUntilChanged()
            .collect { week ->
                val monday = startMonday.plusWeeks(week.toLong())
                val currentMonday = selected.with(DayOfWeek.MONDAY)
                if (monday == currentMonday || pager.isScrollInProgress) return@collect
                val weekday = selected.dayOfWeek.value.coerceIn(1, 5)
                val target = monday.plusDays((weekday - 1).toLong())
                val index = days.indexOf(target)
                if (index >= 0) pager.animateScrollToPage(index)
            }
    }

    fun jumpTo(date: LocalDate) {
        val index = days.indexOf(date)
        if (index >= 0) scope.launch { pager.animateScrollToPage(index) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding() + 4.dp),
    ) {
        Column(
            Modifier.padding(horizontal = MagsChrome.PageGutter),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(magsSpring()) togetherWith fadeOut() },
                label = "rooster-titel",
            ) { date ->
                PageTitle("Rooster", date.format(titleDay).replaceFirstChar { it.titlecase(nl) })
            }
            SectionError(agendaError)
            WeekStrip(
                weekPager = weekPager,
                startMonday = startMonday,
                dayPager = pager,
                days = days,
                today = today,
                onPick = ::jumpTo,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
            if (selected != today) {
                FilledTonalButton(
                    onClick = { jumpTo(today) },
                    modifier = Modifier.testTag("rooster-vandaag"),
                ) { Text("Vandaag") }
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("rooster-pager"),
            beyondViewportPageCount = 1,
            key = { days[it] },
        ) { page ->
            val date = days[page]
            val ofDay = (byDate[date] ?: emptyList()).sortedBy { it.startIso }
            val pageOffset = (pager.currentPage - page) + pager.currentPageOffsetFraction
            val scale = 1f - 0.035f * abs(pageOffset).coerceAtMost(1f)
            if (loading && appointments.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = MagsChrome.PageGutter, vertical = 12.dp),
                ) {
                    ListSkeleton(rows = 3, contentDescription = "Rooster laden")
                }
            } else {
                DayTimeline(
                    date = date,
                    lessons = ofDay,
                    contentBottom = contentPadding.calculateBottomPadding(),
                    today = today,
                    now = clockTime,
                    onOpen = { openLesson = it },
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
        }
    }

    openLesson?.let { lesson ->
        LessonSheet(
            lesson = lesson,
            homework = homework.filter { matchesHomework(it, lesson) },
            onDismiss = { openLesson = null },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun WeekStrip(
    weekPager: PagerState,
    startMonday: LocalDate,
    dayPager: PagerState,
    days: List<LocalDate>,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    HorizontalPager(
        state = weekPager,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag("rooster-weekstrip"),
        beyondViewportPageCount = 1,
        key = { startMonday.plusWeeks(it.toLong()) },
    ) { week ->
        val monday = startMonday.plusWeeks(week.toLong())
        val weekDays = remember(monday) { (0..4).map { monday.plusDays(it.toLong()) } }
        WeekChips(
            weekDays = weekDays,
            dayPager = dayPager,
            days = days,
            today = today,
            onPick = onPick,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun WeekChips(
    weekDays: List<LocalDate>,
    dayPager: PagerState,
    days: List<LocalDate>,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val density = LocalDensity.current
    var stripWidth by remember { mutableIntStateOf(0) }
    val slot = if (weekDays.isEmpty() || stripWidth == 0) 0f else stripWidth / weekDays.size.toFloat()
    val pillIndex = weekdayOffset(dayPager, days)
    val onWeekend = weekDays.none { it == days.getOrNull(dayPager.currentPage) } &&
        abs(dayPager.currentPageOffsetFraction) < 0.02f
    val pillAlpha = if (onWeekend) 0f else 1f
    val selectedDate = days.getOrNull(dayPager.currentPage)

    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .onSizeChanged { stripWidth = it.width },
    ) {
        if (slot > 0f && pillAlpha > 0f) {
            Box(
                Modifier
                    .offset { IntOffset((pillIndex * slot).roundToInt(), 0) }
                    .width(with(density) { slot.toDp() })
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp)
                    .then(
                        magsSharedBounds(
                            key = "week-day-${selectedDate ?: today}",
                            scope = sharedTransitionScope,
                            visibility = animatedVisibilityScope,
                        ),
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = pillAlpha))
                    .testTag("week-pill"),
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            weekDays.forEach { day ->
                val closest = days.getOrNull(dayPager.currentPage)
                val selectedDay = closest == day
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onPick(day) }
                        .padding(vertical = 6.dp)
                        .testTag("week-pick-$day"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.SHORT, nl).replaceFirstChar { it.titlecase(nl) },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedDay) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selectedDay) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selectedDay) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .padding(top = 2.dp)
                            .height(4.dp)
                            .width(4.dp)
                            .background(
                                if (day == today) MaterialTheme.colorScheme.primary
                                else androidx.compose.ui.graphics.Color.Transparent,
                            ),
                    )
                }
            }
        }
    }
}

/** 0 = Ma … 4 = Vr, plus the live pager fraction so the pill slides with the day page. */
internal fun weekdayOffset(pager: PagerState, days: List<LocalDate>): Float {
    val date = days.getOrNull(pager.currentPage) ?: return 0f
    val base = (date.dayOfWeek.value - 1).toFloat()
    return (base + pager.currentPageOffsetFraction).coerceIn(-0.15f, 4.15f)
}

@Composable
private fun DayTimeline(
    date: LocalDate,
    lessons: List<Appointment>,
    contentBottom: androidx.compose.ui.unit.Dp,
    today: LocalDate,
    now: LocalTime,
    onOpen: (Appointment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    val slots = remember(lessons) { buildDaySlots(lessons) }
    val isToday = date == today
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("rooster-dag-$date"),
        contentPadding = PaddingValues(
            start = MagsChrome.PageGutter,
            end = MagsChrome.PageGutter,
            top = 12.dp,
            bottom = contentBottom + 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (weekend) {
            item {
                Text("Weekend", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp))
            }
        } else if (lessons.isEmpty()) {
            item {
                Text("Vrij", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp))
            }
        }
        if (isToday && nowLineBefore(slots, now)) {
            item { NowLine() }
        }
        itemsIndexed(slots, key = { _, slot -> slotKey(slot) }) { index, slot ->
            when (slot) {
                is DaySlot.Gap -> {
                    Box(Modifier.staggerEnter(index)) {
                        GapRow(slot)
                        if (isToday && nowInsideGap(slot, now)) {
                            NowLine(Modifier.align(Alignment.Center))
                        }
                    }
                }
                is DaySlot.Lesson -> {
                    val current = isCurrent(slot.item, isToday, now)
                    LessonCard(
                        item = slot.item,
                        current = current,
                        now = now,
                        onClick = { onOpen(slot.item) },
                        modifier = Modifier.staggerEnter(index),
                    )
                }
            }
        }
        if (isToday && nowLineAfter(slots, now)) {
            item { NowLine() }
        }
    }
}

@Composable
private fun NowLine(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("rooster-now-line"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text("nu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun GapRow(slot: DaySlot.Gap) {
    val from = formatTime(slot.fromIso)
    val to = formatTime(slot.toIso)
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 14.dp)
            .testTag("tussenuur"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Tussenuur", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("$from–$to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LessonCard(
    item: Appointment,
    current: Boolean,
    now: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cardHeight by remember { mutableIntStateOf(0) }
    PressScaleCard(
        colors = CardDefaults.cardColors(
            when {
                item.isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                current -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (current) "les-kaart-nu" else "les-kaart"),
        onClick = onClick,
    ) {
        Box(Modifier.onSizeChanged { cardHeight = it.height }) {
            if (current) {
                CookieFlower(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(72.dp)
                        .testTag("les-nu-vorm"),
                )
            }
            Column(Modifier.padding(if (current) 18.dp else 14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(item.periodFrom?.toString() ?: "–", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            item.title,
                            style = if (current) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            textDecoration = if (item.isCancelled) TextDecoration.LineThrough else null,
                            color = if (item.isCancelled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        if (item.isCancelled) {
                            Text("Uitval", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            item.teachers.firstOrNull() ?: "–",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val room = item.classroom ?: item.location ?: "–"
                        Text(
                            if (item.isChanged) "$room · gewijzigd" else room,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isChanged) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (item.isChanged) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            listOfNotNull(formatTime(item.startIso), formatTime(item.endIso)).joinToString("–"),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        if (current) {
                            item.content?.takeIf { it.isNotBlank() }?.let { body ->
                                Text(
                                    body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (current) {
                val progress = lessonProgress(item, now)
                NowLine(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(end = 84.dp)
                        .zIndex(2f)
                        .offset {
                            val y = ((cardHeight - 16) * progress).roundToInt().coerceAtLeast(0)
                            IntOffset(0, y)
                        },
                )
            }
        }
    }
}

@Composable
private fun LessonSheet(
    lesson: Appointment,
    homework: List<HomeworkItem>,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("les-blad"),
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(lesson.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                listOfNotNull(formatTime(lesson.startIso), formatTime(lesson.endIso)).joinToString("–"),
                style = MaterialTheme.typography.titleMedium,
            )
            lesson.teachers.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            (lesson.classroom ?: lesson.location)?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            when {
                lesson.isCancelled -> Text("Uitval", color = MaterialTheme.colorScheme.error)
                lesson.isChanged -> Text("Gewijzigd", color = MaterialTheme.colorScheme.tertiary)
            }
            infoLabel(lesson.infoType)?.let {
                Text(it, style = MaterialTheme.typography.labelLarge)
            }
            val hw = lesson.content?.trim().orEmpty()
            if (hw.isNotBlank()) {
                Text("Huiswerk", style = MaterialTheme.typography.titleSmall)
                Text(hw, style = MaterialTheme.typography.bodyMedium)
            }
            homework.forEach { item ->
                val extra = item.description?.trim().orEmpty()
                if (extra.isNotBlank() && extra != hw) {
                    Text(extra, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun infoLabel(type: InfoType): String? = when (type) {
    InfoType.Homework -> "Huiswerk"
    InfoType.Test -> "Proefwerk"
    InfoType.Written -> "SO / PO"
    InfoType.Oral -> "Mondeling"
    else -> null
}

private fun matchesHomework(item: HomeworkItem, lesson: Appointment): Boolean {
    if (item.appointmentId == lesson.id) return true
    val sameSubject = item.subject.equals(lesson.title, ignoreCase = true) ||
        lesson.subjects.any { it.equals(item.subject, ignoreCase = true) }
    return sameSubject && localDateOf(item.dueIso) == localDateOf(lesson.startIso)
}

private fun slotKey(slot: DaySlot): String = when (slot) {
    is DaySlot.Lesson -> "les-${slot.item.id}"
    is DaySlot.Gap -> "gap-${slot.fromIso}-${slot.toIso}"
}

private fun isCurrent(item: Appointment, isToday: Boolean, now: LocalTime): Boolean {
    if (!isToday || item.isCancelled) return false
    val start = lessonTime(item.startIso) ?: return false
    val end = lessonTime(item.endIso) ?: return false
    return !now.isBefore(start) && now.isBefore(end)
}

internal fun lessonProgress(item: Appointment, now: LocalTime): Float {
    val start = lessonTime(item.startIso) ?: return 0f
    val end = lessonTime(item.endIso) ?: return 1f
    val span = Duration.between(start.atDate(LocalDate.of(2000, 1, 1)), end.atDate(LocalDate.of(2000, 1, 1))).seconds
    if (span <= 0) return 0f
    val elapsed = Duration.between(start.atDate(LocalDate.of(2000, 1, 1)), now.atDate(LocalDate.of(2000, 1, 1))).seconds
    return (elapsed.toFloat() / span.toFloat()).coerceIn(0.08f, 0.92f)
}

private fun nowLineBefore(slots: List<DaySlot>, now: LocalTime): Boolean {
    val first = slots.firstOrNull() ?: return true
    val start = slotStart(first) ?: return false
    return now.isBefore(start)
}

private fun nowLineAfter(slots: List<DaySlot>, now: LocalTime): Boolean {
    val last = slots.lastOrNull() ?: return true
    val end = slotEnd(last) ?: return false
    return !now.isBefore(end)
}

private fun nowInsideGap(slot: DaySlot.Gap, now: LocalTime): Boolean {
    val start = lessonTime(slot.fromIso) ?: return false
    val end = lessonTime(slot.toIso) ?: return false
    return !now.isBefore(start) && now.isBefore(end)
}

private fun slotStart(slot: DaySlot): LocalTime? = when (slot) {
    is DaySlot.Lesson -> lessonTime(slot.item.startIso)
    is DaySlot.Gap -> lessonTime(slot.fromIso)
}

private fun slotEnd(slot: DaySlot): LocalTime? = when (slot) {
    is DaySlot.Lesson -> lessonTime(slot.item.endIso)
    is DaySlot.Gap -> lessonTime(slot.toIso)
}

private fun lessonTime(iso: String?): LocalTime? =
    lessonDateTime(iso)?.toLocalTime()

private fun lessonDateTime(iso: String?): java.time.ZonedDateTime? {
    if (iso.isNullOrBlank()) return null
    val instant = runCatching { java.time.Instant.parse(iso) }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull()
        ?: return null
    return instant.atZone(zone)
}
