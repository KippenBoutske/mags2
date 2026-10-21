package nl.cita.magister.ui.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.cita.magister.api.School
import nl.cita.magister.api.Tenant

@Composable
fun SchoolPicker(
    selectedName: String,
    selectedHost: String,
    hits: List<School>,
    searching: Boolean,
    onQuery: (String) -> Unit,
    onPick: (host: String, name: String, tenantId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { onQuery(query) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { value ->
                query = value
                if (Tenant.looksLikeHost(value)) {
                    onPick(Tenant.normalize(value), selectedName.ifBlank { value.trim() }, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("School") },
            placeholder = { Text("Zoek je school") },
            supportingText = {
                val label = when {
                    selectedName.isNotBlank() && selectedHost.isNotBlank() -> "$selectedName · $selectedHost"
                    selectedName.isNotBlank() -> selectedName
                    selectedHost.isNotBlank() -> selectedHost
                    else -> "Kies een school, of typ school.magister.net"
                }
                Text(label)
            },
            singleLine = true,
        )
        if (searching) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        hits.forEach { school ->
            TextButton(
                onClick = {
                    query = school.name
                    onPick(school.host, school.name, school.tenantId)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    buildString {
                        append(school.name)
                        if (school.host.isNotBlank()) {
                            append(" · ")
                            append(school.host.removeSuffix(".magister.net"))
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (!searching && query.trim().length >= 2 && hits.isEmpty()) {
            Text(
                "Geen school gevonden. Typ de hostname, zoals school.magister.net.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
