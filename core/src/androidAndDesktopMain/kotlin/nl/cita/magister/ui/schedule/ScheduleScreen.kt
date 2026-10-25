package nl.cita.magister.ui.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.LocalMagsHaptics
import nl.cita.magister.haptics.hapticOnDown
import nl.cita.magister.platform.PlatformBackHandler
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.subject.teacherCode
import nl.cita.magister.ui.components.CookieFlower
import nl.cita.magister.ui.components.EmptyState
import nl.cita.magister.ui.components.ListSkeleton
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.formatTime
import nl.cita.magister.ui.localDateOf
import nl.cita.magister.ui.nav.MagsPage
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.StaggerScope
import nl.cita.magister.ui.motion.pageFadeSpring
import nl.cita.magister.ui.motion.pressSpring
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
    onOpenPage: (MagsPage) -> Unit = {},
) {
    val today = clockDate
    val weekendLessons = remember(appointments) { weekendDatesWithLessons(appointments) }
    val startMonday = remember(today) { today.minusWeeks(WeekRadius.toLong()).with(DayOfWeek.MONDAY) }
    val weekCount = WeekRadius * 2
    val days = remember(startMonday, weekendLessons) {
        schoolDays(startMonday, weekCount, weekendLessons)
    }
    val seed = remember(initialDate, today, weekendLessons) {
        nextSchoolDate(initialDate ?: today, weekendLessons)
    }
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
    val haptics = LocalMagsHaptics.current
    val skipDaySnap = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val lastTargetPage = remember { java.util.concurrent.atomic.AtomicInteger(-1) }
    val targetPage = pager.targetPage
    androidx.compose.runtime.SideEffect {
        val previous = lastTargetPage.getAndSet(targetPage)
        if (previous >= 0 && previous != targetPage) {
            if (!skipDaySnap.getAndSet(false)) haptics.play(HapticEvent.DaySnap)
        }
    }

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
                val weekday = selected.dayOfWeek.value.let { if (it in 1..5) it else 1 }
                val target = monday.plusDays((weekday - 1).toLong())
                val index = days.indexOf(target)
                if (index >= 0) pager.animateScrollToPage(index)
            }
    }

    fun jumpTo(date: LocalDate) {
        val target = nextSchoolDate(date, weekendLessons)
        val index = days.indexOf(target)
        if (index < 0 || index == pager.targetPage) return
        skipDaySnap.set(true)
        scope.launch { pager.animateScrollToPage(index) }
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
            Text("Rooster", style = MaterialTheme.typography.displayMedium)
            AnimatedContent(
                targetState = selected,
                modifier = Modifier.testTag("rooster-titel-$selected"),
                transitionSpec = {
                    val dir = if (targetState.isAfter(initialState)) 1 else -1
                    (
                        fadeIn(pageFadeSpring()) +
                            slideInVertically { dir * 16 } +
                            scaleIn(initialScale = 0.98f, animationSpec = pageFadeSpring())
                        ) togetherWith (
                        fadeOut(pageFadeSpring()) + slideOutVertically { -dir * 12 }
                    )
                },
                label = "rooster-datum",
            ) { date ->
                Text(
                    date.format(titleDay).replaceFirstChar { it.titlecase(nl) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionError(agendaError)
            WeekStrip(
                weekPager = weekPager,
                startMonday = startMonday,
                dayPager = pager,
                days = days,
                today = today,
                weekendLessons = weekendLessons,
                onPick = ::jumpTo,
            )
            if (today in days && selected != today) {
                FilledTonalButton(
                    onClick = { jumpTo(today) },
                    modifier = Modifier
                        .hapticOnDown(HapticEvent.TodayJump)
                        .testTag("rooster-vandaag"),
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
            val away = abs(pageOffset).coerceAtMost(1f)
            val scale = 1f - 0.06f * away
            val pageAlpha = 1f - 0.14f * away
            Crossfade(
                targetState = loading && appointments.isEmpty(),
                animationSpec = pageFadeSpring(),
                label = "rooster-load",
            ) { skeleton ->
                if (skeleton) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = MagsChrome.PageGutter, vertical = 12.dp),
                    ) {
                        ListSkeleton(rows = 3, contentDescription = "Rooster laden")
                    }
                } else {
                    StaggerScope {
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
                                alpha = pageAlpha
                            },
                        )
                    }
                }
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

@Composable
private fun WeekStrip(
    weekPager: PagerState,
    startMonday: LocalDate,
    dayPager: PagerState,
    days: List<LocalDate>,
    today: LocalDate,
    weekendLessons: Set<LocalDate>,
    onPick: (LocalDate) -> Unit,
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
        val weekDays = remember(monday, weekendLessons) { weekVisibleDays(monday, weekendLessons) }
        WeekChips(
            weekDays = weekDays,
            dayPager = dayPager,
            days = days,
            today = today,
            onPick = onPick,
        )
    }
}

@Composable
private fun WeekChips(
    weekDays: List<LocalDate>,
    dayPager: PagerState,
    days: List<LocalDate>,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
) {
    val density = LocalDensity.current
    var stripWidth by remember { mutableIntStateOf(0) }
    val slot = if (weekDays.isEmpty() || stripWidth == 0) 0f else stripWidth / weekDays.size.toFloat()
    val onThisWeek = weekDays.any { it == days.getOrNull(dayPager.currentPage) }
    val pillAlpha by animateFloatAsState(
        targetValue = if (onThisWeek) 1f else 0f,
        animationSpec = pageFadeSpring(),
        label = "week-pill-alpha",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .onSizeChanged { stripWidth = it.width },
    ) {
        if (slot > 0f) {
            Box(
                Modifier
                    .offset {
                        val index = weekdayOffset(dayPager, days, weekDays)
                        IntOffset((index * slot).roundToInt(), 0)
                    }
                    .width(with(density) { slot.toDp() })
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp)
                    .graphicsLayer { alpha = pillAlpha }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
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
                val chipInteraction = remember(day) { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val chipPressed by chipInteraction.collectIsPressedAsState()
                val chipScale by animateFloatAsState(
                    targetValue = if (chipPressed) 0.96f else 1f,
                    animationSpec = pressSpring(),
                    label = "week-press-$day",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = chipScale
                            scaleY = chipScale
                        }
                        .clip(CircleShape)
                        .hapticOnDown(HapticEvent.WeekChip)
                        .clickable(
                            interactionSource = chipInteraction,
                            indication = androidx.compose.material3.ripple(bounded = true),
                        ) { onPick(day) }
                        .padding(vertical = 6.dp)
                        .testTag("week-pick-$day"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.SHORT, nl).replaceFirstChar { it.titlecase(nl) },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
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

/** Index in [weekDays], plus the live pager fraction so the pill slides with the day page. */
internal fun weekdayOffset(pager: PagerState, days: List<LocalDate>, weekDays: List<LocalDate>): Float {
    val date = days.getOrNull(pager.currentPage) ?: return 0f
    val index = weekDays.indexOf(date)
    if (index < 0) return 0f
    val last = (weekDays.size - 1).coerceAtLeast(0).toFloat()
    return (index + pager.currentPageOffsetFraction).coerceIn(-0.15f, last + 0.15f)
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
    val weekend = isWeekend(date)
    val slots = remember(lessons) { buildDaySlots(lessons) }
    val isToday = date == today
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("rooster-dag-$date"),
        userScrollEnabled = true,
        contentPadding = PaddingValues(
            start = MagsChrome.PageGutter,
            end = MagsChrome.PageGutter,
            top = 12.dp,
            bottom = contentBottom + MagsChrome.ListBottomGap,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (weekend || lessons.isEmpty()) {
            item {
                EmptyState(
                    title = if (weekend) "Weekend" else "Vrij",
                    tag = "rooster-leeg",
                )
            }
        }
        if (isToday && nowLineBefore(slots, now)) {
            item { NowLine() }
        }
        itemsIndexed(slots, key = { _, slot -> slotKey(slot) }) { index, slot ->
            when (slot) {
                is DaySlot.Gap -> {
                    Box(Modifier.staggerEnter(index, key = slotKey(slot))) {
                        if (slot.kind == GapKind.FreePeriod) GapRow(slot) else BreakMark()
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
                        modifier = Modifier.staggerEnter(index, key = slotKey(slot)),
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
private fun BreakMark() {
    val line = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Row(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .testTag("pauze"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(line),
        )
        Text(
            "pauze",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(line),
        )
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
    val dimmed = item.isCancelled
    PressScaleCard(
        colors = CardDefaults.cardColors(
            when {
                dimmed -> MaterialTheme.colorScheme.surfaceContainerLow
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
            if (current && !dimmed) {
                CookieFlower(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .testTag("les-nu-vorm"),
                    size = 72.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(end = if (current && !dimmed) 68.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            item.displaySubject(),
                            style = if (current) MaterialTheme.typography.headlineSmall
                            else MaterialTheme.typography.titleLarge,
                            color = if (dimmed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.isCancelled || item.isChanged) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (item.isCancelled) {
                                    LessonBadge(
                                        text = "Uitval",
                                        container = MaterialTheme.colorScheme.errorContainer,
                                        content = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                } else if (item.isChanged) {
                                    LessonBadge(
                                        text = "Gewijzigd",
                                        container = MaterialTheme.colorScheme.tertiaryContainer,
                                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }
                        }
                        val meta = listOfNotNull(
                            item.teacherCode(),
                            item.classroom ?: item.location,
                            listOfNotNull(formatTime(item.startIso), formatTime(item.endIso))
                                .filter { it.isNotBlank() }
                                .joinToString("–")
                                .takeIf { it.isNotBlank() },
                        ).joinToString("  ·  ")
                        if (meta.isNotBlank()) {
                            Text(
                                meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (current && !dimmed) {
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
    PlatformBackHandler(true, onDismiss)
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
            Text(lesson.displaySubject(), style = MaterialTheme.typography.headlineSmall)
            Text(
                listOfNotNull(formatTime(lesson.startIso), formatTime(lesson.endIso)).joinToString("–"),
                style = MaterialTheme.typography.titleMedium,
            )
            lesson.teacherCode()?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            (lesson.classroom ?: lesson.location)?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            when {
                lesson.isCancelled -> LessonBadge(
                    text = "Uitval",
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                )
                lesson.isChanged -> LessonBadge(
                    text = "Gewijzigd",
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                )
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

@Composable
private fun LessonBadge(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Text(
        text,
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(if (text == "Uitval") "les-uitval" else "les-gewijzigd"),
        style = MaterialTheme.typography.labelLarge,
        color = content,
        fontWeight = FontWeight.SemiBold,
    )
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
    is DaySlot.Gap -> "gap-${slot.kind}-${slot.fromIso}-${slot.toIso}"
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
    val first = slots.firstOrNull() ?: return false
    val start = slotStart(first) ?: return false
    return now.isBefore(start)
}

private fun nowLineAfter(slots: List<DaySlot>, now: LocalTime): Boolean {
    val last = slots.lastOrNull() ?: return false
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
