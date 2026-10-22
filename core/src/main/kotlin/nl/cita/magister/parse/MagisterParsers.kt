package nl.cita.magister.parse

import kotlinx.serialization.json.Json
import nl.cita.magister.dto.AbsenceDto
import nl.cita.magister.dto.AccountDto
import nl.cita.magister.dto.AppointmentDto
import nl.cita.magister.dto.AssignmentDto
import nl.cita.magister.dto.AttachmentDto
import nl.cita.magister.dto.EnrollmentDto
import nl.cita.magister.dto.GradeColumnInfoDto
import nl.cita.magister.dto.GradeDto
import nl.cita.magister.dto.ItemsDto
import nl.cita.magister.dto.MessageDto
import nl.cita.magister.dto.MessageFolderDto
import nl.cita.magister.dto.RecentGradeDto
import nl.cita.magister.dto.StudyGuideDto
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Absence
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Attachment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.GradeColumnType
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import nl.cita.magister.model.MessageDetail
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.StudyGuide

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
            val value = dto.resolvedValue
            val numeric = GradeCalculator.parseDutchNumber(value)
            val description = dto.resolvedDescription
            val inferred = columnType(null, description, description, description)
            val type = if (inferred == GradeColumnType.Other) GradeColumnType.Grade else inferred
            val subject = dto.resolvedSubject
            Grade(
                id = dto.resolvedColumnId ?: -(index + 1),
                value = value,
                numericValue = numeric,
                isSufficient = dto.resolvedSufficient ?: ((numeric ?: 0.0) >= 5.5),
                counts = type == GradeColumnType.Grade && numeric != null,
                weight = dto.resolvedWeight,
                subjectId = subject?.resolvedId ?: 0,
                subjectName = subject?.resolvedName ?: "Onbekend",
                subjectCode = subject?.resolvedCode ?: "?",
                columnHeading = description,
                columnDescription = description,
                columnType = type,
                teacher = null,
                enteredAt = dto.resolvedEnteredAt,
                periodName = null,
                isPta = false,
                columnId = dto.resolvedColumnId,
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
            attachments = emptyList(),
        )
    }

    fun parseAttachments(json: String): List<Attachment> {
        val items = MagisterJson.instance.decodeFromString<ItemsDto<AttachmentDto>>(json).all
        return items.mapNotNull { dto ->
            val name = dto.name ?: dto.nameAlt ?: return@mapNotNull null
            Attachment(
                name = name,
                contentType = dto.contentType,
                sizeBytes = dto.size,
                href = dto.links?.get("self")?.href ?: dto.links?.get("download")?.href,
            )
        }
    }

    fun parseAbsences(json: String): List<Absence> {
        val items = MagisterJson.instance.decodeFromString<ItemsDto<AbsenceDto>>(json).all
        return items.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            Absence(
                id = id,
                startIso = dto.start,
                endIso = dto.end,
                period = dto.period,
                description = dto.description ?: "Afwezig",
                code = dto.code,
                excused = dto.excused == true,
                lesson = dto.appointment?.title,
            )
        }
    }

    fun parseStudyGuides(json: String, isProject: Boolean = false): List<StudyGuide> {
        val items = MagisterJson.instance.decodeFromString<ItemsDto<StudyGuideDto>>(json).all
        return items.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            StudyGuide(
                id = id,
                title = dto.title ?: if (isProject) "Project" else "Studiewijzer",
                subjectCodes = dto.subjectCodes.orEmpty(),
                fromIso = dto.from,
                toIso = dto.to,
                href = dto.links?.firstOrNull { it.resolvedRel.equals("Self", ignoreCase = true) }?.resolvedHref
                    ?: dto.links?.firstOrNull()?.resolvedHref,
                isProject = isProject,
            )
        }
    }

    fun columnType(
        code: Int?,
        name: String? = null,
        heading: String? = null,
        description: String? = null,
    ): GradeColumnType {
        if (looksLikeEnd(name, heading, description) || code == 13) return GradeColumnType.End
        if (looksLikeAverage(name, heading, description) || code == 2) return GradeColumnType.Average
        return when (code) {
            1 -> GradeColumnType.Grade
            else -> GradeColumnType.Other
        }
    }

    fun looksLikeAverage(name: String?, heading: String?, description: String?): Boolean {
        val headingNorm = heading?.trim()?.lowercase().orEmpty()
        if (headingNorm in setOf("gem", "vg", "se") || headingNorm.contains("se cijfer")) return true
        if (name?.trim()?.equals("gem", ignoreCase = true) == true) return true
        val blob = listOfNotNull(name, heading, description).joinToString(" ").lowercase()
        return blob.contains("gemiddelde") || blob.contains("voortschrijdend")
    }

    fun looksLikeEnd(name: String?, heading: String?, description: String?): Boolean {
        val headingNorm = heading?.trim()?.lowercase().orEmpty()
        if (headingNorm in setOf("eind", "eindc", "rap", "rapp")) return true
        val blob = listOfNotNull(name, heading, description).joinToString(" ").lowercase()
        return blob.contains("eindcijfer") || blob.contains("rapportcijfer")
    }

    fun applyColumnInfo(
        grades: List<Grade>,
        extra: Map<Int, nl.cita.magister.dto.GradeColumnInfoDto>,
        extraWeights: Map<Int, Double> = emptyMap(),
    ): List<Grade> {
        if (extra.isEmpty() && extraWeights.isEmpty()) return grades
        return grades.map { grade ->
            val columnId = grade.columnId ?: return@map grade
            val info = extra[columnId]
            val weight = extraWeights[columnId] ?: info?.resolvedWeight ?: grade.weight
            val type = if (info != null) {
                columnType(info.type, info.name ?: grade.columnHeading, info.heading ?: grade.columnHeading, info.description)
            } else {
                grade.columnType
            }
            val retakeOf = info?.resolvedRetakeOf
                ?: if (info?.isRetake == true || info?.isRetakeColumn == true) grade.replacesColumnId else grade.replacesColumnId
            val numeric = grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value)
            val counts = when {
                type != GradeColumnType.Grade -> false
                info?.counts == false || info?.countsInRunning == false -> false
                numeric == null -> false
                weight != null && weight <= 0.0 -> false
                else -> grade.counts
            }
            grade.copy(
                weight = weight,
                columnType = type,
                replacesColumnId = retakeOf ?: grade.replacesColumnId,
                counts = counts,
                columnHeading = info?.heading?.takeIf { it.isNotBlank() } ?: grade.columnHeading,
                columnDescription = info?.description?.takeIf { it.isNotBlank() } ?: grade.columnDescription,
            )
        }
    }

    private fun mapGrade(dto: GradeDto, extraWeights: Map<Int, Double>): Grade? {
        val id = dto.id ?: return null
        val subject = dto.subject
        val column = dto.column
        val columnId = column?.id
        val weight = extraWeights[columnId]
            ?: column?.weight
            ?: column?.weightRow
            ?: dto.weightPascal
            ?: dto.weightRow
            ?: dto.weightAlt
        val type = columnType(column?.type, column?.name, column?.heading, column?.description)
        val numeric = GradeCalculator.parseDutchNumber(dto.value)
        val retakeOf = column?.retakeOf ?: column?.retakeOfAlt
        val counts = when {
            type != GradeColumnType.Grade -> false
            dto.counts == false -> false
            numeric == null -> false
            weight != null && weight <= 0.0 -> false
            else -> dto.counts ?: true
        }
        return Grade(
            id = id,
            value = dto.value,
            numericValue = numeric,
            isSufficient = dto.isSufficient ?: ((numeric ?: 0.0) >= 5.5),
            counts = counts,
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
            columnId = columnId,
            replacesColumnId = retakeOf,
        )
    }
}

fun String.stripHtml(): String = replace(Regex("<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace(Regex("\\s+"), " ")
    .trim()
