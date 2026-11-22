package nl.cita.magister.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import nl.cita.magister.desktop.data.DesktopPaths
import nl.cita.magister.desktop.data.ThemeMode
import nl.cita.magister.desktop.ui.DesktopFormat
import nl.cita.magister.desktop.ui.MagsDesktopController

private enum class MoreTab(val label: String) {
    Settings("Instellingen"),
    Absences("Afwezigheid"),
    StudyGuides("Studiewijzers"),
    Assignments("Opdrachten"),
}

@Composable
fun MoreView(controller: MagsDesktopController) {
    val profile by controller.profile.collectAsState()
    val prefs by controller.prefs.collectAsState()
    val absences by controller.absences.collectAsState()
    val studyGuides by controller.studyGuides.collectAsState()
    val assignments by controller.assignments.collectAsState()
    val warnings by controller.warnings.collectAsState()

    var tab by remember { mutableStateOf(MoreTab.Settings) }
    var webhookUrl by remember(prefs.webhookUrl) { mutableStateOf(prefs.webhookUrl) }
    var webhookKey by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Meer", style = MaterialTheme.typography.displayMedium)
            profile?.let { p ->
                Text(
                    DesktopFormat.profileLine("${p.firstName} ${p.lastName}".trim(), p.className, p.schoolName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoreTab.entries.forEach { t ->
                FilterChip(
                    selected = tab == t,
                    onClick = { tab = t },
                    label = { Text(t.label) },
                )
            }
        }

        when (tab) {
            MoreTab.Settings -> {
                // Appearance
                SectionCard("Uiterlijk") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.System to "Systeem",
                            ThemeMode.Light to "Licht",
                            ThemeMode.Dark to "Donker",
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = { controller.setThemeMode(mode) },
                                label = { Text(label) },
                            )
                        }
                    }
                }

                // Plan Goon export
                SectionCard("Geavanceerd — Cijfers exporteren (Plan Goon)") {
                    Text(
                        "Stel een https-endpoint en optionele Bearer-sleutel in om cijfers als JSON door te sturen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Webhook URL (https://...)") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                    OutlinedTextField(
                        value = webhookKey,
                        onValueChange = { webhookKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                if (controller.repo.prefs.hasWebhookKey()) {
                                    "Bearer-sleutel (opgeslagen — laat leeg om te behouden)"
                                } else {
                                    "Bearer-sleutel (optioneel)"
                                },
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                controller.saveWebhook(webhookUrl, webhookKey.takeIf { it.isNotBlank() })
                                webhookKey = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Opslaan")
                        }
                        if (prefs.webhookUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = { controller.exportToPlanGoon() },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Nu versturen")
                            }
                        }
                    }
                }

                // Windows protocol handler
                if (DesktopPaths.isWindows) {
                    SectionCard("Windows-integratie") {
                        Text(
                            "Koppel m6loapp:// in je Windows-gebruikersprofiel zodat inloggen via de browser automatisch terugkeert naar Mags.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { controller.registerWindowsProtocol() },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Registreer m6loapp:// protocol")
                        }
                    }
                }

                // Warnings if any
                if (warnings.isNotEmpty()) {
                    SectionCard("Sync-meldingen (${warnings.size})") {
                        warnings.forEach { w ->
                            Text(
                                "${w.summary()}: ${w.details().take(140)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Account
                SectionCard("Account") {
                    Text(
                        if (prefs.isDemo) {
                            "Je gebruikt momenteel de voorbeeldmodus (Citadel College)."
                        } else {
                            "Ingelogd op ${prefs.tenantHost.ifBlank { "Magister" }}."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { controller.logout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(if (prefs.isDemo) "Verlaat voorbeeldmodus" else "Uitloggen / School wisselen")
                    }
                }
            }

            MoreTab.Absences -> {
                SectionCard("Afwezigheid") {
                    if (absences.isEmpty()) {
                        Text(
                            "Geen meldingen van afwezigheid.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        absences.forEachIndexed { idx, a ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(a.description, style = MaterialTheme.typography.titleMedium)
                                    val meta = listOfNotNull(
                                        DesktopFormat.formatWhen(a.startIso).takeIf { it.isNotBlank() },
                                        a.period?.let { "uur $it" },
                                        a.lesson?.takeIf { it.isNotBlank() },
                                    ).joinToString(" · ")
                                    if (meta.isNotBlank()) {
                                        Text(
                                            meta,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (a.excused) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    },
                                ) {
                                    Text(
                                        if (a.excused) "Geoorloofd" else "Ongeoorloofd",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (a.excused) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            MoreTab.StudyGuides -> {
                SectionCard("Studiewijzers & Projecten") {
                    if (studyGuides.isEmpty()) {
                        Text(
                            "Geen studiewijzers gevonden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        studyGuides.forEachIndexed { idx, g ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(g.title, style = MaterialTheme.typography.titleMedium)
                                val meta = listOfNotNull(
                                    g.subjectCodes.joinToString(", ").takeIf { it.isNotBlank() },
                                    if (g.isProject) "Project" else "Studiewijzer",
                                    g.fromIso?.take(10),
                                ).joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            MoreTab.Assignments -> {
                SectionCard("Opdrachten") {
                    if (assignments.isEmpty()) {
                        Text(
                            "Geen opdrachten gevonden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        assignments.forEachIndexed { idx, a ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(a.title, style = MaterialTheme.typography.titleMedium)
                                val meta = listOfNotNull(
                                    a.subject.takeIf { it.isNotBlank() },
                                    DesktopFormat.formatWhen(a.dueIso).takeIf { it.isNotBlank() },
                                    if (a.isDone) "Ingeleverd" else "Open",
                                ).joinToString(" · ")
                                Text(
                                    meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
