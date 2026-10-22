package nl.cita.magister.parse

import kotlinx.serialization.json.Json
import nl.cita.magister.dto.AccountDto
import nl.cita.magister.dto.AppointmentDto
import nl.cita.magister.dto.AssignmentDto
import nl.cita.magister.dto.EnrollmentDto
import nl.cita.magister.dto.GradeColumnInfoDto
import nl.cita.magister.dto.GradeDto
import nl.cita.magister.dto.ItemsDto
import nl.cita.magister.dto.MessageDto
import nl.cita.magister.dto.MessageFolderDto
import nl.cita.magister.dto.RecentGradeDto
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.GradeColumnType
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import nl.cita.magister.model.MessageDetail
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.StudentProfile

object MagisterJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }
}

object MagisterParsers {
    fun parseAccount(json: String): AccountDto = MagisterJson.instance.decodeFromString(json)

    fun parseProfile(
        json: String,
        tenantHost: String,
        schoolName: String,
        enrollment: EnrollmentDto? = null,
    ): StudentProfile {
        val account = parseAccount(json)
        val person = account.person ?: error("Account-JSON bevat geen Persoon")
        val last = person.resolvedLast
        return StudentProfile(
            personId = person.resolvedId ?: error("Persoon zonder id"),
            firstName = person.resolvedFirst,
            lastName = last,
            className = enrollment?.group?.description ?: enrollment?.group?.code,
            study = enrollment?.study?.description ?: enrollment?.study?.code,
            enrollmentId = enrollment?.resolvedId,
            schoolName = schoolName,
            tenantHost = tenantHost,
        )
    }

    fun parseEnrollments(json: String): List<EnrollmentDto> =
        MagisterJson.instance.decodeFromString<ItemsDto<EnrollmentDto>>(json).all

    fun currentEnrollment(enrollments: List<EnrollmentDto>, nowIsoDate: String): EnrollmentDto? {
        val matching = enrollments.filter { enrollment ->
            val start = enrollment.start?.take(10)
            val end = enrollment.end?.take(10)
            (start == null || start <= nowIsoDate) && (end == null || end >= nowIsoDate)
        }
        return matching.lastOrNull() ?: enrollments.maxByOrNull { it.end.orEmpty() }
    }

    fun parseGrades(json: String, extraWeights: Map<Int, Double> = emptyMap()): List<Grade> {
        val items = MagisterJson.instance.decodeFromString<ItemsDto<GradeDto>>(json).all
        return items.mapNotNull { dto -> mapGrade(dto, extraWeights) }
    }

    fun parseRecentGrades(json: String): List<Grade> {
        val items = MagisterJson.instance.decodeFromString<ItemsDto<RecentGradeDto>>(json).all
        return items.mapIndexed { index, dto ->
            val value = dto.value
            Grade(
                id = dto.columnId ?: -(index + 1),
                value = value,
                numericValue = GradeCalculator.parseDutchNumber(value),
                isSufficient = dto.isSufficient ?: ((GradeCalculator.parseDutchNumber(value) ?: 0.0) >= 5.5),
                counts = true,
                weight = dto.weight,
                subjectId = dto.subject?.id ?: 0,
                subjectName = dto.subject?.name ?: "Onbekend",
                subjectCode = dto.subject?.code ?: "?",
                columnHeading = dto.description,
                columnDescription = dto.description,
                columnType = GradeColumnType.Grade,
                teacher = null,
                enteredAt = dto.enteredAt,
                periodName = null,
                isPta = false,
            )
        }
    }

    fun parseColumnInfo(json: String): GradeColumnInfoDto =
        MagisterJson.instance.decodeFromString(json)

