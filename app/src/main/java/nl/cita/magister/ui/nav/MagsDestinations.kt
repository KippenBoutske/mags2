package nl.cita.magister.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class MagsPage {
    Vandaag,
    Rooster,
    Cijfers,
    Huiswerk,
    Berichten,
    Meer,
    ;

    val isDockTab: Boolean
        get() = this == Vandaag || this == Rooster || this == Cijfers || this == Huiswerk
}

data class DockTab(
    val page: MagsPage,
    val label: String,
    val icon: ImageVector,
)

fun defaultDockTabs(): List<DockTab> = listOf(
    DockTab(MagsPage.Vandaag, "Vandaag", Icons.Outlined.Today),
    DockTab(MagsPage.Rooster, "Rooster", Icons.Outlined.CalendarViewDay),
    DockTab(MagsPage.Cijfers, "Cijfers", Icons.Outlined.Leaderboard),
    DockTab(MagsPage.Huiswerk, "Huiswerk", Icons.AutoMirrored.Outlined.Assignment),
)

object MagsChrome {
    val TopBarHeight = 56.dp
    const val DISPLAY_SMALL = 28
    const val DISPLAY_MEDIUM = 36
    const val DISPLAY_LARGE = 52
    const val HEADLINE_SMALL = 24
    const val HEADLINE_MEDIUM = 30
    const val HEADLINE_LARGE = 38
    const val TITLE_LARGE = 24
    const val TITLE_MEDIUM = 18
    const val BODY_LARGE = 16
    const val BODY_MEDIUM = 14
    const val LABEL_LARGE = 14
    const val LABEL_MEDIUM = 12
    const val LABEL_SMALL = 11

    val DockClearance = 200.dp
    /** Pill + 16.dp onder de dock + ruimte zodat het laatste item boven pill én scrim eindigt. */
    val DockGap = 10.dp
    val DockSide = 16.dp
    val PageGutter = 18.dp
    val CardGap = 14.dp
    val PillHeight = 66.dp
    val FabSize = 58.dp
    val CompactFabSize = 48.dp
    val ScrimHeight = 148.dp
    val ListBottomGap = 16.dp

    fun fromShortcut(value: String?): MagsPage? = when (value?.lowercase()) {
        "vandaag", "today" -> MagsPage.Vandaag
        "rooster", "schedule" -> MagsPage.Rooster
        "cijfers", "grades" -> MagsPage.Cijfers
        "huiswerk", "homework" -> MagsPage.Huiswerk
        "berichten", "messages" -> MagsPage.Berichten
        "meer", "more" -> MagsPage.Meer
        else -> null
    }
}
