package nl.cita.magister.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import nl.cita.magister.api.School
import nl.cita.magister.model.Absence
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.StudyGuide
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.HapticStrength
import nl.cita.magister.haptics.LocalMagsHaptics
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.formatWhen
import nl.cita.magister.ui.motion.magsSpring
import nl.cita.magister.ui.nav.MagsChrome
import nl.cita.magister.ui.profileLine
import nl.cita.magister.ui.school.SchoolPicker
import nl.cita.magister.ui.theme.CitaAction
import nl.cita.magister.ui.theme.CitaActionRow

enum class MeerSection { Hub, Absence, Guides, Assignments, Settings, School }

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
    hapticsEnabled: Boolean = true,
    onHaptics: (Boolean) -> Unit = {},
    hapticStrength: HapticStrength = HapticStrength.Strong,
    onHapticStrength: (HapticStrength) -> Unit = {},
    absences: List<Absence> = emptyList(),
    studyGuides: List<StudyGuide> = emptyList(),
    assignments: List<HomeworkItem> = emptyList(),
    absencesError: ApiFailure? = null,
    studyGuidesError: ApiFailure? = null,
    assignmentsError: ApiFailure? = null,
    unreadMessages: Int = 0,
    onOpenMessages: () -> Unit = {},
    initialSection: MeerSection = MeerSection.Hub,
) {
    var section by remember { mutableStateOf(initialSection) }
    BackHandler(section != MeerSection.Hub) { section = MeerSection.Hub }

    AnimatedContent(
        targetState = section,
        transitionSpec = { fadeIn(magsSpring()) togetherWith fadeOut() },
        label = "meer-pagina",
        modifier = Modifier.fillMaxSize(),
    ) { dest ->
        when (dest) {
            MeerSection.Hub -> MeerHub(
                profile = profile,
                schoolName = schoolName,
                demoMode = demoMode,
                unreadMessages = unreadMessages,
                contentPadding = contentPadding,
                onOpen = { section = it },
                onOpenMessages = onOpenMessages,
                onLogout = onLogout,
            )
            MeerSection.Absence -> MeerListPage(
                title = "Afwezigheid",
                contentPadding = contentPadding,
                onBack = { section = MeerSection.Hub },
            ) {
                SectionError(absencesError)
                if (absences.isEmpty() && absencesError == null) {
                    Text("Geen afwezigheid.")
                }
                absences.forEach { item ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .testTag("meer-afwezig-item"),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(item.description, style = MaterialTheme.typography.titleMedium)
                        Text(
                            profileLine(
                                item.lesson,
                                item.code,
                                if (item.excused) "geoorloofd" else "ongeoorloofd",
                                formatWhen(item.startIso),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            MeerSection.Guides -> MeerListPage(
                title = "Studiewijzers",
                contentPadding = contentPadding,
                onBack = { section = MeerSection.Hub },
            ) {
                SectionError(studyGuidesError)
                if (studyGuides.isEmpty() && studyGuidesError == null) {
                    Text("Geen studiewijzers.")
                }
                studyGuides.forEach { guide ->
                    Column(
                        Modifier.fillMaxWidth().testTag("meer-wijzer-item"),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(guide.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            profileLine(
                                if (guide.isProject) "Project" else null,
                                guide.subjectCodes.joinToString().ifBlank { null },
                            ).ifBlank { "Studiewijzer" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            MeerSection.Assignments -> MeerListPage(
                title = "Opdrachten",
                contentPadding = contentPadding,
                onBack = { section = MeerSection.Hub },
            ) {
                SectionError(assignmentsError)
                if (assignments.isEmpty() && assignmentsError == null) {
                    Text("Geen opdrachten.")
                }
                assignments.forEach { item ->
                    Column(
                        Modifier.fillMaxWidth().testTag("meer-opdracht-item"),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            profileLine(item.subject, formatWhen(item.dueIso).takeIf { it.isNotBlank() }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            MeerSection.Settings -> MeerListPage(
                title = "Instellingen",
                contentPadding = contentPadding,
                onBack = { section = MeerSection.Hub },
            ) {
                SettingsPane(
                    webhookUrl = webhookUrl,
                    hasWebhookKey = hasWebhookKey,
                    lastError = lastError,
                    dynamicColor = dynamicColor,
                    onDynamicColor = onDynamicColor,
                    notifications = notifications,
                    onNotifications = onNotifications,
                    hapticsEnabled = hapticsEnabled,
                    onHaptics = onHaptics,
                    hapticStrength = hapticStrength,
                    onHapticStrength = onHapticStrength,
                    onSaveWebhook = onSaveWebhook,
                    onExport = onExport,
                    onShareJson = onShareJson,
                )
            }
            MeerSection.School -> MeerListPage(
                title = "Andere school",
                contentPadding = contentPadding,
                onBack = { section = MeerSection.Hub },
            ) {
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
    }
}

@Composable
private fun MeerHub(
    profile: StudentProfile?,
    schoolName: String,
    demoMode: Boolean,
    unreadMessages: Int,
    contentPadding: PaddingValues,
    onOpen: (MeerSection) -> Unit,
    onOpenMessages: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = MagsChrome.PageGutter,
                end = MagsChrome.PageGutter,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + MagsChrome.ListBottomGap,
            )
            .testTag("meer-hub"),
        verticalArrangement = Arrangement.spacedBy(MagsChrome.CardGap),
    ) {
        PageTitle("Meer", null)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("meer-profiel"),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    profile?.let { "${it.firstName} ${it.lastName}" }?.trim().orEmpty()
                        .ifBlank { "Niet ingelogd" },
                    style = MaterialTheme.typography.headlineMedium,
                )
                val meta = profileLine(
                    profile?.className,
                    profile?.study,
                    schoolName.ifBlank { profile?.schoolName },
                )
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("meer-profiel-regel"),
                    )
                }
                if (demoMode) {
                    Text("Voorbeeld", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        MeerGroup(
            listOf(
                MeerItem("Berichten", unreadMessages.takeIf { it > 0 }?.toString(), "meer-rij-berichten") { onOpenMessages() },
                MeerItem("Afwezigheid", testTag = "meer-rij-afwezigheid") { onOpen(MeerSection.Absence) },
                MeerItem("Studiewijzers", testTag = "meer-rij-studiewijzers") { onOpen(MeerSection.Guides) },
                MeerItem("Opdrachten", testTag = "meer-rij-opdrachten") { onOpen(MeerSection.Assignments) },
            ),
        )
        MeerGroup(
            listOf(
                MeerItem("Instellingen", testTag = "meer-rij-instellingen") { onOpen(MeerSection.Settings) },
                MeerItem("Andere school", testTag = "meer-rij-school") { onOpen(MeerSection.School) },
                MeerItem("Uitloggen", testTag = "meer-rij-uitloggen", chevron = false, onClick = onLogout),
            ),
        )
        Spacer(Modifier.height(24.dp))
    }
}

private data class MeerItem(
    val title: String,
    val trailing: String? = null,
    val testTag: String,
    val chevron: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
private fun MeerGroup(items: List<MeerItem>) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("meer-groep"),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    )
                }
                MeerRow(item)
            }
        }
    }
}

@Composable
private fun MeerRow(item: MeerItem) {
    val title = item.title
    val trailing = item.trailing
    val testTag = item.testTag
    val chevron = item.chevron
    val onClick = item.onClick
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(testTag)
            .semantics { contentDescription = title },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (!trailing.isNullOrBlank()) {
            Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        if (chevron) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MeerListPage(
    title: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = MagsChrome.PageGutter,
                end = MagsChrome.PageGutter,
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + MagsChrome.ListBottomGap,
            )
            .testTag("meer-pagina"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("meer-terug")) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Terug")
            }
            PageTitle(title, null)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun SettingsPane(
    webhookUrl: String,
    hasWebhookKey: Boolean,
    lastError: ApiFailure?,
    dynamicColor: Boolean,
    onDynamicColor: (Boolean) -> Unit,
    notifications: Boolean,
    onNotifications: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHaptics: (Boolean) -> Unit,
    hapticStrength: HapticStrength,
    onHapticStrength: (HapticStrength) -> Unit,
    onSaveWebhook: (url: String, key: String) -> Unit,
    onExport: () -> Unit,
    onShareJson: () -> Unit,
) {
    var url by remember(webhookUrl) { mutableStateOf(webhookUrl) }
    var key by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(false) }
    lastError?.let { SectionError(it) }
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Material You")
            Text(
                "Licht en donker volgen de telefoon. Uit = warm palet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = dynamicColor,
            onCheckedChange = onDynamicColor,
            modifier = Modifier.semantics { contentDescription = "Material You" },
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Meldingen", modifier = Modifier.weight(1f))
        Switch(
            checked = notifications,
            onCheckedChange = onNotifications,
            modifier = Modifier.semantics { contentDescription = "Meldingen" },
        )
    }
    val haptics = LocalMagsHaptics.current
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("meer-trillen"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Trillen", modifier = Modifier.weight(1f))
        Switch(
            checked = hapticsEnabled,
            onCheckedChange = { next ->
                if (next) {
                    onHaptics(true)
                    haptics.play(HapticEvent.HomeworkDone, ignoreAppToggle = true)
                } else {
                    haptics.play(HapticEvent.HomeworkUndo)
                    onHaptics(false)
                }
            },
            modifier = Modifier.semantics { contentDescription = "Trillen" },
        )
    }
    if (hapticsEnabled) {
        CitaActionRow(
            actions = HapticStrength.entries.map { level ->
                CitaAction(level.label, selected = hapticStrength == level) {
                    haptics.applyStrength(level)
                    onHapticStrength(level)
                    haptics.play(HapticEvent.TabSwitch, ignoreAppToggle = true)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("meer-trillen-sterkte"),
        )
    }
    TextButton(
        onClick = { advanced = !advanced },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meer-geavanceerd"),
    ) {
        Text(if (advanced) "Geavanceerd ▾" else "Geavanceerd")
    }
    AnimatedVisibility(advanced) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Plan Goon", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Adres") },
                modifier = Modifier.fillMaxWidth().testTag("meer-adres"),
                singleLine = true,
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(if (hasWebhookKey) "Nieuwe sleutel" else "Sleutel") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(onClick = { onSaveWebhook(url, key) }, modifier = Modifier.fillMaxWidth()) {
                Text("Opslaan")
            }
            CitaActionRow(
                actions = listOf(
                    CitaAction("Exporteer", onClick = onExport),
                    CitaAction("Deel", onClick = onShareJson),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
