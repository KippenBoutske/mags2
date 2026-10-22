package nl.cita.magister.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import nl.cita.magister.api.School
import nl.cita.magister.model.Absence
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.StudyGuide
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.formatWhen
import nl.cita.magister.ui.school.SchoolPicker
import nl.cita.magister.ui.theme.CitaAction
import nl.cita.magister.ui.theme.CitaActionRow

@Composable
fun SettingsScreen(
    profile: StudentProfile?,
    tenantHost: String,
    schoolName: String,
    webhookUrl: String,
    hasWebhookKey: Boolean,
    demoMode: Boolean,
    schools: List<School>,
    searching: Boolean,
    lastError: ApiFailure?,
    contentPadding: PaddingValues,
    onQuery: (String) -> Unit,
    onSwitchSchool: (host: String, name: String, tenantId: String?) -> Unit,
    onSaveWebhook: (url: String, key: String) -> Unit,
    onExport: () -> Unit,
    onShareJson: () -> Unit,
    onLogout: () -> Unit,
    dynamicColor: Boolean = false,
    onDynamicColor: (Boolean) -> Unit = {},
    notifications: Boolean = true,
    onNotifications: (Boolean) -> Unit = {},
    absences: List<Absence> = emptyList(),
    studyGuides: List<StudyGuide> = emptyList(),
    assignments: List<HomeworkItem> = emptyList(),
    absencesError: ApiFailure? = null,
    studyGuidesError: ApiFailure? = null,
    assignmentsError: ApiFailure? = null,
) {
    var url by remember(webhookUrl) { mutableStateOf(webhookUrl) }
    var key by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageTitle("Meer", profile?.let { "${it.firstName} ${it.lastName}" })
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    profile?.let { "${it.firstName} ${it.lastName}" } ?: "Niet ingelogd",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(listOfNotNull(profile?.className, profile?.study, schoolName).joinToString(" · "))
                if (tenantHost.isNotBlank()) {
                    Text(tenantHost, style = MaterialTheme.typography.labelMedium)
                }
                if (demoMode) Text("Voorbeeld — geen live data.")
            }
        }
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Andere school", style = MaterialTheme.typography.titleMedium)
                SchoolPicker(
                    selectedName = schoolName,
                    selectedHost = tenantHost,
                    hits = schools,
                    searching = searching,
                    onQuery = onQuery,
                    onPick = onSwitchSchool,
                )
            }
        }
        lastError?.let { SectionError(it) }
        ExtraCard("Afwezigheid", absencesError) {
            if (absences.isEmpty() && absencesError == null) {
                Text("Geen afwezigheid in de laatste maanden.")
            }
            absences.take(8).forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.description, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            item.lesson,
                            item.code,
                            if (item.excused) "geoorloofd" else "ongeoorloofd",
                            formatWhen(item.startIso),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        ExtraCard("Studiewijzers", studyGuidesError) {
            if (studyGuides.isEmpty() && studyGuidesError == null) {
                Text("Geen studiewijzers voor deze peildatum.")
            }
            studyGuides.forEach { guide ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(guide.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            if (guide.isProject) append("Project")
                            if (guide.subjectCodes.isNotEmpty()) {
                                if (isNotEmpty()) append(" · ")
                                append(guide.subjectCodes.joinToString())
                            }
                        }.ifBlank { "Studiewijzer" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        ExtraCard("Opdrachten", assignmentsError) {
            if (assignments.isEmpty() && assignmentsError == null) {
                Text("Geen open opdrachten.")
            }
            assignments.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(item.subject, formatWhen(item.dueIso).takeIf { it.isNotBlank() })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weergave", style = MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Material You")
                        Text(
                            "Uit = Donker warm. Aan = kleuren van je telefoon.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = dynamicColor, onCheckedChange = onDynamicColor)
                }
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Meldingen")
                        Text(
                            "Nieuwe cijfers en roosterwijzigingen. Widget ‘Volgende les’ op het startscherm.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = notifications, onCheckedChange = onNotifications)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Plan Goon", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Stuur cijfers naar je planningsassistent.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Webhook-URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(if (hasWebhookKey) "Nieuwe key (leeg = behouden)" else "Bearer-key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(onClick = { onSaveWebhook(url, key) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Opslaan")
                }
                CitaActionRow(
                    actions = listOf(
                        CitaAction("Exporteren", onClick = onExport),
                        CitaAction("Deel JSON", onClick = onShareJson),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Uitloggen")
        }
        Text(
            "Mags is geen officiële Magister-app. Alleen jouw eigen data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExtraCard(
    title: String,
    error: ApiFailure?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            SectionError(error)
            content()
        }
    }
}
