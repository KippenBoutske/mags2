package nl.cita.magister.ui.grades

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Grade
import nl.cita.magister.model.GradeColumnType
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.ui.components.ListSkeleton
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.formatGrade
import nl.cita.magister.ui.formatWhen
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.magsFloatSpring
import nl.cita.magister.ui.motion.magsSharedBounds
import nl.cita.magister.ui.motion.magsSpring
import nl.cita.magister.ui.motion.staggerEnter
import nl.cita.magister.ui.nav.MagsChrome
import nl.cita.magister.ui.profileLine
import nl.cita.magister.ui.theme.CitaAction
import nl.cita.magister.ui.theme.CitaActionRow
import nl.cita.magister.ui.theme.GradeSage

private enum class GradeFilter { All, Weak, Pta }

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GradesScreen(
    profile: StudentProfile?,
    averages: List<SubjectAverage>,
    grades: List<Grade>,
    refreshing: Boolean,
    exporting: Boolean,
    contentPadding: PaddingValues,
    onExport: () -> Unit,
    onShareJson: () -> Unit = {},
    gradesError: ApiFailure? = null,
    initialSubjectId: Int? = null,
) {
    var filter by remember { mutableStateOf(GradeFilter.All) }
    var openSubject by remember {
        mutableStateOf(averages.firstOrNull { it.subjectId == initialSubjectId })
    }
    var openGrade by remember { mutableStateOf<Grade?>(null) }
    val visible = when (filter) {
        GradeFilter.All -> averages
        GradeFilter.Weak -> averages.filter { (it.average ?: 10.0) < 6.0 }
        GradeFilter.Pta -> averages.filter { subject ->
            grades.any { it.subjectId == subject.subjectId && it.isPta }
        }
    }

    BackHandler(openSubject != null) { openSubject = null }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = openSubject,
            transitionSpec = { fadeIn(magsSpring()) togetherWith fadeOut() },
            label = "cijfers-vak",
        ) { subject ->
            if (subject == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("cijfers-lijst"),
                    contentPadding = PaddingValues(
                        start = MagsChrome.PageGutter,
                        end = MagsChrome.PageGutter,
                        top = contentPadding.calculateTopPadding() + 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        PageTitle(
                            "Cijfers",
                            profile?.let { profileLine(it.firstName, it.className, it.study) },
                        )
                    }
                    gradesError?.let { item { SectionError(it) } }
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
                    if (refreshing && averages.isEmpty()) {
                        item { ListSkeleton(rows = 3, contentDescription = "Cijfers laden") }
                    }
                    if (!refreshing && visible.isEmpty()) {
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp)
                                    .testTag("cijfers-leeg"),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text("Nog geen cijfers.", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "Zodra Magister iets binnen heeft, staat het hier.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    itemsIndexed(visible, key = { _, it -> it.subjectId }) { index, item ->
                        SubjectCard(
                            subject = item,
                            grades = grades.filter { it.subjectId == item.subjectId && it.columnType == GradeColumnType.Grade },
                            onOpen = { openSubject = item },
                            modifier = Modifier
                                .staggerEnter(index)
                                .animateItem()
                                .then(
                                    magsSharedBounds(
                                        key = "vak-${item.subjectId}",
                                        scope = this@SharedTransitionLayout,
                                        visibility = this@AnimatedContent,
                                    ),
                                ),
                        )
                    }
                }
            } else {
                SubjectPage(
                    subject = subject,
                    grades = grades.filter { it.subjectId == subject.subjectId },
                    contentPadding = contentPadding,
                    onBack = { openSubject = null },
                    onGrade = { openGrade = it },
                    sharedModifier = magsSharedBounds(
                        key = "vak-${subject.subjectId}",
                        scope = this@SharedTransitionLayout,
                        visibility = this@AnimatedContent,
                    ),
                )
            }
        }
    }

    openGrade?.let { grade ->
        GradeDetailSheet(grade = grade, onDismiss = { openGrade = null })
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectAverage,
    grades: List<Grade>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avg = subject.average
    val animated by animateFloatAsState(
        targetValue = (avg ?: 0.0).toFloat(),
        animationSpec = magsFloatSpring(stiffness = 220f),
        label = "grade",
    )
    val fail = isFailing(avg)
    val numberColor = if (fail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    PressScaleCard(
        modifier = modifier.fillMaxWidth().testTag("cijfers-vak"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = onOpen,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subject.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (avg == null) "–" else formatGrade(animated.toDouble(), subject.averageLabel),
                    style = MaterialTheme.typography.displaySmall,
                    color = numberColor,
                    modifier = Modifier.testTag("cijfers-gemiddelde"),
                )
            }
            if (avg != null) {
                AverageBar(average = avg.toFloat(), modifier = Modifier.fillMaxWidth().testTag("cijfers-balk"))
            }
            GradeCalculator.countedGrades(grades).sortedByDescending { it.enteredAt }.take(4).forEach { grade ->
                val gradeFail = isFailing(grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value))
                val weightBit = GradeCalculator.formatWeight(grade.weight)?.let { "  ·  $it" }.orEmpty()
                Text(
                    "${grade.columnHeading ?: "Cijfer"}  ${grade.value ?: "–"}$weightBit",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (gradeFail) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun AverageBar(average: Float, modifier: Modifier = Modifier) {
    val fill = if (average < 5.5f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val marker = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    Canvas(modifier.height(10.dp)) {
        val h = size.height
        val radius = CornerRadius(h / 2f, h / 2f)
        drawRoundRect(track, size = size, cornerRadius = radius)
        val width = size.width * (average / 10f).coerceIn(0f, 1f)
        if (width > 0f) {
            drawRoundRect(fill, size = Size(width, h), cornerRadius = radius)
        }
        val x = size.width * 0.55f
        drawLine(marker, Offset(x, -2f), Offset(x, h + 2f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    }
}

@Composable
private fun SubjectPage(
    subject: SubjectAverage,
    grades: List<Grade>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onGrade: (Grade) -> Unit,
    sharedModifier: Modifier,
) {
    val counted = remember(grades) {
        GradeCalculator.countedGrades(grades).sortedBy { it.enteredAt.orEmpty() }
    }
    val values = counted.mapNotNull { it.numericValue ?: GradeCalculator.parseDutchNumber(it.value) }
    val trend = averageTrend(values)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(sharedModifier)
            .testTag("cijfers-vak-pagina"),
        contentPadding = PaddingValues(
            start = MagsChrome.PageGutter,
            end = MagsChrome.PageGutter,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("cijfers-terug")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Terug")
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(subject.subjectName, style = MaterialTheme.typography.displayMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        formatGrade(subject.average, subject.averageLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFailing(subject.average)) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (values.size >= 2) {
            item {
                GradeSparkline(
                    values = values,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .testTag("cijfers-grafiek"),
                )
            }
        }
        if (trend != 0) {
            item {
                Text(
                    if (trend > 0) "stijgt" else "daalt",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (trend > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }
        }
        itemsIndexed(counted.reversed(), key = { _, it -> it.id }) { index, grade ->
            PressScaleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerEnter(index)
                    .testTag("cijfers-cijfer"),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                onClick = { onGrade(grade) },
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(grade.columnHeading ?: "Cijfer", style = MaterialTheme.typography.titleMedium)
                        grade.columnDescription?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val gradeFail = isFailing(grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value))
                    Text(
                        grade.value ?: "–",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (gradeFail) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        item {
            CalculatorBlock(subject = subject, grades = grades)
        }
    }
}

@Composable
private fun GradeSparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, magsFloatSpring(dampingRatio = 0.82f, stiffness = 180f))
    }
    val line = MaterialTheme.colorScheme.primary
    val trend = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val min = (values.min() - 0.4).coerceAtLeast(1.0)
        val max = (values.max() + 0.4).coerceAtMost(10.0)
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        fun yOf(value: Double) = size.height - ((value - min) / range * size.height).toFloat()
        fun xOf(index: Int): Float {
            if (values.size == 1) return size.width / 2f
            return index / (values.lastIndex).toFloat() * size.width
        }
        drawLine(grid, Offset(0f, yOf(6.0)), Offset(size.width, yOf(6.0)), strokeWidth = 2f)
        val shown = (1 + (values.size - 1) * progress.value).toInt().coerceIn(1, values.size)
        val running = mutableListOf<Double>()
        val avgPath = Path()
        val gradePath = Path()
        values.take(shown).forEachIndexed { index, value ->
            running += value
            val x = xOf(index)
            val y = yOf(value)
            val ay = yOf(running.average())
            if (index == 0) {
                gradePath.moveTo(x, y)
                avgPath.moveTo(x, ay)
            } else {
                gradePath.lineTo(x, y)
                avgPath.lineTo(x, ay)
            }
            drawCircle(line, radius = 6f, center = Offset(x, y))
        }
        drawPath(avgPath, trend, style = Stroke(width = 4f, cap = StrokeCap.Round))
        drawPath(gradePath, line, style = Stroke(width = 7f, cap = StrokeCap.Round))
    }
}

internal fun averageTrend(values: List<Double>): Int {
    if (values.size < 2) return 0
    val last = values.last()
    val previous = values.dropLast(1).average()
    return when {
        last > previous + 0.15 -> 1
        last < previous - 0.15 -> -1
        else -> 0
    }
}

@Composable
private fun GradeDetailSheet(
    grade: Grade,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("cijfer-blad"),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(grade.subjectName, style = MaterialTheme.typography.headlineSmall)
            Text(grade.value ?: "–", style = MaterialTheme.typography.displaySmall)
            GradeCalculator.formatWeight(grade.weight)?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
            formatWhen(grade.enteredAt).takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            grade.columnHeading?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            grade.columnDescription?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            grade.teacher?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CalculatorBlock(
    subject: SubjectAverage,
    grades: List<Grade>,
) {
    var target by remember { mutableFloatStateOf(7f) }
    val columnWeights = remember(grades) {
        grades.mapNotNull { it.weight?.toInt()?.takeIf { weight -> weight in 1..6 } }
            .distinct()
            .sorted()
    }
    val weightChoices = remember(columnWeights) {
        (listOf(1, 2, 3) + columnWeights).distinct().sorted()
    }
    var weight by remember { mutableFloatStateOf((columnWeights.firstOrNull() ?: 2).toFloat()) }
    val result = remember(target, weight, grades) {
        runCatching { GradeCalculator.scoreNeeded(grades, target.toDouble(), weight.toDouble()) }.getOrNull()
    }
    Column(
        Modifier
            .fillMaxWidth()
            .testTag("cijfers-reken"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Wat moet ik halen?", style = MaterialTheme.typography.headlineSmall)
        Text("Nu ${formatGrade(result?.currentAverage ?: subject.average)}", style = MaterialTheme.typography.bodyMedium)
        Row(
            Modifier.fillMaxWidth().testTag("cijfers-doel"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledTonalIconButton(
                onClick = { target = (target - 0.1f).coerceIn(4f, 10f) },
                modifier = Modifier.testTag("cijfers-doel-min"),
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = "Doel lager")
            }
            Text(
                "Doel ${formatGrade(target.toDouble())}",
                style = MaterialTheme.typography.titleLarge,
            )
            FilledTonalIconButton(
                onClick = { target = (target + 0.1f).coerceIn(4f, 10f) },
                modifier = Modifier.testTag("cijfers-doel-plus"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Doel hoger")
            }
        }
        Text("Weging ×${weight.toInt()}", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("cijfers-weging"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            weightChoices.forEach { choice ->
                FilterChip(
                    selected = weight.toInt() == choice,
                    onClick = { weight = choice.toFloat() },
                    label = { Text("${choice}×") },
                    modifier = Modifier.testTag("cijfers-weging-$choice"),
                )
            }
        }
        val needed = result?.let { GradeCalculator.round1(it.needed) }
        val tone = when {
            result == null -> MaterialTheme.colorScheme.onSurface
            result.alreadyReached -> GradeSage
            !result.reachable -> MaterialTheme.colorScheme.error
            (needed ?: 10.0) <= 6.5 -> GradeSage
            (needed ?: 10.0) <= 8.5 -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.error
        }
        when {
            result == null -> Text("Te weinig cijfers.")
            result.alreadyReached -> {
                Text("–", style = MaterialTheme.typography.displayMedium, color = tone, modifier = Modifier.testTag("cijfers-nodig"))
                Text("al gehaald", style = MaterialTheme.typography.bodyMedium, color = tone)
            }
            else -> {
                Text(
                    formatGrade(needed),
                    style = MaterialTheme.typography.displayMedium,
                    color = tone,
                    modifier = Modifier.testTag("cijfers-nodig"),
                )
                Text(
                    if (result.reachable) "nodig voor je volgende" else "niet haalbaar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tone,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun isFailing(value: Double?): Boolean = value != null && value < 5.5
