package nl.cita.magister.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.cita.magister.desktop.ui.DesktopFormat
import nl.cita.magister.desktop.ui.MagsDesktopController
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Grade
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.subject.displaySubject
import java.time.LocalDate
import kotlin.math.roundToInt

private enum class GradeFilter(val label: String) {
    All("Alle vakken"),
    Attention("Aandacht"),
    Pta("PTA"),
}

@Composable
fun GradesView(controller: MagsDesktopController) {
    val profile by controller.profile.collectAsState()
    val allGrades by controller.grades.collectAsState()
    val prefs by controller.prefs.collectAsState()
    val clipboard = LocalClipboardManager.current

    var filter by remember { mutableStateOf(GradeFilter.All) }
    var currentYearOnly by remember { mutableStateOf(true) }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }

    val today = remember { LocalDate.now(DesktopFormat.zone) }
    val scopedGrades = remember(allGrades, currentYearOnly, today) {
        if (currentYearOnly) {
            val yearGrades = GradeCalculator.currentSchoolYearGrades(allGrades, today)
            yearGrades.ifEmpty { allGrades }
        } else {
            allGrades
        }
    }

    val averages = remember(scopedGrades, filter) {
        val raw = GradeCalculator.averagesBySubject(scopedGrades)
        when (filter) {
            GradeFilter.All -> raw
            GradeFilter.Attention -> raw.filter { (it.average ?: 10.0) < 6.0 }
            GradeFilter.Pta -> {
                val ptaSubjects = scopedGrades.filter { it.isPta }.map { it.subjectId }.toSet()
                raw.filter { it.subjectId in ptaSubjects }
            }
        }
    }

    val selectedAverage = remember(averages, selectedSubjectId) {
        averages.firstOrNull { it.subjectId == selectedSubjectId } ?: averages.firstOrNull()
    }

    val selectedGrades = remember(scopedGrades, selectedAverage) {
        if (selectedAverage == null) emptyList()
        else GradeCalculator.countedGrades(scopedGrades.filter { it.subjectId == selectedAverage.subjectId })
            .sortedByDescending { it.enteredAt.orEmpty() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Cijfers", style = MaterialTheme.typography.displayMedium)
                profile?.let { p ->
                    val line = DesktopFormat.profileLine(p.firstName, p.className, p.study)
                    if (line.isNotBlank()) {
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val json = controller.repo.exportJson(pretty = true)
                        clipboard.setText(AnnotatedString(json))
                        controller.showStatus("Cijfers-JSON gekopieerd naar klembord")
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Kopieer JSON")
                }
                if (prefs.webhookUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { controller.exportToPlanGoon() },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Outlined.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Stuur naar Plan Goon")
                    }
                }
            }
        }

        // Filter row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GradeFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                )
            }
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = currentYearOnly,
                onClick = { currentYearOnly = !currentYearOnly },
                label = { Text("Dit schooljaar") },
            )
        }

        // Master-detail split
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Left: Subject average cards
            if (averages.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Geen vakken gevonden voor dit filter.", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(averages, key = { it.subjectId }) { avg ->
                        val subjectCounted = remember(scopedGrades, avg.subjectId) {
                            GradeCalculator.countedGrades(scopedGrades.filter { it.subjectId == avg.subjectId })
                                .sortedByDescending { it.enteredAt.orEmpty() }
                                .take(2)
                        }
                        SubjectAverageCard(
                            avg = avg,
                            recentPreview = subjectCounted,
                            selected = selectedAverage?.subjectId == avg.subjectId,
                            onClick = { selectedSubjectId = avg.subjectId },
                        )
                    }
                }
            }

            // Right: Subject detail + "Wat moet ik halen?" calculator
            if (selectedAverage != null) {
                SubjectDetailPane(
                    subject = selectedAverage,
                    grades = selectedGrades,
                    modifier = Modifier.weight(1.1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun SubjectAverageCard(
    avg: SubjectAverage,
    recentPreview: List<Grade>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val value = avg.average
    val isFail = value != null && value < 5.5

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = avg.displaySubject(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = avg.averageLabel ?: DesktopFormat.formatGrade(value),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isFail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }

            GradeBar(value = value)

            if (recentPreview.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    recentPreview.forEach { g ->
                        val heading = g.columnHeading?.takeIf { it.isNotBlank() } ?: g.columnDescription.orEmpty()
                        val w = GradeCalculator.formatWeight(g.weight)
                        Text(
                            text = listOfNotNull(
                                heading.takeIf { it.isNotBlank() },
                                DesktopFormat.formatGrade(g.numericValue, g.value),
                                w,
                            ).joinToString(" · "),
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

@Composable
private fun GradeBar(value: Double?) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val fill = if (value != null && value < 5.5) error else primary
    val fraction = ((value ?: 0.0) / 10.0).coerceIn(0.0, 1.0).toFloat()

    Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
        val barH = 6.dp.toPx()
        val y = (size.height - barH) / 2f
        val r = CornerRadius(barH / 2f, barH / 2f)
        drawRoundRect(color = track, topLeft = Offset(0f, y), size = Size(size.width, barH), cornerRadius = r)
        if (fraction > 0f) {
            drawRoundRect(
                color = fill,
                topLeft = Offset(0f, y),
                size = Size(size.width * fraction, barH),
                cornerRadius = r,
            )
        }
        // 5.5 pass mark tick
        val passX = size.width * 0.55f
        drawLine(
            color = primary.copy(alpha = 0.7f),
            start = Offset(passX, 0f),
            end = Offset(passX, size.height),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

@Composable
private fun SubjectDetailPane(
    subject: SubjectAverage,
    grades: List<Grade>,
    modifier: Modifier = Modifier,
) {
    var target by remember(subject.subjectId) { mutableStateOf(6.0f) }
    var nextWeight by remember(subject.subjectId) { mutableStateOf(2f) }

    val calc = remember(grades, target, nextWeight) {
        GradeCalculator.scoreNeeded(
            currentGrades = grades,
            target = (target * 10f).roundToInt() / 10.0,
            nextWeight = nextWeight.roundToInt().toDouble().coerceAtLeast(1.0),
        )
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.displaySubject(), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${subject.gradesCounted} cijfers · totale weging ${GradeCalculator.formatAverage(subject.weightSum)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = subject.averageLabel ?: DesktopFormat.formatGrade(subject.average),
                    style = MaterialTheme.typography.displayMedium,
                    color = if ((subject.average ?: 10.0) < 5.5) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            // "Wat moet ik halen?" calculator card
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text("Wat moet ik halen?", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val neededText = when {
                            calc.alreadyReached -> "Al gehaald"
                            !calc.reachable -> "Niet haalbaar (${GradeCalculator.formatAverage(calc.needed)})"
                            else -> GradeCalculator.formatAverage(calc.needed)
                        }
                        Text(
                            text = "Nodig op volgende toets (×${nextWeight.roundToInt()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = neededText,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (!calc.reachable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Doelcijfer", style = MaterialTheme.typography.labelMedium)
                            Text(
                                GradeCalculator.formatAverage((target * 10f).roundToInt() / 10.0),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Slider(
                            value = target,
                            onValueChange = { target = it },
                            valueRange = 5.5f..9.5f,
                            steps = 39,
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Weging volgende toets", style = MaterialTheme.typography.labelMedium)
                            Text("×${nextWeight.roundToInt()}", style = MaterialTheme.typography.labelLarge)
                        }
                        Slider(
                            value = nextWeight,
                            onValueChange = { nextWeight = it },
                            valueRange = 1f..6f,
                            steps = 4,
                        )
                    }
                }
            }

            // All grades list
            Text("Alle behaalde cijfers", style = MaterialTheme.typography.titleMedium)
            if (grades.isEmpty()) {
                Text(
                    "Geen losse cijfers gevonden voor dit vak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    grades.forEachIndexed { idx, g ->
                        if (idx > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }
                        val num = GradeCalculator.numeric(g)
                        val fail = num != null && num < 5.5
                        val day = DesktopFormat.localDateOf(g.enteredAt)?.let { DesktopFormat.formatDayShort(it) }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                text = DesktopFormat.formatGrade(g.numericValue, g.value),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (fail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = g.columnDescription?.takeIf { it.isNotBlank() }
                                        ?: g.columnHeading?.takeIf { it.isNotBlank() }
                                        ?: "Cijfer",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val meta = listOfNotNull(
                                    g.columnHeading?.takeIf { it.isNotBlank() },
                                    GradeCalculator.formatWeight(g.weight),
                                    if (g.isPta) "PTA" else null,
                                    g.periodName?.takeIf { it.isNotBlank() },
                                ).joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
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
        }
    }
}
