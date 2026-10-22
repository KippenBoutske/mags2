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
