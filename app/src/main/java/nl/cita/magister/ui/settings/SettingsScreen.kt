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
import nl.cita.magister.model.StudentProfile
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
    contentPadding: PaddingValues,
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
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Meer", style = MaterialTheme.typography.headlineMedium)
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(profile?.let { "${it.firstName} ${it.lastName}" } ?: "Niet ingelogd", style = MaterialTheme.typography.titleLarge)
                Text(listOfNotNull(profile?.className, profile?.study, schoolName).joinToString(" · "))
                Text(tenantHost, style = MaterialTheme.typography.labelMedium)
                if (demoMode) Text("Voorbeeldmodus — geen live Magister-data.")
            }
        }
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Plan Goon", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Stuur je cijfers naar je planningsassistent via een Grok Bot-routine-webhook. " +
                        "De bearer-key wordt versleuteld opgeslagen en nooit gelogd.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Webhook-URL (HTTPS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(if (hasWebhookKey) "Nieuwe bearer-key (leeg = behouden)" else "Bearer-key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(onClick = { onSaveWebhook(url, key) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Webhook opslaan")
                }
                CitaActionRow(
                    actions = listOf(
                        CitaAction("Cijfers exporteren", onExport),
                        CitaAction("Kopieer / deel JSON", onShareJson),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Uitloggen")
        }
        Text(
            "Cita is geen officiële Magister-app. Alleen jouw eigen leerlingdata. " +
                "OAuth via M6LOAPP / m6loapp://oauth2redirect/ zoals bekende open-source clients.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
