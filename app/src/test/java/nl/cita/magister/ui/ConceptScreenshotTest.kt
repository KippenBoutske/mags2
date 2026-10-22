package nl.cita.magister.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
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
import nl.cita.magister.ui.nav.PhotosDock
import nl.cita.magister.ui.schedule.ScheduleScreen
import nl.cita.magister.ui.settings.MeerSection
import nl.cita.magister.ui.settings.SettingsScreen
import nl.cita.magister.ui.theme.CitaTheme
import nl.cita.magister.ui.today.TodayScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Eén setContent per test — ComposeTestRule weigert een tweede.
 * PNGs: Donker-warm op 412dp en 360dp.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
class ConceptScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun login412() = snap("login", 412) { login() }
    @Test fun login360() = snap("login", 360) { login() }
    @Test fun loginError412() = snap("login-error", 412) { login(error = true) }
    @Test fun loginSearch412() = snap("login-search", 412) { login(searching = true) }
    @Test fun todaySkeleton412() = snap("today-skeleton", 412) { todayLoading() }
    @Test fun today412() = snap("today", 412) { today() }
    @Test fun today360() = snap("today", 360) { today() }
    @Test fun rooster412() = snap("rooster", 412) { rooster() }
    @Test fun rooster360() = snap("rooster", 360) { rooster() }
    @Test fun cijfers412() = snap("cijfers", 412) { grades() }
    @Test fun cijfers360() = snap("cijfers", 360) { grades() }
    @Test fun cijfersVak412() = snap("cijfers-vak", 412) { grades(subjectId = 3) }
    @Test fun huiswerk412() = snap("huiswerk", 412) { homework() }
    @Test fun berichten412() = snap("berichten", 412) { messages() }
    @Test fun berichten403() = snap("berichten-403", 412) { messagesForbidden() }
    @Test fun meer412() = snap("meer", 412) { settings() }
    @Test fun meer360() = snap("meer", 360) { settings() }
    @Test fun meerAfwezigheid412() = snap("meer-afwezigheid", 412) { settings(MeerSection.Absence) }
    @Test fun meerInstellingen412() = snap("meer-instellingen", 412) { settings(MeerSection.Settings) }
    @Test fun dockVandaag412() = snap("dock-vandaag", 412) { dock(MagsPage.Vandaag) }
    @Test fun dockVandaag360() = snap("dock-vandaag", 360) { dock(MagsPage.Vandaag) }
    @Test fun dockHuiswerk360() = snap("dock-huiswerk", 360) { dock(MagsPage.Huiswerk) }
    @Test fun dockRooster412() = snap("dock-rooster", 412) { dock(MagsPage.Rooster) }
    @Test fun shellVandaag412() = snap("shell-vandaag", 412) { shell(MagsPage.Vandaag) }
    @Test fun shellRooster412() = snap("shell-rooster", 412) { shell(MagsPage.Rooster) }

    @Test
    fun dockRoosterHasSingleIndicator() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme { dock(MagsPage.Rooster) }
            }
        }
        compose.waitForIdle()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        writePng("dock-rooster-assert", 412, bitmap)
    }

    private fun snap(name: String, width: Int, content: @Composable () -> Unit) {
        compose.setContent {
            Box(Modifier.requiredSize(width.dp, 915.dp)) {
                CitaTheme(safeMode = false) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                        content = content,
                    )
                }
            }
        }
        compose.waitForIdle()
        writePng(name, width, compose.onRoot().captureToImage().asAndroidBitmap())
    }

    private fun writePng(name: String, width: Int, bitmap: Bitmap) {
        outDirs().forEach { dir ->
            dir.mkdirs()
            File(dir, "$name-${width}dp.png").outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }

    private fun outDirs(): List<File> = listOf(
        File("app/build/outputs/self-test/screenshots"),
        File("build/outputs/self-test/screenshots"),
        File("artifacts/screens-0.1.12"),
        File("/workspace/artifacts/screens-0.1.12"),
        File("/cursor/stores/self/artifacts/screens-0.1.12"),
        File("/opt/cursor/artifacts/0.1.12"),
    )

    @Composable
    private fun login(error: Boolean = false, searching: Boolean = false) {
        LoginScreen(
            schoolName = "Citadel College",
            tenantHost = "citadel.magister.net",
            schools = if (searching) emptyList() else listOf(School("Citadel College", "citadel.magister.net", "Nijmegen / Lent")),
            searching = searching,
            lastError = if (error) ApiFailure("/api/account", 403, """{"fouttype":"OnvoldoendePrivileges"}""") else null,
            onQuery = {},
            onSchoolChange = { _, _, _ -> },
            onStartLogin = { "https://accounts.magister.net/connect/authorize" },
            onWebViewLogin = {},
            onDemo = {},
        )
    }

    @Composable
    private fun todayLoading() {
        TodayScreen(
            profile = null,
            appointments = emptyList(),
            homework = emptyList(),
            averages = emptyList(),
            messages = emptyList(),
            contentPadding = PaddingValues(0.dp),
            onOpenPage = {},
            loading = true,
        )
    }

    @Composable
    private fun today() {
        TodayScreen(
            profile = MockFixtures.profile(),
            appointments = MockFixtures.appointments(),
            homework = MockFixtures.homework(),
            averages = GradeCalculator.averagesBySubject(MockFixtures.grades()),
            messages = MockFixtures.messages(),
            contentPadding = PaddingValues(0.dp),
            onOpenPage = {},
            clockDate = LocalDate.of(2026, 10, 22),
            clockTime = LocalTime.of(11, 0),
        )
    }

    @Composable
    private fun rooster() {
        ScheduleScreen(
            appointments = MockFixtures.appointments(),
            contentPadding = PaddingValues(0.dp),
            initialDate = LocalDate.of(2026, 10, 22),
            clockDate = LocalDate.of(2026, 10, 22),
            clockTime = LocalTime.of(11, 0),
        )
    }

    @Composable
    private fun grades(subjectId: Int? = null) {
        val grades = MockFixtures.grades()
        GradesScreen(
            profile = MockFixtures.profile(),
            averages = GradeCalculator.averagesBySubject(grades),
            grades = grades,
            refreshing = false,
            exporting = false,
            contentPadding = PaddingValues(0.dp),
            onExport = {},
            onShareJson = {},
            initialSubjectId = subjectId,
        )
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
            messagesError = ApiFailure("/api/berichten/mappen/alle", 403, """{"fouttype":"OnvoldoendePrivileges"}"""),
        )
    }

    @Composable
    private fun settings(section: MeerSection = MeerSection.Hub) {
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
            absences = MockFixtures.absences(),
            studyGuides = MockFixtures.studyGuides(),
            assignments = MockFixtures.assignments(),
            initialSection = section,
        )
    }

    @Composable
    private fun dock(selected: MagsPage) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            PhotosDock(selected = selected, expansion = 1f, onSelect = {}, onSearch = {})
        }
    }

    @Composable
    private fun shell(page: MagsPage) {
        val host = remember { SnackbarHostState() }
        val grades = MockFixtures.grades()
        MagsSignedInScaffold(
            profile = MockFixtures.profile(),
            grades = grades,
            averages = GradeCalculator.averagesBySubject(grades),
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
            initialPage = page,
            clockDate = LocalDate.of(2026, 10, 22),
            clockTime = LocalTime.of(11, 0),
        )
    }
}
