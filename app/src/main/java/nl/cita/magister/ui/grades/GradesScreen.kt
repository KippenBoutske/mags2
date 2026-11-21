package nl.cita.magister.ui.grades

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
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
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.hapticForGrade
import nl.cita.magister.haptics.hapticOnDown
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Grade
import nl.cita.magister.model.GradeColumnType
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.ui.components.EmptyState
import nl.cita.magister.ui.components.ExpressiveScallopShape
import nl.cita.magister.ui.components.ListSkeleton
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.formatDayShort
import nl.cita.magister.ui.formatGrade
import nl.cita.magister.ui.formatWhen
import nl.cita.magister.ui.localDateOf
import nl.cita.magister.ui.motion.GradeConfettiBurst
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.expressiveSheen
import nl.cita.magister.ui.motion.magsFloatSpring
import nl.cita.magister.ui.motion.magsSharedBounds
import nl.cita.magister.ui.motion.magsSpring
import nl.cita.magister.ui.motion.pageFadeSpring
import nl.cita.magister.ui.motion.staggerEnter
import nl.cita.magister.ui.nav.MagsChrome
import nl.cita.magister.ui.profileLine
import nl.cita.magister.ui.theme.CitaAction
import nl.cita.magister.ui.theme.CitaActionRow
import nl.cita.magister.ui.theme.GradeSage
import java.time.LocalDate

