package nl.cita.magister.ui.homework

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.fixtures.manyHomeworkItems
import nl.cita.magister.model.InfoType
import nl.cita.magister.ui.formatWhen
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
class HomeworkGroupTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun groupsOverdueTodayAndToets() {
        val items = MockFixtures.homework()
        val buckets = groupHomework(items, today, HwFilter.Open)
        val overdue = buckets.filterIsInstance<HomeworkBucket.Overdue>().single()
        assertTrue(overdue.items.any { it.subject == "Wiskunde B" })
        assertTrue(overdue.items.none { it.isDone })
        val days = buckets.filterIsInstance<HomeworkBucket.Day>()
        assertTrue(days.any { it.label == "Vandaag" })
        assertTrue(items.any { isToets(it.infoType) })
        assertEquals("Do 29 okt", homeworkDayLabel(LocalDate.of(2026, 10, 29), today))
        assertEquals("Morgen", homeworkDayLabel(today.plusDays(1), today))
    }

    @Test
    fun manyHomeworkHasThirtyPlus() {
        assertTrue(MockFixtures.manyHomeworkItems(today).size >= 30)
    }

    @Test
    fun screenShowsVerlopenAndToetsChip() {
        compose.setContent {
            Box(Modifier.requiredSize(412.dp, 915.dp)) {
                CitaTheme {
                    HomeworkScreen(
                        items = MockFixtures.homework(),
                        contentPadding = PaddingValues(0.dp),
                        onToggle = { _, _ -> },
                        clockDate = today,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("hw-groep-verlopen").assertIsDisplayed()
        compose.onNodeWithText("Verlopen", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("hw-toets", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Toets", substring = true).assertExists()
        val toets = MockFixtures.homework().first { isToets(it.infoType) }
        assertEquals("Toets · ${formatWhen(toets.dueIso)}", homeworkMeta(toets))
        compose.onNodeWithTag("hw-groep-verlopen").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Wiskunde B").assertExists()
    }

    @Test
    fun toetsTypes() {
        assertTrue(isToets(InfoType.Test))
        assertTrue(isToets(InfoType.Written))
        assertTrue(isToets(InfoType.Oral))
        assertTrue(!isToets(InfoType.Homework))
    }
}
