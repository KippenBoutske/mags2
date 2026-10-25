package nl.cita.magister.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AppClock(
    val date: LocalDate,
    val time: LocalTime,
) {
    fun millis(): Long = date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    companion object {
        val zone: ZoneId = ZoneId.of("Europe/Amsterdam")

        /** Fixture Thursday during Geschiedenis (10:40–12:15). */
        val Demo = AppClock(LocalDate.of(2026, 10, 22), LocalTime.of(11, 0))

        /** Wednesday before first lesson — Vandaag-hero is VOLGENDE LES. */
        val BeforeFirstLesson = AppClock(LocalDate.of(2026, 10, 21), LocalTime.of(7, 0))

        fun system(): AppClock = AppClock(LocalDate.now(zone), LocalTime.now(zone))

        fun parse(raw: String?): AppClock? {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) return null
            runCatching {
                val dateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                return AppClock(dateTime.toLocalDate(), dateTime.toLocalTime())
            }
            runCatching {
                val dateTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                return AppClock(dateTime.toLocalDate(), dateTime.toLocalTime())
            }
            return null
        }
    }
}
