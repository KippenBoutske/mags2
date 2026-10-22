package nl.cita.magister.ui.grades

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.haptics.hapticForGrade
import nl.cita.magister.haptics.hapticOnDown
import nl.cita.magister.model.Grade
import nl.cita.magister.ui.formatDayShort
import nl.cita.magister.ui.formatGrade
import nl.cita.magister.ui.localDateOf
import nl.cita.magister.ui.motion.PressScaleCard

internal fun latestGradeCaption(grade: Grade): String =
    listOfNotNull(
        grade.columnHeading?.takeIf { it.isNotBlank() } ?: grade.columnDescription?.takeIf { it.isNotBlank() },
        GradeCalculator.formatWeight(grade.weight),
        localDateOf(grade.enteredAt)?.let { formatDayShort(it) },
    ).joinToString(" · ")

@Composable
fun LatestGradesStrip(
    grades: List<Grade>,
    onOpen: (Grade) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (grades.isEmpty()) {
        Text(
            "Nog geen recente cijfers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.testTag("laatste-cijfers-leeg"),
        )
        return
    }
    PressScaleCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("laatste-cijfers"),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            grades.forEach { grade ->
                val fail = (grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value))
                    ?.let { it < 5.5 } == true
                Row(
                    Modifier
                        .fillMaxWidth()
                        .testTag("laatste-cijfer")
                        .hapticOnDown(hapticForGrade(grade.isSufficient))
                        .clickable { onOpen(grade) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        formatGrade(grade.numericValue, grade.value),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (fail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            grade.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            latestGradeCaption(grade),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
