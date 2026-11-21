package nl.cita.magister.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import nl.cita.magister.MainActivity
import nl.cita.magister.ui.theme.WarmDarkScheme
import nl.cita.magister.ui.theme.WarmLightScheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

private val PageParamKey = ActionParameters.Key<String>(MainActivity.EXTRA_PAGE)

private fun openPageAction(page: String): Action =
    actionStartActivity<MainActivity>(actionParametersOf(PageParamKey to page))

@Composable
private fun MagsWidgetTheme(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme(content = content)
    } else {
        GlanceTheme(
            colors = ColorProviders(
                light = WarmLightScheme,
                dark = WarmDarkScheme,
            ),
            content = content,
        )
    }
}

private data class WidgetLessonFocus(
    val lesson: WidgetLesson,
    val isCurrent: Boolean,
    val minutesDelta: Long,
)

private fun pickFocus(lessons: List<WidgetLesson>, now: Instant): WidgetLessonFocus? {
    val active = lessons.filter { !it.isCancelled }
    val current = active.firstOrNull { lesson ->
        val s = NextLessonStore.parseInstant(lesson.startIso) ?: return@firstOrNull false
        val e = NextLessonStore.parseInstant(lesson.endIso) ?: return@firstOrNull false
        !now.isBefore(s) && now.isBefore(e)
    }
    if (current != null) {
        val end = NextLessonStore.parseInstant(current.endIso) ?: now
        val mins = ceil(Duration.between(now, end).seconds.coerceAtLeast(60L) / 60.0).toLong()
        return WidgetLessonFocus(current, isCurrent = true, minutesDelta = mins)
    }
    val next = active.firstOrNull { lesson ->
        val s = NextLessonStore.parseInstant(lesson.startIso) ?: return@firstOrNull false
        s.isAfter(now)
    } ?: return null
    val start = NextLessonStore.parseInstant(next.startIso) ?: now
    val mins = ceil(Duration.between(now, start).seconds.coerceAtLeast(60L) / 60.0).toLong()
    return WidgetLessonFocus(next, isCurrent = false, minutesDelta = mins)
}

private fun formatMinutesLabel(minutes: Long, isCurrent: Boolean): String {
    val dur = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0L -> "${minutes / 60} u"
        else -> "${minutes / 60} u ${minutes % 60} min"
    }
    return if (isCurrent) "nog $dur" else "over $dur"
}

// ---------------------------------------------------------------------------
// 1. Volgende les (Material You)
// ---------------------------------------------------------------------------

class NextLessonWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = NextLessonStore.readFull(context)
        provideContent {
            MagsWidgetTheme {
                NextLessonContent(snap)
            }
        }
    }
}

