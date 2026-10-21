package nl.cita.magister.ui.grades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Grade
import nl.cita.magister.model.GradeColumnType
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.ui.formatGrade
import nl.cita.magister.ui.theme.CitaAction
import nl.cita.magister.ui.theme.CitaActionRow

private enum class GradeFilter { All, Weak, Pta }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GradesScreen(
    profile: StudentProfile?,
    averages: List<SubjectAverage>,
    grades: List<Grade>,
    refreshing: Boolean,
    exporting: Boolean,
    contentPadding: PaddingValues,
    onExport: () -> Unit,
    onShareJson: () -> Unit,
) {
    var filter by remember { mutableStateOf(GradeFilter.All) }
    var calculatorFor by remember { mutableStateOf<SubjectAverage?>(null) }
    val visible = when (filter) {
        GradeFilter.All -> averages
        GradeFilter.Weak -> averages.filter { (it.average ?: 10.0) < 6.0 }
        GradeFilter.Pta -> averages.filter { subject ->
            grades.any { it.subjectId == subject.subjectId && it.isPta }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                profile?.let { "${it.firstName} · ${it.className ?: it.study ?: "leerling"}" } ?: "Cijfers",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Tik een vak voor de calculator.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            CitaActionRow(
                actions = listOf(
                    CitaAction("Alle vakken") { filter = GradeFilter.All },
                    CitaAction("Aandacht") { filter = GradeFilter.Weak },
                    CitaAction("PTA") { filter = GradeFilter.Pta },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onExport, enabled = !exporting) {
                    Icon(Icons.Outlined.Send, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(if (exporting) "Versturen…" else "Cijfers exporteren")
                }
                OutlinedButton(onClick = onShareJson) {
                    Icon(Icons.Outlined.IosShare, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Deel JSON")
                }
            }
        }
        if (refreshing && averages.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Cijfers ophalen…")
                }
            }
        }
        if (!refreshing && visible.isEmpty()) {
            item {
                Text("Nog geen cijfers in de cache. Trek naar beneden om te vernieuwen.")
            }
        }
        items(visible, key = { it.subjectId }) { subject ->
            SubjectCard(
                subject = subject,
                grades = grades.filter { it.subjectId == subject.subjectId && it.columnType == GradeColumnType.Grade },
                onCalculate = { calculatorFor = subject },
            )
        }
    }

    calculatorFor?.let { subject ->
        CalculatorSheet(
            subject = subject,
            grades = grades.filter { it.subjectId == subject.subjectId },
            onDismiss = { calculatorFor = null },
        )
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectAverage,
    grades: List<Grade>,
    onCalculate: () -> Unit,
) {
    val avg = subject.average
    val color = when {
        avg == null -> MaterialTheme.colorScheme.surfaceContainerHigh
        avg < 5.5 -> MaterialTheme.colorScheme.errorContainer
        avg < 6.0 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(subject.subjectName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${subject.subjectCode.uppercase()} · ${subject.gradesCounted} cijfers",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(formatGrade(avg), style = MaterialTheme.typography.displaySmall)
            }
            if (avg != null) {
                LinearProgressIndicator(
                    progress = { (avg / 10f).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            grades.sortedByDescending { it.enteredAt }.take(4).forEach { grade ->
                Text(
                    "${grade.columnHeading ?: "Cijfer"}  ${grade.value ?: "–"}  ·  ×${grade.weight ?: 1}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = onCalculate) {
                Icon(Icons.Outlined.Calculate, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Wat moet ik halen?")
            }
        }
    }
}

@Composable
private fun CalculatorSheet(
    subject: SubjectAverage,
    grades: List<Grade>,
    onDismiss: () -> Unit,
) {
    var target by remember { mutableFloatStateOf(7f) }
    var weight by remember { mutableFloatStateOf(2f) }
    val result = remember(target, weight, grades) {
        runCatching { GradeCalculator.scoreNeeded(grades, target.toDouble(), weight.toDouble()) }.getOrNull()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Wat moet ik halen?", style = MaterialTheme.typography.headlineSmall)
            Text(subject.subjectName, style = MaterialTheme.typography.titleMedium)
            Text("Huidig gemiddelde: ${formatGrade(result?.currentAverage ?: subject.average)}")
            Text("Doel: ${formatGrade(target.toDouble())}")
            Slider(value = target, onValueChange = { target = it }, valueRange = 5.5f..9f, steps = 6)
            Text("Weging volgende toets: ${weight.toInt()}×")
            Slider(value = weight, onValueChange = { weight = it }, valueRange = 1f..6f, steps = 4)
            OutlinedTextField(
                value = formatGrade(target.toDouble()),
                onValueChange = { raw ->
                    GradeCalculator.parseDutchNumber(raw)?.toFloat()?.let { target = it }
                },
                label = { Text("Doelcijfer") },
                modifier = Modifier.fillMaxWidth(),
            )
            when {
                result == null -> Text("Nog te weinig cijfers om te rekenen.")
                result.alreadyReached -> Text("Je zit er al boven, ook met een 1,0.")
                !result.reachable -> Text("Niet haalbaar met deze weging — je zou een ${formatGrade(result.needed)} nodig hebben.")
                else -> Text(
                    "Haal een ${formatGrade(GradeCalculator.round1(result.needed))} of hoger.",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
