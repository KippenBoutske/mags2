package nl.cita.magister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import nl.cita.magister.api.School
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.ui.grades.GradesScreen
import nl.cita.magister.ui.homework.HomeworkScreen
import nl.cita.magister.ui.login.LoginScreen
import nl.cita.magister.ui.messages.MessagesScreen
import nl.cita.magister.ui.nav.MagsPage
import nl.cita.magister.ui.nav.MagsTopBar
import nl.cita.magister.ui.nav.PhotosDock
import nl.cita.magister.ui.schedule.ScheduleScreen
import nl.cita.magister.ui.settings.SettingsScreen
import nl.cita.magister.ui.theme.CitaAction
import nl.cita.magister.ui.theme.CitaActionRow
import nl.cita.magister.ui.theme.CitaTheme
import nl.cita.magister.ui.today.TodayScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Meet elke schermcompositie op Pixel-10-breedte (412dp) en smal (320dp).
 * Vangt measure-crashes zoals ButtonGroup's `maxWidth must be >= minWidth`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
class ScreenMeasureTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun login_412() {
        measure(412) {
            LoginScreen(
                schoolName = "Citadel College",
                tenantHost = "citadel.magister.net",
                schools = listOf(School("Citadel College", "citadel.magister.net")),
                searching = false,
                lastError = ApiFailure("/api/account", 403, """{"fouttype":"OnvoldoendePrivileges"}"""),
                onQuery = {},
                onSchoolChange = { _, _, _ -> },
                onStartLogin = { "https://accounts.magister.net/connect/authorize" },
                onWebViewLogin = {},
                onDemo = {},
            )
        }
        compose.onNodeWithText("Mags").assertExists()
    }

    @Test
    fun login_320() {
        measure(320) {
            LoginScreen(
                schoolName = "",
                tenantHost = "",
                schools = emptyList(),
                searching = true,
                lastError = null,
                onQuery = {},
                onSchoolChange = { _, _, _ -> },
                onStartLogin = { "https://example.test" },
                onWebViewLogin = {},
                onDemo = {},
            )
        }
        compose.onNodeWithText("Inloggen").assertExists()
    }

    @Test
    fun today_412() = measure(412) { today() }

    @Test
    fun today_320() = measure(320) { today() }

    @Test
    fun grades_412() = measure(412) { grades() }

    @Test
    fun grades_320() = measure(320) { grades() }

    @Test
    fun gradesSubject_412() = measure(412) { grades(subjectId = 3) }

    @Test
    fun schedule_412() = measure(412) { schedule() }

    @Test
    fun schedule_320() = measure(320) { schedule() }

    @Test
    fun homework_412() = measure(412) { homework() }

    @Test
    fun homework_320() = measure(320) { homework() }

    @Test
    fun messages_412() = measure(412) { messages() }

    @Test
    fun messages_320() = measure(320) { messages() }

    @Test
    fun messages403HidesRawHttpUntilDetails() {
        measure(412) { messagesForbidden() }
        compose.onNodeWithText("Berichten zijn voor dit account niet beschikbaar.").assertIsDisplayed()
        compose.onNodeWithText("Geen berichten.").assertIsDisplayed()
        compose.onNodeWithText("/api/berichten/mappen/alle", substring = true).assertDoesNotExist()
        compose.onNodeWithText("HTTP 403", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("section-error-details").performClick()
        compose.onNodeWithText("/api/berichten/mappen/alle", substring = true).assertIsDisplayed()
        compose.onNodeWithText("HTTP 403", substring = true).assertIsDisplayed()
    }

    @Test
    fun grades403StaysOnSection() {
        measure(412) { gradesForbidden() }
        compose.onNodeWithText("Cijfers zijn nu niet beschikbaar.").assertExists()
        compose.onNodeWithText("Nog geen cijfers").assertExists()
        compose.onNodeWithText("/api/personen", substring = true).assertDoesNotExist()
        compose.onNodeWithText("HTTP 403", substring = true).assertDoesNotExist()
    }

    @Test
    fun settings_412() = measure(412) { settings() }

    @Test
    fun settings_320() = measure(320) { settings() }

    @Test
    fun photosDockExpanded_412() {
        measure(412) { dock(1f, MagsPage.Vandaag) }
        compose.onNodeWithContentDescription("Zoeken").assertExists()
        compose.onNodeWithText("Vandaag").assertExists()
    }

    @Test
    fun photosDockExpanded_320() {
        measure(320) { dock(1f, MagsPage.Cijfers) }
        compose.onNodeWithContentDescription("Zoeken").assertExists()
        compose.onNodeWithText("Cijfers").assertExists()
    }

    @Test
    fun photosDockCollapsed_412() {
        measure(412) { dock(0f, MagsPage.Rooster) }
        compose.onNodeWithContentDescription("Zoeken").assertExists()
    }

    @Test
    fun photosDockCollapsed_320() {
        measure(320) { dock(0f, MagsPage.Huiswerk) }
        compose.onNodeWithContentDescription("Zoeken").assertExists()
    }

    @Test
    fun photosDockNoSelection_320() {
        measure(320) { dock(1f, null) }
        compose.onNodeWithTag("dock-tab-Vandaag").assertExists()
        compose.onNodeWithContentDescription("Zoeken").assertExists()
    }

    @Test
    fun topBar_412() {
        measure(412) { topBar() }
        compose.onNodeWithContentDescription("Profiel").assertExists()
    }

    @Test
    fun topBar_320() {
        measure(320) { topBar() }
        compose.onNodeWithContentDescription("Profiel").assertExists()
    }

    @Test
    fun signedInShell_412() {
        measure(412) { shell() }
        compose.onNodeWithTag("dock-tab-Vandaag").assertExists()
        compose.onNodeWithContentDescription("Zoeken").assertExists()
        compose.onNodeWithContentDescription("Profiel").assertExists()
    }

    @Test
    fun signedInShell_320() {
        measure(320) { shell() }
        compose.onNodeWithTag("dock-tab-Vandaag").assertExists()
        compose.onNodeWithContentDescription("Zoeken").assertExists()
        compose.onNodeWithContentDescription("Profiel").assertExists()
    }

    @Test
    fun actionRowSevenItemsInLazyColumn_320() {
        measure(320) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    CitaActionRow(
                        actions = (1..7).map { CitaAction("Dag $it") {} },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item { Text("onder de rij") }
            }
        }
        compose.onNodeWithText("Dag 1").assertExists()
    }

    @Test
    fun actionRowSevenItemsInLazyColumn_412() {
        measure(412) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    CitaActionRow(
                        actions = (1..7).map { CitaAction("Ma $it") {} },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        compose.onNodeWithText("Ma 1").assertExists()
    }

    private fun measure(widthDp: Int, content: @Composable () -> Unit) {
        compose.setContent {
            Box(Modifier.requiredSize(widthDp.dp, 915.dp)) {
                CitaTheme(safeMode = false, content = content)
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun today() {
        TodayScreen(
            profile = MockFixtures.profile(),
            appointments = MockFixtures.appointments(),
            homework = MockFixtures.homework(),
            averages = GradeCalculator.averagesBySubject(MockFixtures.grades()),
            grades = MockFixtures.grades(),
            messages = MockFixtures.messages(),
            contentPadding = PaddingValues(0.dp),
            onOpenPage = {},
        )
    }

    @Composable
    private fun grades(subjectId: Int? = null) {
        val grades = MockFixtures.grades()
        GradesScreen(
            profile = MockFixtures.profile(),
            averages = GradeCalculator.averagesBySubject(grades),
            grades = grades,
            latestGrades = GradeCalculator.latestEarnedGrades(grades),
            refreshing = false,
            exporting = false,
            contentPadding = PaddingValues(0.dp),
            onExport = {},
            onShareJson = {},
            initialSubjectId = subjectId,
        )
    }

    @Composable
    private fun schedule() {
        ScheduleScreen(MockFixtures.appointments(), PaddingValues(0.dp))
    }

    @Composable
    private fun homework() {
        HomeworkScreen(
            items = MockFixtures.homework(),
            contentPadding = PaddingValues(bottom = 80.dp),
            onToggle = { _, _ -> },
        )
    }

    @Composable
    private fun messages() {
        MessagesScreen(
            messages = MockFixtures.messages(),
            detail = null,
            contentPadding = PaddingValues(0.dp),
            onOpen = {},
            onClose = {},
        )
    }

    @Composable
    private fun messagesForbidden() {
        MessagesScreen(
            messages = emptyList(),
            detail = null,
            contentPadding = PaddingValues(0.dp),
            onOpen = {},
            onClose = {},
            messagesError = ApiFailure(
                "/api/berichten/mappen/alle",
                403,
                """{"fouttype":"OnvoldoendePrivileges"}""",
            ),
        )
    }

    @Composable
    private fun gradesForbidden() {
        GradesScreen(
            profile = MockFixtures.profile(),
            averages = emptyList(),
            grades = emptyList(),
            refreshing = false,
            exporting = false,
            contentPadding = PaddingValues(0.dp),
            onExport = {},
            onShareJson = {},
            gradesError = ApiFailure("/api/personen/1/cijfers", 403, ""),
        )
    }

    @Composable
    private fun settings() {
        SettingsScreen(
            profile = MockFixtures.profile(),
            tenantHost = "citadel.magister.net",
            schoolName = "Citadel College",
            webhookUrl = "",
            hasWebhookKey = false,
            demoMode = true,
            schools = emptyList(),
            searching = false,
            lastError = null,
            contentPadding = PaddingValues(0.dp),
            onQuery = {},
            onSwitchSchool = { _, _, _ -> },
            onSaveWebhook = { _, _ -> },
            onExport = {},
            onShareJson = {},
            onLogout = {},
        )
    }

    @Composable
    private fun dock(expansion: Float, selected: MagsPage?) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            PhotosDock(
                selected = selected,
                expansion = expansion,
                onSelect = {},
                onSearch = {},
            )
        }
    }

    @Composable
    private fun topBar() {
        MagsTopBar(
            profile = MockFixtures.profile(),
            unreadMessages = 2,
            onMessages = {},
            onMore = {},
        )
    }

    @Composable
    private fun shell() {
        val host = remember { SnackbarHostState() }
        val grades = MockFixtures.grades()
        MagsSignedInScaffold(
            profile = MockFixtures.profile(),
            grades = grades,
            averages = GradeCalculator.averagesBySubject(grades),
            latestGrades = GradeCalculator.latestEarnedGrades(grades),
            appointments = MockFixtures.appointments(),
            homework = MockFixtures.homework(),
            messages = MockFixtures.messages(),
            detail = null,
            refreshing = false,
            exporting = false,
            tenantHost = "citadel.magister.net",
            schoolName = "Citadel College",
            webhookUrl = "",
            hasWebhookKey = false,
            demoMode = true,
            schools = emptyList(),
            searching = false,
            lastError = null,
            snackbarHost = host,
            onRefresh = {},
            onExport = {},
            onShareJson = {},
            onToggleHomework = { _, _ -> },
            onOpenMessage = {},
            onCloseMessage = {},
            onQuery = {},
            onSwitchSchool = { _, _, _ -> },
            onSaveWebhook = { _, _ -> },
            onLogout = {},
        )
    }
}
