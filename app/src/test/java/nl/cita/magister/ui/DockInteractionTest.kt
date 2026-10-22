package nl.cita.magister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.ui.nav.MagsPage
import nl.cita.magister.ui.nav.PhotosDock
import nl.cita.magister.ui.theme.CitaTheme
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
class DockInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tappingEachTabChangesScreenAndKeepsSingleIndicator() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
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
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("dock-tab-Vandaag").assertIsSelected()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)

        compose.onNodeWithTag("dock-tab-Rooster").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("rooster-pager").assertExists()
        compose.onNodeWithTag("dock-tab-Rooster").assertIsSelected()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)

        compose.onNodeWithTag("dock-tab-Cijfers").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("cijfers-lijst").assertExists()
        compose.onNodeWithTag("dock-tab-Cijfers").assertIsSelected()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)

        compose.onNodeWithTag("dock-tab-Huiswerk").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("dock-tab-Huiswerk").assertIsSelected()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)

        compose.onNodeWithTag("dock-tab-Vandaag").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("VOLGENDE LES").assertExists()
        compose.onNodeWithTag("dock-tab-Vandaag").assertIsSelected()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)
    }

    @Test
    fun dockScreenshotAt412WithInsetsHasOneIndicator() {
        compose.setContent {
            Box(
                Modifier
                    .requiredSize(412.dp, 915.dp)
                    .windowInsetsPadding(WindowInsets(0, 48, 0, 32))
                    .testTag("dock-shot"),
            ) {
                CitaTheme {
                    Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp)) {
                        PhotosDock(
                            selected = MagsPage.Rooster,
                            expansion = 1f,
                            onSelect = {},
                            onSearch = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)
        compose.onNodeWithTag("dock-tab-Rooster").assertIsSelected()
        val image = compose.onNodeWithTag("dock-shot").captureToImage()
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun searchButtonBoundsStayInside360() {
        compose.setContent {
            Box(
                Modifier
                    .requiredSize(360.dp, 800.dp)
                    .testTag("dock-screen-360"),
            ) {
                CitaTheme {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        PhotosDock(
                            selected = MagsPage.Huiswerk,
                            expansion = 1f,
                            onSelect = {},
                            onSearch = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val screen = compose.onNodeWithTag("dock-screen-360").getUnclippedBoundsInRoot()
        val search = compose.onNodeWithTag("dock-search").getUnclippedBoundsInRoot()
        assertTrue("search left ${search.left} < screen ${screen.left}", search.left >= screen.left)
        assertTrue("search right ${search.right} > screen ${screen.right}", search.right <= screen.right)
        assertTrue("search top ${search.top} < screen ${screen.top}", search.top >= screen.top)
        assertTrue("search bottom ${search.bottom} > screen ${screen.bottom}", search.bottom <= screen.bottom)
        compose.onNodeWithTag("dock-tab-Huiswerk").assertIsSelected()
    }

    @Test
    fun indicatorSettlesOnSelectedTabNotOnVandaag() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp).testTag("dock-settle")) {
                CitaTheme {
                    PhotosDock(
                        selected = MagsPage.Rooster,
                        expansion = 1f,
                        onSelect = {},
                        onSearch = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_200)
        compose.waitForIdle()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)
        compose.onNodeWithTag("dock-tab-Rooster").assertIsSelected()
        val indicator = compose.onNodeWithTag("dock-indicator").getUnclippedBoundsInRoot()
        val rooster = compose.onNodeWithTag("dock-tab-Rooster").getUnclippedBoundsInRoot()
        val vandaag = compose.onNodeWithTag("dock-tab-Vandaag").getUnclippedBoundsInRoot()
        assertTrue(
            "indicator left ${indicator.left} should sit on Rooster ${rooster.left}",
            indicator.left >= rooster.left - 6.dp && indicator.right <= rooster.right + 6.dp,
        )
        val vCenter = (vandaag.left + vandaag.right) / 2
        assertTrue(
            "ghost pill: Vandaag center $vCenter still inside indicator $indicator",
            vCenter < indicator.left || vCenter > indicator.right,
        )
    }

    @Test
    fun pillWrapsContentWithoutDeadSpace() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp).testTag("dock-wrap")) {
                CitaTheme {
                    PhotosDock(
                        selected = MagsPage.Rooster,
                        expansion = 1f,
                        onSelect = {},
                        onSearch = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        val pill = compose.onNodeWithTag("dock-pill").getUnclippedBoundsInRoot()
        val last = compose.onNodeWithTag("dock-tab-Huiswerk").getUnclippedBoundsInRoot()
        val search = compose.onNodeWithTag("dock-search").getUnclippedBoundsInRoot()
        val slack = 16.dp
        assertTrue(
            "pill has dead space after last icon: pill.right=${pill.right} last.right=${last.right}",
            pill.right <= last.right + slack,
        )
        assertTrue("search should sit outside the pill", search.left >= pill.right)
        assertTrue(pill.right - pill.left < 320.dp)
    }

    @Test
    fun pressedTabDoesNotAddSecondIndicator() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    PhotosDock(
                        selected = MagsPage.Rooster,
                        expansion = 1f,
                        onSelect = {},
                        onSearch = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("dock-tab-Vandaag").performTouchInput {
            down(center)
        }
        compose.waitForIdle()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)
        compose.onNodeWithTag("dock-tab-Vandaag").performTouchInput { up() }
        compose.waitForIdle()
        compose.onAllNodesWithTag("dock-indicator").assertCountEquals(1)
        compose.onNodeWithTag("dock-tab-Rooster").assertIsSelected()
    }
}
