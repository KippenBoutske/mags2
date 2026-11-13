package nl.cita.magister.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import nl.cita.magister.ui.theme.CitaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h740dp")
class DockLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun dockLabelsStayVisibleOnCompactScreen() {
        compose.setContent {
            Box(Modifier.requiredSize(320.dp, 740.dp)) {
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

        compose.onNodeWithTag("dock-tab-Vandaag").assertIsDisplayed()
        compose.onNodeWithTag("dock-tab-Rooster").assertIsDisplayed()
        compose.onNodeWithTag("dock-tab-Cijfers").assertIsDisplayed()
        compose.onNodeWithTag("dock-tab-Huiswerk").assertIsDisplayed()
        compose.onNodeWithContentDescription("Zoeken").assertIsDisplayed()
        compose.onNodeWithText("Vandaag").assertIsDisplayed()
        compose.onNodeWithText("Huiswerk").assertIsDisplayed()
    }
}
