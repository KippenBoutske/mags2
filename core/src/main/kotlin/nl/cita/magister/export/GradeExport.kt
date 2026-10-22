package nl.cita.magister.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Grade
import nl.cita.magister.model.StudentProfile
import java.time.Instant

/**
 * JSON-payload voor de Plan Goon (Grok Bot) webhook.
 * Schema staat ook in de README.
 */
@Serializable
data class GradeExportPayload(
    val source: String = "magister-app",
    @SerialName("exported_at") val exportedAt: String,
    val student: ExportStudent,
    val subjects: List<ExportSubject>,
    val attention: List<ExportAttention>,
)

@Serializable
data class ExportStudent(
    val name: String,
    @SerialName("class") val className: String?,
    val study: String?,
    val school: String,
    val tenant: String,
)

@Serializable
data class ExportSubject(
    val code: String,
    val name: String,
    val average: Double?,
    val weighted: Boolean,
    val grades: List<ExportGrade>,
)

@Serializable
data class ExportGrade(
    val value: Double?,
    val weight: Double?,
    val description: String?,
    val date: String?,
)

@Serializable
data class ExportAttention(
    val code: String,
    val name: String,
    val reason: String,
    val average: Double?,
)

object GradeExport {
    const val ATTENTION_THRESHOLD = 6.0
    const val TREND_DROP = 0.3

    fun build(
        profile: StudentProfile,
        grades: List<Grade>,
        exportedAt: Instant = Instant.now(),
    ): GradeExportPayload {
        val bySubject = grades.groupBy { it.subjectId }
        val subjects = bySubject.values.map { items ->
            val first = items.first()
            val counted = GradeCalculator.countedGrades(items)
            val summary = GradeCalculator.averagesBySubject(items).firstOrNull()
            ExportSubject(
                code = first.subjectCode,
                name = first.subjectName,
                average = summary?.average?.let { GradeCalculator.round2(it) },
                weighted = counted.any { (it.weight ?: 1.0) != 1.0 },
                grades = counted.sortedBy { it.enteredAt.orEmpty() }.map { grade ->
                    ExportGrade(
                        value = grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value),
                        weight = grade.weight,
                        description = grade.columnDescription ?: grade.columnHeading,
                        date = grade.enteredAt,
                    )
                },
            )
        }.sortedBy { it.name.lowercase() }

        return GradeExportPayload(
            exportedAt = exportedAt.toString(),
            student = ExportStudent(
                name = listOf(profile.firstName, profile.lastName).filter { it.isNotBlank() }.joinToString(" "),
                className = profile.className,
                study = profile.study,
                school = profile.schoolName,
                tenant = profile.tenantHost,
            ),
            subjects = subjects,
            attention = attention(subjects),
        )
    }

    fun attention(subjects: List<ExportSubject>): List<ExportAttention> {
        return subjects.mapNotNull { subject ->
            val reasons = mutableListOf<String>()
            val average = subject.average
            if (average != null && average < ATTENTION_THRESHOLD) {
                reasons += "gemiddelde ${formatNl(average)} ligt onder de 6,0"
            }
            if (isTrendingDown(subject.grades)) {
                reasons += "laatste cijfer is lager dan het eerdere gemiddelde"
            }
            if (reasons.isEmpty()) return@mapNotNull null
            ExportAttention(
                code = subject.code,
                name = subject.name,
                reason = reasons.joinToString("; "),
                average = average,
            )
        }
    }

    fun isTrendingDown(grades: List<ExportGrade>): Boolean {
        val values = grades.mapNotNull { it.value }
        if (values.size < 2) return false
        val last = values.last()
        val previous = values.dropLast(1)
        val prevAvg = previous.average()
        if (last <= prevAvg - TREND_DROP) return true
        if (values.size >= 3) {
            val a = values[values.lastIndex - 2]
            val b = values[values.lastIndex - 1]
            val c = values.last()
            return c < b && b < a
        }
        return false
    }

    private fun formatNl(value: Double): String = String.format("%.1f", value).replace('.', ',')
}
