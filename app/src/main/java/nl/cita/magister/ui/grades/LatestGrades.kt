package nl.cita.magister.ui.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
internal fun GradeValuePill(
    value: Double?,
    label: String?,
    modifier: Modifier = Modifier,
) {
    val numeric = value ?: GradeCalculator.parseDutchNumber(label)
    val container = when {
        numeric != null && numeric < 5.5 -> MaterialTheme.colorScheme.errorContainer
        numeric != null && numeric >= 7.0 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val content = when {
        numeric != null && numeric < 5.5 -> MaterialTheme.colorScheme.onErrorContainer
        numeric != null && numeric >= 7.0 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatGrade(value, label),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = content,
            maxLines = 1,
            softWrap = false,
        )
    }
}

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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("laatste-cijfers"),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            grades.forEachIndexed { index, grade ->
                val day = localDateOf(grade.enteredAt)?.let { formatDayShort(it) }
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .testTag("laatste-cijfer")
                        .hapticOnDown(hapticForGrade(grade.isSufficient, grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value)))
                        .clickable { onOpen(grade) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GradeValuePill(
                        value = grade.numericValue,
                        label = grade.value,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            grade.displaySubject(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val caption = latestGradeCaption(grade)
                        if (caption.isNotBlank()) {
                            Text(
                                caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (!day.isNullOrBlank()) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Text(
                                day,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
