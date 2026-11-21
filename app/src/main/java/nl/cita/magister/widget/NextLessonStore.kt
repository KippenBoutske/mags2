package nl.cita.magister.widget

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import nl.cita.magister.CitaApp
import nl.cita.magister.agenda.NextLesson
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.parse.MagisterJson
import nl.cita.magister.subject.displaySubject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class NextLessonSnapshot(
    val title: String,
    val startIso: String,
    val room: String?,
)

@Serializable
data class WidgetLesson(
    val subject: String,
    val startIso: String,
    val endIso: String,
    val periodFrom: Int? = null,
    val room: String? = null,
    val isCancelled: Boolean = false,
    val isChanged: Boolean = false,
)

@Serializable
data class WidgetGrade(
    val subject: String,
    val heading: String? = null,
    val valueLabel: String,
    val numericValue: Double? = null,
    val weightLabel: String? = null,
)

@Serializable
data class MagsWidgetSnapshot(
    val lessons: List<WidgetLesson> = emptyList(),
    val latestGrades: List<WidgetGrade> = emptyList(),
    val openHomeworkCount: Int = 0,
    val updatedAtEpochMs: Long = 0L,
)

object NextLessonStore {
    private const val PREFS = "mags_widget"
    private const val KEY = "next_lesson"
    private const val KEY_FULL = "mags_widget_snapshot_v2"

    fun write(context: Context, lesson: Appointment?) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (lesson == null) {
            prefs.edit().remove(KEY).apply()
            return
        }
        val snap = NextLessonSnapshot(
            title = lesson.displaySubject(),
            startIso = lesson.startIso,
            room = lesson.classroom ?: lesson.location,
        )
        prefs.edit().putString(KEY, MagisterJson.instance.encodeToString(NextLessonSnapshot.serializer(), snap)).apply()
    }

    fun read(context: Context): NextLessonSnapshot? {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return null
        return runCatching {
            MagisterJson.instance.decodeFromString(NextLessonSnapshot.serializer(), raw)
        }.getOrNull()
    }

    fun writeFull(
        context: Context,
        appointments: List<Appointment>,
        latestGrades: List<Grade>,
        homework: List<HomeworkItem>,
    ) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val endDate = today.plusDays(3)
        val upcomingLessons = appointments
            .filter {
                val date = parseLocalDate(it.startIso, zone)
                date != null && !date.isBefore(today) && !date.isAfter(endDate)
            }
            .sortedBy { it.startIso }
            .take(24)
            .map {
                WidgetLesson(
                    subject = it.displaySubject(),
                    startIso = it.startIso,
                    endIso = it.endIso,
                    periodFrom = it.periodFrom,
                    room = it.classroom ?: it.location,
                    isCancelled = it.isCancelled,
                    isChanged = it.isChanged,
                )
            }

        val topGrades = latestGrades.take(5).map { g ->
            WidgetGrade(
                subject = g.displaySubject(),
                heading = g.columnHeading?.takeIf { it.isNotBlank() },
                valueLabel = GradeCalculator.formatGradeValue(g.numericValue, g.value),
                numericValue = g.numericValue ?: GradeCalculator.parseDutchNumber(g.value),
                weightLabel = GradeCalculator.formatWeight(g.weight),
            )
        }

        val snap = MagsWidgetSnapshot(
            lessons = upcomingLessons,
            latestGrades = topGrades,
            openHomeworkCount = homework.count { !it.isDone },
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FULL, MagisterJson.instance.encodeToString(MagsWidgetSnapshot.serializer(), snap))
            .apply()
        write(context, NextLesson.pick(appointments))
    }

    fun readFull(context: Context): MagsWidgetSnapshot {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FULL, null)
        if (raw != null) {
            runCatching {
                return MagisterJson.instance.decodeFromString(MagsWidgetSnapshot.serializer(), raw)
            }
        }
        val legacy = read(context)
        return if (legacy != null) {
            MagsWidgetSnapshot(
                lessons = listOf(
                    WidgetLesson(
                        subject = legacy.title,
                        startIso = legacy.startIso,
                        endIso = "",
                        room = legacy.room,
                    ),
                ),
            )
        } else {
            MagsWidgetSnapshot()
        }
    }

    internal fun parseInstant(iso: String): Instant? {
        if (iso.isBlank()) return null
        return runCatching { Instant.parse(iso) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
    }

    internal fun parseLocalDate(iso: String, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
        parseInstant(iso)?.atZone(zone)?.toLocalDate()

    internal fun formatClock(iso: String, zone: ZoneId = ZoneId.systemDefault()): String =
        parseInstant(iso)?.atZone(zone)?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
}

object MagsWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { sync(appContext) }
        }
    }

    suspend fun sync(context: Context) {
        val app = context.applicationContext as? CitaApp ?: return
        val repo = app.container.repository
        val appointments = runCatching { repo.cachedAppointments() }.getOrDefault(emptyList())
        val latestGrades = runCatching { repo.observeLatestGrades().first() }.getOrDefault(emptyList())
        val homework = runCatching { repo.observeHomework().first() }.getOrDefault(emptyList())
        NextLessonStore.writeFull(context, appointments, latestGrades, homework)
        updateAllWidgets(context)
    }

    suspend fun updateAllWidgets(context: Context) {
        runCatching { NextLessonWidgetReceiver.updateAll(context) }
        runCatching { TodayScheduleWidgetReceiver.updateAll(context) }
        runCatching { LatestGradesWidgetReceiver.updateAll(context) }
    }
}
