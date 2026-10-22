package nl.cita.magister.grades

import nl.cita.magister.model.Grade
import nl.cita.magister.model.GradeColumnType
import nl.cita.magister.model.SubjectAverage
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

object GradeCalculator {
    private val nl = Locale("nl", "NL")

    fun parseDutchNumber(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
            .replace(" ", "")
            .replace(',', '.')
        return cleaned.toDoubleOrNull()?.takeIf { it in 1.0..10.0 }
    }

    fun numeric(grade: Grade): Double? = grade.numericValue ?: parseDutchNumber(grade.value)

    fun countedGrades(grades: List<Grade>): List<Grade> {
        val retakeByOriginal = grades
            .filter { it.replacesColumnId != null && numeric(it) != null }
            .associateBy { it.replacesColumnId }
        val withoutReplaced = grades.filter { grade ->
            val retake = retakeByOriginal[grade.columnId]
            retake == null || retake.id == grade.id
        }
        val latestByColumn = withoutReplaced
            .groupBy { it.columnId ?: -(it.id) }
            .map { (_, group) -> group.maxBy { "${it.enteredAt.orEmpty()}|${it.id}" } }
        return latestByColumn.filter { grade ->
            if (grade.columnType != GradeColumnType.Grade) return@filter false
            if (!grade.counts) return@filter false
            if (numeric(grade) == null) return@filter false
            val weight = grade.weight
            if (weight != null && weight <= 0.0) return@filter false
            true
        }
    }

    fun weightedAverage(grades: List<Grade>): Double? {
        val usable = countedGrades(grades).mapNotNull { grade ->
            val value = numeric(grade) ?: return@mapNotNull null
            val weight = (grade.weight ?: 1.0).takeIf { it > 0 } ?: return@mapNotNull null
            value to weight
        }
        if (usable.isEmpty()) return null
        val weightSum = usable.sumOf { it.second }
        if (weightSum <= 0.0) return null
        return usable.sumOf { it.first * it.second } / weightSum
    }

    /**
     * Wat moet je halen op de volgende toets om [target] te bereiken?
     * `needed = (target * (somWeging + nieuweWeging) - huidigeSom) / nieuweWeging`
     */
    fun scoreNeeded(
        currentGrades: List<Grade>,
        target: Double,
        nextWeight: Double,
    ): ScoreNeeded {
        require(target in 1.0..10.0) { "Doelcijfer moet tussen 1 en 10 liggen." }
        require(nextWeight > 0.0) { "Weging moet groter dan 0 zijn." }
        val usable = countedGrades(currentGrades).mapNotNull { grade ->
            val value = numeric(grade) ?: return@mapNotNull null
            val weight = (grade.weight ?: 1.0).takeIf { it > 0 } ?: return@mapNotNull null
            value to weight
        }
        val currentSum = usable.sumOf { it.first * it.second }
        val currentWeight = usable.sumOf { it.second }
        val needed = (target * (currentWeight + nextWeight) - currentSum) / nextWeight
        return ScoreNeeded(
            needed = needed,
            currentAverage = if (currentWeight > 0) currentSum / currentWeight else null,
            currentWeight = currentWeight,
            nextWeight = nextWeight,
            target = target,
            reachable = needed <= 10.0,
            alreadyReached = needed <= 1.0,
        )
    }

    fun pickMagisterAverage(grades: List<Grade>): Pair<Double, String?>? {
        data class Candidate(val rank: Int, val grade: Grade, val value: Double)
        val candidates = grades.mapNotNull { grade ->
            if (grade.columnType != GradeColumnType.Average && grade.columnType != GradeColumnType.End) {
                return@mapNotNull null
            }
            val value = numeric(grade) ?: return@mapNotNull null
            Candidate(averageRank(grade), grade, value)
        }
        if (candidates.isEmpty()) return null
        val bestRank = candidates.maxOf { it.rank }
        val best = candidates
            .filter { it.rank == bestRank }
            .maxBy { "${it.grade.enteredAt.orEmpty()}|${it.grade.id}" }
        val label = best.grade.value?.trim()?.takeIf { it.isNotEmpty() }
        return best.value to label
    }

    fun averagesBySubject(grades: List<Grade>): List<SubjectAverage> {
        return grades.groupBy { it.subjectId }.map { (subjectId, items) ->
            val first = items.first()
            val magister = pickMagisterAverage(items)
            val counted = countedGrades(items)
            val computed = weightedAverage(items)
            val average = magister?.first ?: computed
            SubjectAverage(
                subjectId = subjectId,
                subjectName = first.subjectName,
                subjectCode = first.subjectCode,
                average = average,
                averageLabel = magister?.second ?: formatAverage(average),
                magisterAverage = magister?.first,
                gradesCounted = counted.size,
                weightSum = counted.sumOf { it.weight ?: 1.0 },
                latest = counted.maxByOrNull { it.enteredAt.orEmpty() },
            )
        }.sortedBy { it.subjectName.lowercase() }
    }

    fun round1(value: Double): Double = round(value * 10.0) / 10.0

    fun round2(value: Double): Double = round(value * 100.0) / 100.0

    fun formatAverage(value: Double?, label: String? = null): String {
        val fromLabel = label?.trim()?.takeIf { it.isNotEmpty() && parseDutchNumber(it) != null }
        if (fromLabel != null) return fromLabel.replace('.', ',')
        if (value == null) return "–"
        val one = round1(value)
        val two = round2(value)
        return if (abs(two - one) < 0.001) {
            String.format(nl, "%.1f", one).replace('.', ',')
        } else {
            String.format(nl, "%.2f", two).replace('.', ',')
        }
    }

    fun formatWeight(weight: Double?): String? {
        if (weight == null || weight <= 0.0) return null
        val shown = if (weight == weight.toLong().toDouble()) {
            weight.toLong().toString()
        } else {
            weight.toString()
        }
        return "×$shown"
    }

    internal fun averageRank(grade: Grade): Int {
        val blob = listOfNotNull(grade.columnHeading, grade.columnDescription)
            .joinToString(" ")
            .lowercase()
        val heading = grade.columnHeading?.trim()?.lowercase().orEmpty()
        return when {
            heading == "gem" || blob.contains("voortschrijdend") -> 4
            blob.contains("gemiddelde") && !blob.contains("se") -> 4
            blob.contains("se") -> 3
            grade.columnType == GradeColumnType.Average -> 2
            grade.columnType == GradeColumnType.End -> 1
            else -> 0
        }
    }
}

data class ScoreNeeded(
    val needed: Double,
    val currentAverage: Double?,
    val currentWeight: Double,
    val nextWeight: Double,
    val target: Double,
    val reachable: Boolean,
    val alreadyReached: Boolean,
)
