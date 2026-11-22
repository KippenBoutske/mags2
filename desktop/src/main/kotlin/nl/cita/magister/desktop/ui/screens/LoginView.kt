package nl.cita.magister.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.cita.magister.api.School
import nl.cita.magister.api.Tenant
import nl.cita.magister.desktop.data.DesktopPaths
import nl.cita.magister.desktop.ui.MagsDesktopController

@Composable
fun LoginView(controller: MagsDesktopController) {
    val schoolResults by controller.schoolResults.collectAsState()
    val searching by controller.searchingSchools.collectAsState()
    val activeAuth by controller.activeAuth.collectAsState()
    val loginError by controller.loginError.collectAsState()
    val refreshing by controller.refreshing.collectAsState()
    val clipboard = LocalClipboardManager.current

    var query by remember { mutableStateOf("") }
    var selectedSchool by remember { mutableStateOf<School?>(null) }
    var redirectInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Mags",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Onofficiële desktop-app voor Magister — rustiger, sneller, en volledig lokaal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        controller.searchSchools(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Zoek je school of vul <school>.magister.net in") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )

                val directHost = query.trim().takeIf { Tenant.looksLikeHost(it) }?.let { host ->
                    School(name = host, host = Tenant.normalize(host))
                }
                val displayedSchools = remember(schoolResults, directHost) {
                    buildList {
                        if (directHost != null && schoolResults.none { it.host.equals(directHost.host, true) }) {
                            add(directHost)
                        }
                        addAll(schoolResults)
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                ) {
                    LazyColumn {
                        items(displayedSchools) { school ->
                            val isSelected = selectedSchool == school
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        },
                                    )
                                    .clickable { selectedSchool = school }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        school.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                    if (school.host.isNotBlank()) {
                                        Text(
                                            school.host,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = "Geselecteerd",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            val school = selectedSchool ?: displayedSchools.firstOrNull()
                            if (school != null) controller.startBrowserLogin(school)
                        },
                        enabled = (selectedSchool != null || displayedSchools.isNotEmpty()) && !refreshing,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Inloggen in browser")
                    }

                    OutlinedButton(
                        onClick = { controller.enterDemo() },
                        enabled = !refreshing,
                        modifier = Modifier.height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Voorbeeld")
                    }
                }

                if (activeAuth != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Stap 2 — Rond inloggen af",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Na het inloggen stuurt Magister je door naar een link die begint met m6loapp://oauth2redirect/. " +
                                "Kopieer die link uit je browser (of uit het netwerktabblad/adresbalk) en plak hem hieronder.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = redirectInput,
                            onValueChange = { redirectInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Plak m6loapp://oauth2redirect/... URL") },
                            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { controller.handleRedirectUrl(redirectInput) },
                                enabled = redirectInput.isNotBlank() && !refreshing,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (refreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(Modifier.size(8.dp))
                                }
                                Text("Voltooi inloggen")
                            }
                            TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(activeAuth!!.authorizeUrl))
                                    controller.showStatus("Inloglink gekopieerd naar klembord")
                                },
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Kopieer inloglink")
                            }
                        }
                        if (DesktopPaths.isWindows) {
                            TextButton(onClick = { controller.registerWindowsProtocol() }) {
                                Text("Koppel m6loapp:// automatisch op Windows")
                            }
                        }
                    }
                }

                if (!loginError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = loginError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}