private enum class GradeFilter { All, Weak, Pta }

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GradesScreen(
    profile: StudentProfile?,
    averages: List<SubjectAverage>,
    grades: List<Grade>,
    latestGrades: List<Grade> = emptyList(),
    refreshing: Boolean,
    exporting: Boolean,
    contentPadding: PaddingValues,
    onExport: () -> Unit,
    onShareJson: () -> Unit = {},
    gradesError: ApiFailure? = null,
    initialSubjectId: Int? = null,
) {
    val today = LocalDate.now()
    val currentYearGrades = GradeCalculator.currentSchoolYearGrades(grades, today)
    val currentYearAverages = GradeCalculator.averagesBySubject(currentYearGrades)
    val allCurrentYearLatest = GradeCalculator.latestEarnedGrades(
        GradeCalculator.currentSchoolYearGrades(latestGrades + grades, today),
        limit = Int.MAX_VALUE,
    )
    var showAllLatest by remember { mutableStateOf(false) }
    val currentYearLatest = if (showAllLatest) allCurrentYearLatest else allCurrentYearLatest.take(5)
    var filter by remember { mutableStateOf(GradeFilter.All) }
    var openSubject by remember {
        mutableStateOf(currentYearAverages.firstOrNull { it.subjectId == initialSubjectId })
    }
    var openGrade by remember { mutableStateOf<Grade?>(null) }
    val visible = when (filter) {
        GradeFilter.All -> currentYearAverages
        GradeFilter.Weak -> currentYearAverages.filter { (it.average ?: 10.0) < 6.0 }
        GradeFilter.Pta -> currentYearAverages.filter { subject ->
            currentYearGrades.any { it.subjectId == subject.subjectId && it.isPta }
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
                Crossfade(
                    targetState = refreshing && currentYearAverages.isEmpty(),
                    animationSpec = pageFadeSpring(),
                    label = "cijfers-load",
                    modifier = Modifier.fillMaxSize(),
                ) { skeleton ->
                if (skeleton) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = MagsChrome.PageGutter,
                                end = MagsChrome.PageGutter,
                                top = contentPadding.calculateTopPadding() + 8.dp,
                                bottom = contentPadding.calculateBottomPadding() + MagsChrome.ListBottomGap,
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PageTitle(
                            "Cijfers",
                            profile?.let { profileLine(it.firstName, it.className, it.study) },
                        )
                        ListSkeleton(rows = 3, contentDescription = "Cijfers laden")
                    }
                } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("cijfers-lijst"),
                    contentPadding = PaddingValues(
                        start = MagsChrome.PageGutter,
                        end = MagsChrome.PageGutter,
                        top = contentPadding.calculateTopPadding() + 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + MagsChrome.ListBottomGap,
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
                                CitaAction("Alle vakken", selected = filter == GradeFilter.All) { filter = GradeFilter.All },
                                CitaAction("Aandacht", selected = filter == GradeFilter.Weak) { filter = GradeFilter.Weak },
                                CitaAction("PTA", selected = filter == GradeFilter.Pta) { filter = GradeFilter.Pta },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (filter == GradeFilter.All) {
                        val validAverages = currentYearAverages.mapNotNull { it.average }
                        if (validAverages.isNotEmpty()) {
                            item {
                                GradesOverviewHero(
                                    overallAverage = validAverages.average(),
                                    passingCount = validAverages.count { it >= 5.5 },
                                    weakCount = validAverages.count { it < 5.5 },
                                    totalSubjects = validAverages.size,
                                )
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Laatste cijfers", style = MaterialTheme.typography.titleLarge)
                                if (allCurrentYearLatest.size > 5) {
                                    TextButton(
                                        onClick = { showAllLatest = !showAllLatest },
                                        modifier = Modifier.testTag("laatste-cijfers-toggle"),
                                    ) {
                                        Text(
                                            if (showAllLatest) "Minder" else "Alle ${allCurrentYearLatest.size}",
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            LatestGradesStrip(
                                grades = currentYearLatest,
                                onOpen = { openGrade = it },
                            )
                        }
                        item {
                            Text("Per vak", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    if (!refreshing && visible.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Nog geen cijfers",
                                tag = "cijfers-leeg",
                            )
                        }
                    }
                    itemsIndexed(visible, key = { _, it -> it.subjectId }) { index, item ->
                        SubjectCard(
                            subject = item,
                            grades = currentYearGrades.filter { it.subjectId == item.subjectId && it.columnType == GradeColumnType.Grade },
                            onOpen = { openSubject = item },
                            modifier = Modifier
                                .staggerEnter(index, key = item.subjectId)
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
                }
                }
            } else {
                SubjectPage(
                    subject = subject,
                    grades = currentYearGrades.filter { it.subjectId == subject.subjectId },
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
private fun GradesOverviewHero(
    overallAverage: Double,
    passingCount: Int,
    weakCount: Int,
    totalSubjects: Int,
) {
    val animatedAvg = remember { Animatable(0f) }
    LaunchedEffect(overallAverage) {
        animatedAvg.animateTo(
            targetValue = overallAverage.toFloat(),
            animationSpec = magsFloatSpring(dampingRatio = 0.72f, stiffness = 200f),
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveSheen(alpha = 0.12f)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(ExpressiveScallopShape(lobes = 10, depth = 0.09f))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatGrade(animatedAvg.value.toDouble()),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "SCHOOLJAAR GEMIDDELDE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    "$passingCount van $totalSubjects voldoende",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (weakCount > 0) {
                    Text(
                        "$weakCount vak${if (weakCount == 1) "" else "ken"} onder 5,5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
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
    val badgeBg = when {
        fail -> MaterialTheme.colorScheme.errorContainer
        avg != null && avg >= 7.0 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeFg = when {
        fail -> MaterialTheme.colorScheme.onErrorContainer
        avg != null && avg >= 7.0 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val recent = remember(grades) { GradeCalculator.latestEarnedGrades(grades, limit = 4) }
    val countLabel = when (subject.gradesCounted) {
        0 -> null
        1 -> "1 cijfer"
        else -> "${subject.gradesCounted} cijfers"
    }
    PressScaleCard(
        modifier = modifier.fillMaxWidth().testTag("cijfers-vak"),
        cornerRadius = 30.dp,
        pressedCornerRadius = 18.dp,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = onOpen,
        haptic = HapticEvent.GradeOpen,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        subject.displaySubject(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (countLabel != null) {
                        Text(
                            countLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeBg)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (avg == null) "–" else formatGrade(animated.toDouble(), subject.averageLabel),
                        style = MaterialTheme.typography.headlineMedium,
                        color = badgeFg,
                        modifier = Modifier.testTag("cijfers-gemiddelde"),
                    )
                }
            }
            if (avg != null) {
                AverageBar(average = avg.toFloat(), modifier = Modifier.fillMaxWidth().testTag("cijfers-balk"))
            }
            if (recent.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    recent.forEach { grade ->
                        val gradeFail = isFailing(grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value))
                        val weightBit = GradeCalculator.formatWeight(grade.weight)?.let { " · $it" }.orEmpty()
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                grade.columnHeading ?: grade.columnDescription ?: "Cijfer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            Text(
                                "${formatGrade(grade.numericValue, grade.value)}$weightBit",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (gradeFail) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
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

@Composable
internal fun AverageBar(average: Float, modifier: Modifier = Modifier) {
    val fillProgress = remember { Animatable(0f) }
    LaunchedEffect(average) {
        fillProgress.animateTo(
            targetValue = (average / 10f).coerceIn(0f, 1f),
            animationSpec = magsFloatSpring(dampingRatio = 0.68f, stiffness = 220f),
        )
    }
    val fill = when {
        average < 5.5f -> MaterialTheme.colorScheme.error
        average >= 7.0f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val marker = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
    Canvas(modifier.height(14.dp)) {
        val h = size.height
        val radius = CornerRadius(h / 2f, h / 2f)
        drawRoundRect(track, size = size, cornerRadius = radius)
        val width = size.width * fillProgress.value.coerceIn(0f, 1f)
        if (width > 0f) {
            drawRoundRect(fill, size = Size(width, h), cornerRadius = radius)
        }
        val x = size.width * 0.55f
        drawLine(marker, Offset(x, -2f), Offset(x, h + 2f), strokeWidth = 3f, cap = StrokeCap.Round)
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
    val allEarned = remember(grades) {
        GradeCalculator.latestEarnedGrades(grades, limit = Int.MAX_VALUE)
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
            bottom = contentPadding.calculateBottomPadding() + MagsChrome.ListBottomGap,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("cijfers-terug")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Terug")
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(subject.displaySubject(), style = MaterialTheme.typography.displayMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                    color = if (trend > 0) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.error,
                )
            }
        }
        itemsIndexed(allEarned, key = { _, it -> it.id }) { index, grade ->
            val meta = listOfNotNull(
                GradeCalculator.formatWeight(grade.weight),
                localDateOf(grade.enteredAt)?.let { formatDayShort(it) },
                if (!grade.counts) "telt niet mee" else null,
            ).joinToString(" · ")
            PressScaleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerEnter(index, key = grade.id)
                    .testTag("cijfers-cijfer"),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                onClick = { onGrade(grade) },
                haptic = hapticForGrade(grade.isSufficient, grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value)),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            grade.columnHeading ?: "Cijfer",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        grade.columnDescription?.takeIf { it.isNotBlank() && it != grade.columnHeading }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (meta.isNotBlank()) {
                            Text(
                                meta,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    GradeValuePill(
                        value = grade.numericValue,
                        label = grade.value,
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
    val numeric = grade.numericValue ?: GradeCalculator.parseDutchNumber(grade.value)
    val celebrate = numeric != null && numeric >= 7.5
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("cijfer-blad"),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(grade.displaySubject(), style = MaterialTheme.typography.headlineSmall)
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
            if (celebrate) {
                GradeConfettiBurst(
                    triggerKey = grade.id,
                    modifier = Modifier.matchParentSize(),
                )
            }
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
                modifier = Modifier
                    .hapticOnDown(HapticEvent.CalculatorStep, enabled = target > 4f)
                    .testTag("cijfers-doel-min"),
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = "Doel lager")
            }
            Text(
                "Doel ${formatGrade(target.toDouble())}",
                style = MaterialTheme.typography.titleLarge,
            )
            FilledTonalIconButton(
                onClick = { target = (target + 0.1f).coerceIn(4f, 10f) },
                modifier = Modifier
                    .hapticOnDown(HapticEvent.CalculatorStep, enabled = target < 10f)
                    .testTag("cijfers-doel-plus"),
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
                    modifier = Modifier
                        .hapticOnDown(HapticEvent.CalculatorStep, enabled = weight.toInt() != choice)
                        .testTag("cijfers-weging-$choice"),
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
