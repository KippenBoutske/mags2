package nl.cita.magister.grades

import nl.cita.magister.model.Grade
import nl.cita.magister.model.GradeColumnType
import nl.cita.magister.model.SubjectAverage
import kotlin.math.round

object GradeCalculator {
    fun parseDutchNumber(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
            .replace(" ", "")
            .replace(',', '.')
        return cleaned.toDoubleOrNull()?.takeIf { it in 1.0..10.0 }
    }

    fun weightedAverage(grades: List<Grade>): Double? {
        val usable = grades.filter { it.counts && it.columnType == GradeColumnType.Grade }
            .mapNotNull { grade ->
                val value = grade.numericValue ?: parseDutchNumber(grade.value) ?: return@mapNotNull null
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
        val usable = currentGrades.filter { it.counts && it.columnType == GradeColumnType.Grade }
            .mapNotNull { grade ->
                val value = grade.numericValue ?: parseDutchNumber(grade.value) ?: return@mapNotNull null
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

    fun averagesBySubject(grades: List<Grade>): List<SubjectAverage> {
        return grades.groupBy { it.subjectId }.map { (subjectId, items) ->
            val first = items.first()
            val magisterAvg = items
                .filter { it.columnType == GradeColumnType.Average || it.columnType == GradeColumnType.End }
                .mapNotNull { it.numericValue ?: parseDutchNumber(it.value) }
                .lastOrNull()
            val counted = items.filter { it.counts && it.columnType == GradeColumnType.Grade }
            SubjectAverage(
                subjectId = subjectId,
                subjectName = first.subjectName,
                subjectCode = first.subjectCode,
                average = weightedAverage(items),
                magisterAverage = magisterAvg,
                gradesCounted = counted.size,
                weightSum = counted.sumOf { it.weight ?: 1.0 },
                latest = counted.maxByOrNull { it.enteredAt.orEmpty() },
            )
        }.sortedBy { it.subjectName.lowercase() }
    }

    fun round1(value: Double): Double = round(value * 10.0) / 10.0
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
