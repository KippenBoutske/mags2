package nl.cita.magister.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nl.cita.magister.model.Absence
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.StudyGuide
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.subject.teacherCode
import nl.cita.magister.ui.nav.MagsPage

data class SearchHit(
    val page: MagsPage,
    val title: String,
    val subtitle: String,
    val messageId: Int? = null,
)

fun searchMags(
    query: String,
    grades: List<Grade>,
    appointments: List<Appointment>,
    homework: List<HomeworkItem>,
    messages: List<MessagePreview>,
    absences: List<Absence> = emptyList(),
    studyGuides: List<StudyGuide> = emptyList(),
): List<SearchHit> {
    val q = query.trim().lowercase()
    if (q.length < 2) return emptyList()
    val hits = mutableListOf<SearchHit>()
    grades.filter { matches(q, it.subjectName, it.subjectCode, it.value, it.columnHeading) }
        .distinctBy { it.subjectId }
        .take(6)
        .forEach {
            hits += SearchHit(MagsPage.Cijfers, it.displaySubject(), listOfNotNull(it.value).joinToString(" · "))
        }
    appointments.filter { matches(q, it.title, it.displaySubject(), it.location, it.classroom, it.teachers.joinToString(), it.content) }
        .take(6)
        .forEach {
            hits += SearchHit(MagsPage.Rooster, it.displaySubject(), listOfNotNull(it.classroom, it.teacherCode()).joinToString(" · "))
        }
    homework.filter { matches(q, it.subject, it.title, it.description, it.displaySubject()) }
        .take(6)
        .forEach {
            hits += SearchHit(MagsPage.Huiswerk, it.displaySubject(), it.title)
        }
    messages.filter { matches(q, it.subject, it.sender, it.snippet) }
        .take(6)
        .forEach {
            hits += SearchHit(MagsPage.Berichten, it.subject, it.sender, messageId = it.id)
        }
    absences.filter { matches(q, it.description, it.lesson, it.code) }
        .take(4)
        .forEach {
            hits += SearchHit(MagsPage.Meer, it.description, it.lesson ?: it.code.orEmpty())
        }
    studyGuides.filter { matches(q, it.title, it.subjectCodes.joinToString()) }
        .take(4)
        .forEach {
            hits += SearchHit(MagsPage.Meer, it.title, it.subjectCodes.joinToString(" · "))
        }
    return hits
}

private fun matches(q: String, vararg fields: String?): Boolean =
    fields.any { it?.lowercase()?.contains(q) == true }

@Composable
fun SearchSheet(
    query: String,
    onQuery: (String) -> Unit,
    grades: List<Grade>,
    appointments: List<Appointment>,
    homework: List<HomeworkItem>,
    messages: List<MessagePreview>,
    absences: List<Absence> = emptyList(),
    studyGuides: List<StudyGuide> = emptyList(),
    onDismiss: () -> Unit,
    onOpen: (SearchHit) -> Unit,
) {
    val hits = remember(query, grades, appointments, homework, messages, absences, studyGuides) {
        searchMags(query, grades, appointments, homework, messages, absences, studyGuides)
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Zoeken", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .semantics { contentDescription = "Zoek in Mags" },
                placeholder = { Text("Zoeken") },
                singleLine = true,
            )
            when {
                query.trim().length < 2 -> { }
                hits.isEmpty() -> Text("Niets gevonden.")
                else -> hits.forEach { hit ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onOpen(hit) }
                            .padding(vertical = 8.dp)
                            .semantics { contentDescription = hit.title },
                    ) {
                        Text(hit.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${hit.page.name} · ${hit.subtitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
