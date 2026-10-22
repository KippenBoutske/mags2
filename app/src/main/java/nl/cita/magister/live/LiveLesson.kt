package nl.cita.magister.live

import nl.cita.magister.model.Appointment
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.ui.parseInstant
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class LiveSegment(val lengthMinutes: Int, val color: Int)

data class LiveSnapshot(
    val title: String,
    val text: String,
    val chip: String,
    val whenMillis: Long,
    val countdown: Boolean,
    val progress: Int,
    val segments: List<LiveSegment>,
    val nextAlarmMillis: Long,
)

object LiveLesson {
    const val LeadMinutes = 40L
    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")

    private const val ColorLesson = 0xFFC4A36A.toInt()
    private const val ColorChanged = 0xFFC47A4A.toInt()
    private const val ColorCancelled = 0xFFF0B8AA.toInt()
    private const val ColorBreak = 0xFF8A7B64.toInt()

    fun snapshot(appointments: List<Appointment>, now: Instant = Instant.now()): LiveSnapshot? {
        val day = schoolDay(appointments, now) ?: return null
        val firstStart = day.first().start ?: return null
        val lastEnd = day.last().end ?: return null
        val showFrom = firstStart.minus(Duration.ofMinutes(LeadMinutes))
        if (now.isBefore(showFrom) || !now.isBefore(lastEnd)) return null

        val current = day.firstOrNull { it.contains(now) }
        val upcoming = day.firstOrNull { it.start?.isAfter(now) == true }
        val focus = current ?: upcoming ?: return null
        val title: String
        val text: String
        val target: Instant
        val countdown: Boolean
        if (current != null) {
            title = if (current.item.isCancelled) "Uitval: ${current.name}" else "Nu: ${current.name}"
            text = buildString {
                append("tot ${current.endLabel}")
                current.room?.let { append(" · $it") }
                if (current.item.isChanged && !current.item.isCancelled) append(" · gewijzigd")
            }
            target = current.end ?: now
            countdown = true
        } else {
            title = if (focus.item.isCancelled) "Uitval: ${focus.name}" else "Volgende: ${focus.name}"
            text = buildString {
                append(focus.startLabel)
                focus.room?.let { append(" · $it") }
                if (focus.item.isCancelled) append(" · Uitval")
                else if (focus.item.isChanged) append(" · gewijzigd")
            }
            target = focus.start ?: now
            countdown = true
        }

        val remainingMin = Duration.between(now, target).toMinutes()
        val chip = when {
            current?.item?.isCancelled == true -> "Uitval"
            remainingMin in 1..99 -> "$remainingMin min"
            else -> (if (current != null) current.endLabel else focus.startLabel).take(5)
        }

        val segments = segmentsOf(day)
        val elapsed = Duration.between(firstStart, now).toMinutes().toInt().coerceAtLeast(0)
        val nextAlarm = nextBoundary(day, now, lastEnd) ?: lastEnd
        return LiveSnapshot(
            title = title,
            text = text,
            chip = chip.take(7),
            whenMillis = target.toEpochMilli(),
            countdown = countdown,
            progress = elapsed,
            segments = segments,
            nextAlarmMillis = nextAlarm.toEpochMilli(),
        )
    }

    fun nextWake(appointments: List<Appointment>, now: Instant = Instant.now()): Instant? {
        val today = now.atZone(zone).toLocalDate()
        val todayDay = schoolDay(appointments, now)
        if (todayDay != null) {
            val firstStart = todayDay.first().start ?: return null
            val lastEnd = todayDay.last().end ?: return null
            val showFrom = firstStart.minus(Duration.ofMinutes(LeadMinutes))
            if (now.isBefore(showFrom)) return showFrom
            if (now.isBefore(lastEnd)) return nextBoundary(todayDay, now, lastEnd) ?: lastEnd
        }
        var cursor = today.plusDays(1)
        repeat(10) {
            val instant = cursor.atTime(7, 0).atZone(zone).toInstant()
            val day = schoolDay(appointments, instant)
            if (day != null) {
                val start = day.first().start ?: return instant
                return start.minus(Duration.ofMinutes(LeadMinutes))
            }
            cursor = cursor.plusDays(1)
        }
        return today.plusDays(1).atTime(7, 0).atZone(zone).toInstant()
    }

    private fun schoolDay(appointments: List<Appointment>, now: Instant): List<TimedLesson>? {
        val date = now.atZone(zone).toLocalDate()
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) return null
        val day = appointments.mapNotNull { TimedLesson.from(it) }
            .filter { it.date == date }
            .sortedBy { it.start }
        return day.takeIf { it.isNotEmpty() }
    }

    private fun nextBoundary(day: List<TimedLesson>, now: Instant, lastEnd: Instant): Instant? {
        val marks = day.flatMap { listOfNotNull(it.start, it.end) }.filter { it.isAfter(now) }
        return marks.minOrNull() ?: lastEnd.takeIf { it.isAfter(now) }
    }

    private fun segmentsOf(day: List<TimedLesson>): List<LiveSegment> {
        val out = ArrayList<LiveSegment>(day.size * 2)
        day.forEachIndexed { index, lesson ->
            if (index > 0) {
                val prev = day[index - 1]
                val gap = minutesBetween(prev.end, lesson.start)
                if (gap >= 5) out += LiveSegment(gap, ColorBreak)
            }
            out += LiveSegment(
                lengthMinutes = minutesBetween(lesson.start, lesson.end).coerceAtLeast(1),
                color = when {
                    lesson.item.isCancelled -> ColorCancelled
                    lesson.item.isChanged -> ColorChanged
                    else -> ColorLesson
                },
            )
        }
        return out
    }

    private fun minutesBetween(from: Instant?, to: Instant?): Int {
        if (from == null || to == null) return 1
        return Duration.between(from, to).toMinutes().toInt().coerceAtLeast(0)
    }

    private data class TimedLesson(
        val item: Appointment,
        val start: Instant?,
        val end: Instant?,
        val date: java.time.LocalDate,
        val name: String,
        val room: String?,
        val startLabel: String,
        val endLabel: String,
    ) {
        fun contains(now: Instant): Boolean {
            val s = start ?: return false
            val e = end ?: return false
            return !now.isBefore(s) && now.isBefore(e)
        }

        companion object {
            fun from(item: Appointment): TimedLesson? {
                val start = parseInstant(item.startIso) ?: return null
                val end = parseInstant(item.endIso) ?: start.plus(Duration.ofMinutes(45))
                val date = start.atZone(zone).toLocalDate()
                val room = item.classroom?.takeIf { it.isNotBlank() }
                    ?: item.location?.takeIf { it.isNotBlank() }
                val timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(zone)
                return TimedLesson(
                    item = item,
                    start = start,
                    end = end,
                    date = date,
                    name = item.displaySubject(),
                    room = room,
                    startLabel = timeFmt.format(start),
                    endLabel = timeFmt.format(end),
                )
            }
        }
    }
}
