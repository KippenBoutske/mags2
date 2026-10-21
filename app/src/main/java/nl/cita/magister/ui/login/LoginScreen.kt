package nl.cita.magister.ui.login

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import nl.cita.magister.api.KnownSchools
import nl.cita.magister.api.Tenant

@Composable
fun LoginScreen(
    schoolName: String,
    tenantHost: String,
    onSchoolChange: (host: String, name: String) -> Unit,
    onStartLogin: () -> String,
    onWebViewLogin: () -> Unit,
    onDemo: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember(schoolName) { mutableStateOf(schoolName) }
    val matches = remember(query) { KnownSchools.search(query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Cita", style = MaterialTheme.typography.displaySmall)
        Text("Magister, maar sneller.", style = MaterialTheme.typography.titleMedium)
        Text(
            "Onofficiële client voor je eigen cijfers, rooster, huiswerk en berichten. " +
                "Citadel College gebruikt ${Tenant.CITADEL_HOST} — niet citadelcollege.magister.net " +
                "(die host bestaat niet).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSchoolChange(Tenant.normalize(it), it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("School") },
            supportingText = { Text(tenantHost) },
        )
        matches.take(5).forEach { school ->
            TextButton(onClick = {
                query = school.name
                onSchoolChange(school.host, school.name)
            }) {
                Text("${school.name} · ${school.host}")
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Inloggen", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Via accounts.magister.net (OAuth + PKCE, client M6LOAPP). " +
                        "Microsoft/Entra-SSO van school werkt het best in een Custom Tab.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = {
                        val url = onStartLogin()
                        try {
                            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                        } catch (_: ActivityNotFoundException) {
                            onWebViewLogin()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Inloggen") }
                FilledTonalButton(onClick = onWebViewLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("In de app")
                }
                OutlinedButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                    Text("Voorbeeld")
                }
            }
        }
        Text(
            "We slaan geen wachtwoord op. Tokens staan versleuteld in Android Keystore. " +
                "Alleen jouw eigen Magister-data wordt opgevraagd.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}
