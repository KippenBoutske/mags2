package nl.cita.magister.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Appointment
import nl.cita.magister.ui.components.ListSkeleton
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.formatTime
import nl.cita.magister.ui.localDateOf
import nl.cita.magister.ui.motion.PressScaleCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private val zone = ZoneId.of("Europe/Amsterdam")
private val nl = Locale("nl", "NL")
private val titleDay = DateTimeFormatter.ofPattern("EEEE d MMMM", nl)

/** Weeks on each side of today. Pager pages are lazy; adjacent weeks prefetch on settle. */
private const val WeekRadius = 40

@Composable
fun ScheduleScreen(
    appointments: List<Appointment>,
    contentPadding: PaddingValues,
    agendaError: ApiFailure? = null,
    initialDate: LocalDate? = null,
    onVisibleWeek: (LocalDate) -> Unit = {},
    loading: Boolean = false,
) {
    val today = remember { LocalDate.now(zone) }
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
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PageTitle("Rooster", selected.format(titleDay).replaceFirstChar { it.titlecase(nl) })
            SectionError(agendaError)
            WeekStrip(
                weekPager = weekPager,
                startMonday = startMonday,
                dayPager = pager,
                days = days,
                today = today,
                onPick = ::jumpTo,
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
            if (loading && appointments.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    ListSkeleton(rows = 3, contentDescription = "Rooster laden")
                }
            } else {
                DayTimeline(
                    date = date,
                    lessons = ofDay,
                    contentBottom = contentPadding.calculateBottomPadding(),
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(
    weekPager: PagerState,
    startMonday: LocalDate,
    dayPager: PagerState,
    days: List<LocalDate>,
    today: LocalDate,
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
        val weekDays = remember(monday) { (0..4).map { monday.plusDays(it.toLong()) } }
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
    val pillIndex = weekdayOffset(dayPager, days)
    val onWeekend = weekDays.none { it == days.getOrNull(dayPager.currentPage) } &&
        kotlin.math.abs(dayPager.currentPageOffsetFraction) < 0.02f
    val pillAlpha = if (onWeekend) 0f else 1f

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
) {
    val now = LocalTime.now(zone)
    val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rooster-dag-$date"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = contentBottom + 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (weekend) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 24.dp)) {
                    Text("Weekend", style = MaterialTheme.typography.headlineSmall)
                    Text("Niks op school. Swipe naar maandag.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else if (lessons.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 24.dp)) {
                    Text("Vrij", style = MaterialTheme.typography.headlineSmall)
                    Text("Geen lessen op deze dag.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        itemsIndexed(lessons, key = { _, it -> it.id }) { _, item ->
            val current = isCurrent(item, date, now)
            PressScaleCard(
                colors = CardDefaults.cardColors(
                    when {
                        item.isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                        current -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    if (current) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("rooster-now-line"),
                        )
                    }
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                style = MaterialTheme.typography.titleMedium,
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
                        }
                    }
                }
            }
        }
    }
}

private fun isCurrent(item: Appointment, date: LocalDate, now: LocalTime): Boolean {
    if (date != LocalDate.now(zone) || item.isCancelled) return false
    val start = lessonTime(item.startIso) ?: return false
    val end = lessonTime(item.endIso) ?: return false
    return !now.isBefore(start) && now.isBefore(end)
}

private fun lessonTime(iso: String?): LocalTime? =
    runCatching { java.time.OffsetDateTime.parse(iso).toLocalTime() }.getOrNull()
        ?: runCatching { java.time.Instant.parse(iso!!).atZone(zone).toLocalTime() }.getOrNull()
