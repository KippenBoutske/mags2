package nl.cita.magister.model

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    val personId: Int,
    val firstName: String,
    val lastName: String,
    val className: String?,
    val study: String?,
    val enrollmentId: Int?,
    val schoolName: String,
    val tenantHost: String,
)

@Serializable
data class Grade(
    val id: Int,
    val value: String?,
    val numericValue: Double?,
    val isSufficient: Boolean,
    val counts: Boolean,
    val weight: Double?,
    val subjectId: Int,
    val subjectName: String,
    val subjectCode: String,
    val columnHeading: String?,
    val columnDescription: String?,
    val columnType: GradeColumnType,
    val teacher: String?,
    val enteredAt: String?,
    val periodName: String?,
    val isPta: Boolean,
    val columnId: Int? = null,
    val replacesColumnId: Int? = null,
)

@Serializable
enum class GradeColumnType {
    Grade,
    Average,
    End,
    Other,
}

@Serializable
data class SubjectAverage(
    val subjectId: Int,
    val subjectName: String,
    val subjectCode: String,
    val average: Double?,
    val averageLabel: String? = null,
    val magisterAverage: Double?,
    val gradesCounted: Int,
    val weightSum: Double,
    val latest: Grade?,
)

@Serializable
data class Appointment(
    val id: Int,
    val startIso: String,
    val endIso: String,
    val periodFrom: Int?,
    val periodTo: Int?,
    val title: String,
    val location: String?,
    val classroom: String?,
    val teachers: List<String>,
    val subjects: List<String>,
    val content: String?,
    val infoType: InfoType,
    val isCancelled: Boolean,
    val isChanged: Boolean = false,
    val isCompleted: Boolean,
    val hasAttachments: Boolean,
)

@Serializable
enum class InfoType(val code: Int) {
    None(0),
    Homework(1),
    Test(2),
    Written(3),
    Oral(4),
    Information(5),
    Planner(6),
    Unknown(-1);

    companion object {
        fun from(code: Int?): InfoType = entries.firstOrNull { it.code == code } ?: Unknown
    }
}

@Serializable
data class HomeworkItem(
    val id: Int,
    val appointmentId: Int?,
    val dueIso: String,
    val subject: String,
    val title: String,
    val description: String?,
    val location: String?,
    val infoType: InfoType,
    val isDone: Boolean,
    val source: Source,
) {
    @Serializable
    enum class Source { Appointment, Assignment, Local }
}

@Serializable
data class MessagePreview(
    val id: Int,
    val subject: String,
    val sender: String,
    val sentAt: String?,
    val isRead: Boolean,
    val hasAttachments: Boolean,
    val snippet: String?,
    val href: String?,
)

@Serializable
data class Attachment(
    val name: String,
    val contentType: String? = null,
    val sizeBytes: Long? = null,
    val href: String? = null,
)

@Serializable
data class MessageDetail(
    val preview: MessagePreview,
    val bodyHtml: String?,
    val recipients: List<String>,
    val attachments: List<Attachment> = emptyList(),
)

@Serializable
data class Absence(
    val id: Int,
    val startIso: String?,
    val endIso: String?,
    val period: Int?,
    val description: String,
    val code: String?,
    val excused: Boolean,
    val lesson: String?,
)

@Serializable
data class StudyGuide(
    val id: Int,
    val title: String,
    val subjectCodes: List<String>,
    val fromIso: String?,
    val toIso: String?,
    val href: String?,
    val isProject: Boolean = false,
)