@Composable
private fun NextLessonContent(snap: MagsWidgetSnapshot) {
    val now = Instant.now()
    val focus = pickFocus(snap.lessons, now)
    val bg = if (focus?.isCurrent == true) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.widgetBackground
    val titleColor = if (focus?.isCurrent == true) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurface
    val subColor = if (focus?.isCurrent == true) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurfaceVariant

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(bg)
            .padding(16.dp)
            .clickable(openPageAction("vandaag")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (focus == null) {
            Box(
                modifier = GlanceModifier
                    .cornerRadius(12.dp)
                    .background(GlanceTheme.colors.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "MAGS",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            Text(
                "Vrij voor vandaag",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                "Tik om je rooster te openen",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        } else {
            val lesson = focus.lesson
            val start = NextLessonStore.formatClock(lesson.startIso)
            val end = NextLessonStore.formatClock(lesson.endIso)
            val timeRange = if (end.isNotBlank()) "$start – $end" else start
            val badgeBg = if (focus.isCurrent) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer
            val badgeFg = if (focus.isCurrent) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(10.dp)
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (focus.isCurrent) "NU" else "VOLGENDE",
                        style = TextStyle(color = badgeFg, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    formatMinutesLabel(focus.minutesDelta, focus.isCurrent),
                    style = TextStyle(color = subColor, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            Text(
                lesson.subject,
                style = TextStyle(
                    color = titleColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
            )

            Spacer(GlanceModifier.height(6.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!lesson.room.isNullOrBlank()) {
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(10.dp)
                            .background(GlanceTheme.colors.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            lesson.room,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    Spacer(GlanceModifier.width(8.dp))
                }
                Text(
                    timeRange,
                    style = TextStyle(color = subColor, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}

class NextLessonWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextLessonWidget()

    companion object {
        suspend fun updateAll(context: Context) {
            NextLessonWidget().updateAll(context)
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Rooster vandaag (Material You)
// ---------------------------------------------------------------------------

class TodayScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = NextLessonStore.readFull(context)
        provideContent {
            MagsWidgetTheme {
                TodayScheduleContent(snap)
            }
        }
    }
}

@Composable
private fun TodayScheduleContent(snap: MagsWidgetSnapshot) {
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
    val today = LocalDate.now(zone)
    val tomorrow = today.plusDays(1)
    val todayLessons = snap.lessons.filter { NextLessonStore.parseLocalDate(it.startIso, zone) == today }
    val tomorrowLessons = snap.lessons.filter { NextLessonStore.parseLocalDate(it.startIso, zone) == tomorrow }

    val remainingToday = todayLessons.filter { lesson ->
        val end = NextLessonStore.parseInstant(lesson.endIso)
            ?: NextLessonStore.parseInstant(lesson.startIso)
            ?: return@filter true
        end.isAfter(now)
    }
    val showingTomorrow = remainingToday.isEmpty() && tomorrowLessons.isNotEmpty()
    val listToShow = if (showingTomorrow) tomorrowLessons.take(4) else remainingToday.take(4)
    val headerLabel = if (showingTomorrow) "Rooster morgen" else "Rooster vandaag"

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(14.dp)
            .clickable(openPageAction("rooster")),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                headerLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (listToShow.isNotEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(10.dp)
                        .background(GlanceTheme.colors.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    val count = if (showingTomorrow) tomorrowLessons.size else todayLessons.size
                    Text(
                        if (count == 1) "1 les" else "$count lessen",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }

        Spacer(GlanceModifier.height(8.dp))

        if (listToShow.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Geen lessen meer gepland",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
            }
        } else {
            listToShow.forEachIndexed { idx, lesson ->
                val s = NextLessonStore.parseInstant(lesson.startIso)
                val e = NextLessonStore.parseInstant(lesson.endIso)
                val isCurrent = !lesson.isCancelled && s != null && e != null && !now.isBefore(s) && now.isBefore(e)
                val rowBg = when {
                    lesson.isCancelled -> GlanceTheme.colors.errorContainer
                    isCurrent -> GlanceTheme.colors.primaryContainer
                    else -> GlanceTheme.colors.surfaceVariant
                }
                val fg = when {
                    lesson.isCancelled -> GlanceTheme.colors.onErrorContainer
                    isCurrent -> GlanceTheme.colors.onPrimaryContainer
                    else -> GlanceTheme.colors.onSurface
                }
                val sub = listOfNotNull(
                    NextLessonStore.formatClock(lesson.startIso, zone).takeIf { it.isNotBlank() },
                    lesson.room?.takeIf { it.isNotBlank() },
                    if (lesson.isCancelled) "Uitval" else null,
                ).joinToString(" · ")

                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(14.dp)
                        .background(rowBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        lesson.periodFrom?.toString() ?: "·",
                        style = TextStyle(color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        lesson.subject,
                        style = TextStyle(color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        sub,
                        style = TextStyle(color = fg, fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
                if (idx < listToShow.lastIndex) {
                    Spacer(GlanceModifier.height(5.dp))
                }
            }
        }
    }
}

class TodayScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayScheduleWidget()

    companion object {
        suspend fun updateAll(context: Context) {
            TodayScheduleWidget().updateAll(context)
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Laatste cijfers (Material You)
// ---------------------------------------------------------------------------

class LatestGradesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = NextLessonStore.readFull(context)
        provideContent {
            MagsWidgetTheme {
                LatestGradesContent(snap)
            }
        }
    }
}

@Composable
private fun LatestGradesContent(snap: MagsWidgetSnapshot) {
    val grades = snap.latestGrades.take(3)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(14.dp)
            .clickable(openPageAction("cijfers")),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Laatste cijfers",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (snap.openHomeworkCount > 0) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(10.dp)
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "${snap.openHomeworkCount} huiswerk",
                        style = TextStyle(
                            color = GlanceTheme.colors.onTertiaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }

        Spacer(GlanceModifier.height(8.dp))

        if (grades.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Open Mags om je cijfers te laden",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
            }
        } else {
            grades.forEachIndexed { idx, g ->
                val pillBg = when {
                    g.numericValue != null && g.numericValue < 5.5 -> GlanceTheme.colors.errorContainer
                    g.numericValue != null && g.numericValue >= 7.0 -> GlanceTheme.colors.primaryContainer
                    else -> GlanceTheme.colors.secondaryContainer
                }
                val pillFg = when {
                    g.numericValue != null && g.numericValue < 5.5 -> GlanceTheme.colors.onErrorContainer
                    g.numericValue != null && g.numericValue >= 7.0 -> GlanceTheme.colors.onPrimaryContainer
                    else -> GlanceTheme.colors.onSecondaryContainer
                }
                val meta = listOfNotNull(g.heading, g.weightLabel).joinToString(" · ")

                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(14.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            g.subject,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        if (meta.isNotBlank()) {
                            Text(
                                meta,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(10.dp)
                            .background(pillBg)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text(
                            g.valueLabel,
                            style = TextStyle(
                                color = pillFg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
                if (idx < grades.lastIndex) {
                    Spacer(GlanceModifier.height(5.dp))
                }
            }
        }
    }
}

class LatestGradesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LatestGradesWidget()

    companion object {
        suspend fun updateAll(context: Context) {
            LatestGradesWidget().updateAll(context)
        }
    }
}
