package nl.cita.magister.ui.school

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nl.cita.magister.api.School
import nl.cita.magister.api.Tenant
import nl.cita.magister.ui.components.SchoolSearchSkeleton

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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("school-search")
                .semantics { contentDescription = "Zoek je school" },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            ),
            placeholder = { Text("Zoek je school") },
            singleLine = true,
        )
        if (searching && hits.isEmpty()) {
            SchoolSearchSkeleton()
        }
        hits.forEach { school ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        query = school.name
                        onPick(school.host, school.name, school.tenantId)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("school-hit")
                    .semantics { contentDescription = "Kies ${school.name}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(school.name, style = MaterialTheme.typography.titleMedium)
                    if (school.city.isNotBlank()) {
                        Text(
                            school.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (!searching && query.trim().length >= 2 && hits.isEmpty()) {
            Text(
                "Geen school.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
