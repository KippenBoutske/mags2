package nl.cita.magister.ui.grades

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.ui.formatDayShort
import nl.cita.magister.ui.formatGrade
import nl.cita.magister.ui.localDateOf

internal fun latestGradeCaption(grade: Grade): String =
    listOfNotNull(
        grade.columnHeading?.takeIf { it.isNotBlank() } ?: grade.columnDescription?.takeIf { it.isNotBlank() },
        GradeCalculator.formatWeight(grade.weight),
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("laatste-cijfers"),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        grades.forEachIndexed { index, grade ->
            val fail = (grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value))
                ?.let { it < 5.5 } == true
            val day = localDateOf(grade.enteredAt)?.let { formatDayShort(it) }
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .testTag("laatste-cijfer")
                    .hapticOnDown(hapticForGrade(grade.isSufficient))
                    .clickable { onOpen(grade) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    formatGrade(grade.numericValue, grade.value),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (fail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        grade.displaySubject(),
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
                if (!day.isNullOrBlank()) {
                    Text(
                        day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
