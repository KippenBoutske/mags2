package nl.cita.magister.fixtures

import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import nl.cita.magister.parse.MagisterParsers
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class DemoFixture {
    Default,
    Vacation,
    NoGrades,
    LongNames,
    ManyHomework,
    ;

    companion object {
        fun parse(raw: String?): DemoFixture = when (raw?.trim()?.lowercase()) {
            "vacation", "vakantie", "empty" -> Vacation
            "nogrades", "no-grades", "cijfers" -> NoGrades
            "longnames", "long", "lang" -> LongNames
            "manyhw", "huiswerk", "many" -> ManyHomework
            else -> Default
        }
    }
}

fun MockFixtures.appointments(fixture: DemoFixture): List<Appointment> = when (fixture) {
    DemoFixture.Vacation -> emptyList()
    DemoFixture.LongNames -> longNameAppointments()
    DemoFixture.ManyHomework -> manyHomeworkAppointments()
    else -> appointments()
}

fun MockFixtures.grades(fixture: DemoFixture): List<Grade> = when (fixture) {
    DemoFixture.NoGrades -> emptyList()
    DemoFixture.LongNames -> longNameGrades()
    else -> grades()
}

fun MockFixtures.homework(fixture: DemoFixture): List<HomeworkItem> = when (fixture) {
    DemoFixture.Vacation -> emptyList()
    DemoFixture.ManyHomework -> manyHomeworkItems()
    DemoFixture.LongNames -> MagisterParsers.homeworkFromAppointments(appointments(fixture))
    else -> homework()
}

private val amsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")

private fun MockFixtures.longNameAppointments(): List<Appointment> {
    val longTitle = "Maatschappijwetenschappen en levensbeschouwing"
    val longBody = "Schrijf een beschouwing van minstens vierhonderd woorden over de verhouding tussen burger, overheid en media, en lever die in via Magister vóór het begin van de les."
    val stretch = setOf("Wiskunde B", "Biologie", "Geschiedenis")
    return appointments().map { item ->
        if (item.title !in stretch) item else item.copy(
            title = longTitle,
            subjects = listOf(longTitle),
            content = longBody,
        )
    }
}

private fun MockFixtures.longNameGrades(): List<Grade> {
    val longName = "Maatschappijwetenschappen en levensbeschouwing"
    return grades().map { grade ->
        if (grade.subjectId != 1) grade else grade.copy(subjectName = longName)
    }
}

private fun MockFixtures.manyHomeworkAppointments(): List<Appointment> {
    val base = appointments()
    val extras = manyHomeworkItems().map { item ->
        Appointment(
            id = item.id,
            startIso = item.dueIso,
            endIso = item.dueIso,
            periodFrom = 1,
            periodTo = 1,
            title = item.subject,
            location = item.location,
            classroom = item.location,
            teachers = listOf("Docent"),
            subjects = listOf(item.subject),
            content = item.description,
            infoType = item.infoType,
            isCancelled = false,
            isCompleted = item.isDone,
            hasAttachments = false,
        )
    }
    return base + extras
}

fun MockFixtures.manyHomeworkItems(today: LocalDate = LocalDate.of(2026, 10, 22)): List<HomeworkItem> {
    val subjects = listOf(
        "Nederlands", "Engels", "Wiskunde B", "Natuurkunde", "Scheikunde",
        "Biologie", "Geschiedenis", "Aardrijkskunde", "CKV", "LO",
    )
    val types = listOf(
        InfoType.Homework, InfoType.Homework, InfoType.Test,
        InfoType.Written, InfoType.Homework, InfoType.Oral,
    )
    return (0 until 32).map { index ->
        val date = today.plusDays((index % 11) - 4L)
        val type = types[index % types.size]
        HomeworkItem(
            id = 9100 + index,
            appointmentId = 9100 + index,
            dueIso = date.atTime(LocalTime.of(9, 30)).atZone(amsterdam).toInstant().toString(),
            subject = subjects[index % subjects.size],
            title = subjects[index % subjects.size],
            description = when (type) {
                InfoType.Test -> "Proefwerk hoofdstuk ${index % 8 + 1}."
                InfoType.Written -> "SO par. ${index % 6 + 1}."
                InfoType.Oral -> "Mondeling presentatie."
                else -> "Opdracht ${index + 1}: lees en maak de vragen."
            },
            location = "Griftdijk",
            infoType = type,
            isDone = index % 9 == 0,
            source = HomeworkItem.Source.Appointment,
        )
    }
}
