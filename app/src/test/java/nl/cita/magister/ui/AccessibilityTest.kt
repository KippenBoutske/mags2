package nl.cita.magister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import nl.cita.magister.api.School
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.ui.login.LoginScreen
import nl.cita.magister.ui.nav.MagsPage
import nl.cita.magister.ui.nav.PhotosDock
import nl.cita.magister.ui.theme.CitaTheme
import nl.cita.magister.ui.today.TodayScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
class AccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun dockAndLoginExposeContentDescriptionsAnd48dpTargets() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    PhotosDock(
                        selected = MagsPage.Vandaag,
                        expansion = 1f,
                        onSelect = {},
                        onSearch = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Vandaag").assertIsDisplayed()
        compose.onNodeWithContentDescription("Rooster").assertExists()
        compose.onNodeWithContentDescription("Cijfers").assertExists()
        compose.onNodeWithContentDescription("Huiswerk").assertExists()
        compose.onNodeWithContentDescription("Zoeken").assertIsDisplayed()
        val search = compose.onNodeWithTag("dock-search").getUnclippedBoundsInRoot()
        assertTrue("search height ${search.bottom - search.top} < 48.dp", search.bottom - search.top >= 48.dp)
        val tab = compose.onNodeWithTag("dock-tab-Vandaag").getUnclippedBoundsInRoot()
        assertTrue("tab height ${tab.bottom - tab.top} < 48.dp", tab.bottom - tab.top >= 48.dp)
    }

    @Test
    fun loginHasSchoolSearchAndDemo() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    LoginScreen(
                        schoolName = "Citadel College",
                        tenantHost = "citadel.magister.net",
                        schools = listOf(
                            School("Citadel College", "citadel.magister.net", "Nijmegen / Lent"),
                            School("Canisius College", "canisius.magister.net", "Nijmegen"),
                        ),
                        searching = false,
                        lastError = null,
                        onQuery = {},
                        onSchoolChange = { _, _, _ -> },
                        onStartLogin = { "https://accounts.magister.net/connect/authorize" },
                        onWebViewLogin = {},
                        onDemo = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Zoek je school").assertExists()
        compose.onNodeWithContentDescription("Inloggen bij Magister").assertExists()
        compose.onNodeWithContentDescription("Voorbeeld zonder account").assertExists()
        compose.onNodeWithContentDescription("Kies Citadel College").assertExists()
        compose.onAllNodesWithTag("school-hit").assertCountEquals(2)
    }

    @Test
    fun fontScale13DoesNotClipDockOrLogin() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                Box(Modifier.requiredSize(412.dp, 915.dp).testTag("a11y-scale")) {
                    CitaTheme {
                        LoginScreen(
                            schoolName = "Citadel College",
                            tenantHost = "citadel.magister.net",
                            schools = emptyList(),
                            searching = false,
                            lastError = null,
                            onQuery = {},
                            onSchoolChange = { _, _, _ -> },
                            onStartLogin = { "https://example.test" },
                            onWebViewLogin = {},
                            onDemo = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Mags").assertIsDisplayed()
        compose.onNodeWithText("Inloggen").assertIsDisplayed()
        compose.onNodeWithTag("login-demo").assertIsDisplayed()
        val screen = compose.onNodeWithTag("a11y-scale").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("login-demo").getUnclippedBoundsInRoot().also { demo ->
            assertTrue(demo.bottom <= screen.bottom + 8.dp)
            assertTrue(demo.right <= screen.right)
        }
    }

    @Test
    fun fontScale13TodayAndShellStayOnScreen() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                Box(Modifier.requiredSize(412.dp, 915.dp).testTag("a11y-shell")) {
                    CitaTheme {
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
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("VOLGENDE LES").assertExists()
        val screen = compose.onNodeWithTag("a11y-shell").getUnclippedBoundsInRoot()
        val search = compose.onNodeWithTag("dock-search").getUnclippedBoundsInRoot()
        assertTrue(search.right <= screen.right)
        assertTrue(search.bottom <= screen.bottom)
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)
    }

    @Test
    fun todayLoadingShowsConceptSkeleton() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
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
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("today-skeleton").assertExists()
        compose.onNodeWithContentDescription("Vandaag laden").assertExists()
    }
}
