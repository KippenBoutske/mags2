package nl.cita.magister.fixtures

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import nl.cita.magister.api.Tenant
import nl.cita.magister.dto.EnrollmentDto
import nl.cita.magister.model.Absence
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Attachment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessageDetail
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.StudyGuide
import nl.cita.magister.parse.MagisterJson
import nl.cita.magister.parse.MagisterParsers

/**
 * Duidelijk gemarkeerde voorbeelddata (5 VWO).
 * Gebruikt wanneer er geen live-login is, en als testdata voor parsers.
 */
object MockFixtures {
    const val MARKER = "MOCK"

    private fun load(name: String): String {
        val stream = checkNotNull(MockFixtures::class.java.classLoader.getResourceAsStream("fixtures/$name")) {
            "Fixture $name ontbreekt"
        }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    val accountJson get() = load("account.json")
    val enrollmentsJson get() = load("enrollments.json")
    val gradesJson get() = load("grades.json")
    val citadelGradesJson get() = load("grades-citadel.json")
    val recentGradesJson get() = load("grades-laatste.json")

    fun extraColumnInfoJson(columnId: Int): String {
        data class Extra(
            val weight: Double?,
            val type: Int,
            val heading: String,
            val name: String,
            val retakeOf: Int? = null,
            val counts: Boolean = true,
        )
        val extra = when (columnId) {
            101, 102, 103, 202, 301, 302 -> Extra(0.0, 2, "SE cijfer", "SE", counts = false)
            201 -> Extra(2.0, 1, "Klassenwerk", "KW1")
            303, 304 -> Extra(1.0, 1, "Toetsen/PO", "PO")
            401 -> Extra(3.0, 1, "PW1", "PW1")
            402 -> Extra(3.0, 1, "PW2", "PW2")
            411 -> Extra(3.0, 1, "PW1", "PW1H", retakeOf = 401)
            409 -> Extra(0.0, 2, "GEM", "gem", counts = false)
            601 -> Extra(1.0, 1, "PW1", "PW1")
            602 -> Extra(1.0, 1, "PW2", "PW2")
            603 -> Extra(1.0, 1, "Houding", "LO")
            else -> Extra(null, 1, "kolom $columnId", "K")
        }
        val weightJson = extra.weight?.let { """"Weging":$it,"Weegfactor":$it,""" }.orEmpty()
        val retakeJson = extra.retakeOf?.let { """"IsHerkansing":true,"HerkansingVanKolomId":$it,""" }.orEmpty()
        return """{"KolomNaam":"${extra.name}","KolomKop":"${extra.heading}","KolomOmschrijving":"${extra.heading}",$weightJson$retakeJson"KolomSoort":${extra.type},"TeltMee":${extra.counts}}"""
    }

    fun citadelGrades(withExtra: Boolean = true): List<Grade> {
        val parsed = MagisterParsers.parseGrades(citadelGradesJson)
        if (!withExtra) return parsed
        val extra = parsed.mapNotNull { it.columnId }.distinct().associateWith { columnId ->
            MagisterParsers.parseColumnInfo(extraColumnInfoJson(columnId))
        }
        return MagisterParsers.applyColumnInfo(parsed, extra)
    }

    val appointmentsJson get() = load("appointments.json")
    val messagesJson get() = load("messages.json")
    val foldersJson get() = load("message_folders.json")
    val absencesJson get() = load("absences.json")
    val studyGuidesJson get() = load("studyguides.json")
    val assignmentsJson get() = load("assignments.json")
    val attachmentsJson get() = load("attachments.json")

    fun profile(): StudentProfile {
        val enrollment: EnrollmentDto? = MagisterParsers.currentEnrollment(
            MagisterParsers.parseEnrollments(enrollmentsJson),
            "2026-10-21",
        )
        return MagisterParsers.parseProfile(
            json = accountJson,
            tenantHost = Tenant.CITADEL_HOST,
            schoolName = Tenant.CITADEL_DISPLAY,
            enrollment = enrollment,
        )
    }

    fun grades(): List<Grade> = MagisterParsers.parseGrades(gradesJson)

    fun appointments(): List<Appointment> = MagisterParsers.parseAppointments(appointmentsJson)

    fun homework(): List<HomeworkItem> = MagisterParsers.homeworkFromAppointments(appointments())

    fun messages(): List<MessagePreview> = MagisterParsers.parseMessages(messagesJson)

    fun absences(): List<Absence> = MagisterParsers.parseAbsences(absencesJson)

    fun studyGuides(): List<StudyGuide> = MagisterParsers.parseStudyGuides(studyGuidesJson)

    fun assignments(): List<HomeworkItem> = MagisterParsers.parseAssignments(assignmentsJson)

    fun attachments(): List<Attachment> = MagisterParsers.parseAttachments(attachmentsJson)

    fun message(id: Int): MessageDetail? {
        val preview = messages().firstOrNull { it.id == id } ?: return null
        val items = MagisterJson.instance.parseToJsonElement(messagesJson)
            .jsonObject["items"]
            ?.jsonArray
            .orEmpty()
        val raw = items.firstOrNull { element ->
            element.jsonObject["id"]?.toString() == id.toString()
        } ?: return MessageDetail(preview, preview.snippet, emptyList())
        val detail = MagisterParsers.parseMessageDetail(
            Json.encodeToString(JsonObject.serializer(), raw.jsonObject),
            preview,
        )
        return if (id == 9001) detail.copy(attachments = attachments()) else detail
    }
}
