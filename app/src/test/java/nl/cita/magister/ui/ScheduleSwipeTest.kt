package nl.cita.magister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.model.InfoType
import nl.cita.magister.ui.schedule.DaySlot
import nl.cita.magister.ui.schedule.GapKind
import nl.cita.magister.ui.schedule.ScheduleScreen
import nl.cita.magister.ui.schedule.buildDaySlots
import nl.cita.magister.ui.theme.CitaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
class ScheduleSwipeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun swipeMovesToNextDay() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments(),
                        contentPadding = PaddingValues(0.dp),
                        initialDate = LocalDate.of(2026, 10, 21),
                        clockDate = LocalDate.of(2026, 10, 22),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Wiskunde B").assertExists()
        compose.onNodeWithTag("rooster-vandaag").assertIsDisplayed()
        compose.onNodeWithTag("rooster-pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Biologie").assertExists()
    }

    @Test
    fun tapChipJumpsAndSwipeStripChangesWeek() {
        val wednesday = LocalDate.of(2026, 10, 21)
        val thursday = LocalDate.of(2026, 10, 22)
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments(),
                        contentPadding = PaddingValues(0.dp),
                        initialDate = wednesday,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("week-pick-$thursday").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Biologie").assertExists()

        compose.onNodeWithTag("rooster-weekstrip").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag("week-pick-${thursday.plusWeeks(1)}").assertExists()

        compose.onNodeWithTag("rooster-weekstrip").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithTag("week-pick-$thursday").assertExists()
    }

    @Test
    fun thursdayShowsTussenuurAndLessonSheet() {
        val thursday = LocalDate.of(2026, 10, 22)
        val ofDay = MockFixtures.appointments().filter {
            it.startIso.startsWith("2026-10-22")
        }
        val slots = buildDaySlots(ofDay)
        assertEquals(1, slots.filterIsInstance<DaySlot.Gap>().count { it.kind == GapKind.FreePeriod })

        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments(),
                        contentPadding = PaddingValues(0.dp),
                        initialDate = thursday,
                        clockDate = thursday,
                        clockTime = java.time.LocalTime.of(11, 0),
                    )
                }
            }
        }
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithTag("tussenuur").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("rooster-now-line", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("les-kaart-nu").assertExists()
        compose.onNodeWithTag("les-nu-vorm", useUnmergedTree = true).assertExists()
        val line = compose.onNodeWithTag("rooster-now-line", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val card = compose.onNodeWithTag("les-kaart-nu").getUnclippedBoundsInRoot()
        assertTrue("now-line top ${line.top} outside card ${card.top}–${card.bottom}", line.top >= card.top && line.bottom <= card.bottom)
        compose.onNodeWithText("Geschiedenis").assertExists()
        compose.onNodeWithText("Geschiedenis").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("les-blad").assertExists()
        compose.onAllNodesWithText("MRS", substring = true).fetchSemanticsNodes().also { nodes ->
            assertTrue(nodes.size >= 2)
        }
    }

    @Test
    fun wednesdayBreaksAreNotTussenuur() {
        val wednesday = LocalDate.of(2026, 10, 21)
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments(),
                        contentPadding = PaddingValues(0.dp),
                        initialDate = wednesday,
                        clockDate = wednesday,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNode(
            hasTestTag("tussenuur") and hasAnyAncestor(hasTestTag("rooster-dag-2026-10-21")),
        ).assertDoesNotExist()
        assertTrue(
            compose.onAllNodes(
                hasTestTag("pauze") and hasAnyAncestor(hasTestTag("rooster-dag-2026-10-21")),
            ).fetchSemanticsNodes().isNotEmpty(),
        )
        compose.onNodeWithText("Wiskunde B").assertExists()
    }

    @Test
    fun swipeFridayGoesToNextMonday() {
        val friday = LocalDate.of(2026, 10, 23)
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments(),
                        contentPadding = PaddingValues(0.dp),
                        initialDate = friday,
                        clockDate = friday,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Weekend").assertDoesNotExist()
        compose.onNodeWithTag("week-pick-2026-10-24").assertDoesNotExist()
        compose.onNodeWithTag("week-pick-2026-10-23").assertExists()
        compose.onNodeWithTag("rooster-titel-2026-10-23").assertExists()
        compose.onNodeWithTag("rooster-weekstrip").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag("week-pick-2026-10-26").performClick()
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 3_000) {
            compose.onAllNodesWithTag("rooster-titel-2026-10-26").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("rooster-titel-2026-10-26").assertExists()
        compose.onNodeWithText("Weekend").assertDoesNotExist()
        compose.onNodeWithTag("rooster-weekstrip").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithTag("week-pick-2026-10-23").performClick()
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 3_000) {
            compose.onAllNodesWithTag("rooster-titel-2026-10-23").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("rooster-titel-2026-10-23").assertExists()
    }

    @Test
    fun saturdayWithoutLessonsOpensNextMonday() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments(),
                        contentPadding = PaddingValues(0.dp),
                        initialDate = LocalDate.of(2026, 10, 24),
                        clockDate = LocalDate.of(2026, 10, 24),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("rooster-dag-2026-10-26").assertExists()
        compose.onNodeWithText("Weekend").assertDoesNotExist()
        compose.onNodeWithTag("week-pick-2026-10-24").assertDoesNotExist()
        compose.onNodeWithTag("week-pick-2026-10-26").assertExists()
    }

    @Test
    fun saturdayWithLessonStaysVisible() {
        val saturday = MockFixtures.appointments().first().copy(
            id = 8099,
            startIso = "2026-10-24T09:00:00.0000000Z",
            endIso = "2026-10-24T09:50:00.0000000Z",
            title = "Inhaal",
            subjects = listOf("Inhaal"),
            periodFrom = 1,
            periodTo = 1,
            infoType = InfoType.None,
        )
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    ScheduleScreen(
                        appointments = MockFixtures.appointments() + saturday,
                        contentPadding = PaddingValues(0.dp),
                        initialDate = LocalDate.of(2026, 10, 24),
                        clockDate = LocalDate.of(2026, 10, 24),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Inhaal").assertExists()
        compose.onNodeWithTag("week-pick-2026-10-24").assertExists()
    }
}
