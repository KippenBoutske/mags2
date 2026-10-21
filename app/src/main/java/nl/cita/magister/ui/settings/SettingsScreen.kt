package nl.cita.magister.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.ui.login.ErrorDetails
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
        Text("Meer", style = MaterialTheme.typography.headlineMedium)
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
        lastError?.let { ErrorDetails(it) }
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