    fun parseAppointments(json: String): List<Appointment> {
        val items = MagisterJson.instance.decodeFromString<ItemsDto<AppointmentDto>>(json).all
        return items.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            Appointment(
                id = id,
                startIso = dto.start.orEmpty(),
                endIso = dto.end.orEmpty(),
                periodFrom = dto.periodFrom,
                periodTo = dto.periodTo,
                title = dto.title ?: "Les",
                location = dto.location,
                classroom = dto.rooms?.firstOrNull()?.let { it.name ?: it.nameAlt ?: it.code },
                teachers = dto.teachers?.mapNotNull { it.name ?: it.code } ?: emptyList(),
                subjects = dto.subjects?.mapNotNull { it.name ?: it.nameAlt ?: it.description ?: it.code }
                    ?: emptyList(),
                content = dto.content?.takeIf { it.isNotBlank() },
                infoType = InfoType.from(dto.infoType),
                isCancelled = dto.status == 5,
                isChanged = dto.status == 4,
                isCompleted = dto.completed == true,
                hasAttachments = dto.hasAttachments == true,
            )
        }
    }

    fun homeworkFromAppointments(appointments: List<Appointment>): List<HomeworkItem> {
        return appointments.filter { appointment ->
            appointment.infoType == InfoType.Homework ||
                appointment.infoType == InfoType.Test ||
                appointment.infoType == InfoType.Written ||
                appointment.infoType == InfoType.Oral ||
                !appointment.content.isNullOrBlank()
        }.map { appointment ->
            HomeworkItem(
                id = appointment.id,
                appointmentId = appointment.id,
                dueIso = appointment.startIso,
                subject = appointment.subjects.firstOrNull() ?: appointment.title,
                title = appointment.title,
                description = appointment.content,
                location = appointment.location ?: appointment.classroom,
                infoType = appointment.infoType,
                isDone = appointment.isCompleted,
                source = HomeworkItem.Source.Appointment,
            )
        }
    }

    fun parseAssignments(json: String): List<HomeworkItem> {
        val items = MagisterJson.instance.decodeFromString<ItemsDto<AssignmentDto>>(json).all
        return items.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            HomeworkItem(
                id = id,
                appointmentId = null,
                dueIso = dto.due.orEmpty(),
                subject = dto.subject?.name ?: dto.subject?.nameAlt ?: dto.subject?.code ?: "Opdracht",
                title = dto.title ?: "Opdracht",
                description = dto.description,
                location = null,
                infoType = InfoType.Homework,
                isDone = dto.closed == true,
                source = HomeworkItem.Source.Assignment,
            )
        }
    }

    fun parseFolders(json: String): List<MessageFolderDto> =
        MagisterJson.instance.decodeFromString<ItemsDto<MessageFolderDto>>(json).all

    fun parseMessages(json: String): List<MessagePreview> {
        val items = MagisterJson.instance.decodeFromString<ItemsDto<MessageDto>>(json).all
        return items.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            MessagePreview(
                id = id,
                subject = dto.subject ?: "(geen onderwerp)",
                sender = dto.sender?.name ?: "Onbekend",
                sentAt = dto.sentAt,
                isRead = dto.isRead == true,
                hasAttachments = dto.hasAttachments == true,
                snippet = dto.body?.stripHtml()?.take(160),
                href = dto.links?.get("self")?.href,
            )
        }
    }

    fun parseMessageDetail(json: String, fallback: MessagePreview? = null): MessageDetail {
        val dto = MagisterJson.instance.decodeFromString<MessageDto>(json)
        val preview = MessagePreview(
            id = dto.id ?: fallback?.id ?: 0,
            subject = dto.subject ?: fallback?.subject ?: "(geen onderwerp)",
            sender = dto.sender?.name ?: fallback?.sender ?: "Onbekend",
            sentAt = dto.sentAt ?: fallback?.sentAt,
            isRead = dto.isRead ?: fallback?.isRead ?: false,
            hasAttachments = dto.hasAttachments ?: fallback?.hasAttachments ?: false,
            snippet = dto.body?.stripHtml()?.take(160),
            href = dto.links?.get("self")?.href ?: fallback?.href,
        )
        return MessageDetail(
            preview = preview,
            bodyHtml = dto.body,
            recipients = emptyList(),
        )
    }

    fun columnType(code: Int?): GradeColumnType = when (code) {
        1 -> GradeColumnType.Grade
        2, 13 -> if (code == 13) GradeColumnType.End else GradeColumnType.Average
        else -> GradeColumnType.Other
    }

    private fun mapGrade(dto: GradeDto, extraWeights: Map<Int, Double>): Grade? {
        val id = dto.id ?: return null
        val subject = dto.subject
        val column = dto.column
        val columnId = column?.id
        val weight = extraWeights[columnId] ?: column?.weight ?: dto.weightAlt
        val type = columnType(column?.type)
        return Grade(
            id = id,
            value = dto.value,
            numericValue = GradeCalculator.parseDutchNumber(dto.value),
            isSufficient = dto.isSufficient ?: ((GradeCalculator.parseDutchNumber(dto.value) ?: 0.0) >= 5.5),
            counts = dto.counts ?: (type == GradeColumnType.Grade),
            weight = weight,
            subjectId = subject?.id ?: 0,
            subjectName = subject?.name ?: subject?.nameAlt ?: "Onbekend",
            subjectCode = subject?.code ?: "?",
            columnHeading = column?.heading ?: column?.name,
            columnDescription = column?.description,
            columnType = type,
            teacher = dto.teacher ?: dto.enteredBy,
            enteredAt = dto.enteredAt,
            periodName = dto.period?.name,
            isPta = column?.isPta == true,
        )
    }
}

fun String.stripHtml(): String = replace(Regex("<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace(Regex("\\s+"), " ")
    .trim()
