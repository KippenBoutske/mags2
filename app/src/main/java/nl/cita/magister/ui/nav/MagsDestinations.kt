package nl.cita.magister.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Grade
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
    DockTab(MagsPage.Rooster, "Rooster", Icons.Outlined.CalendarMonth),
    DockTab(MagsPage.Cijfers, "Cijfers", Icons.Outlined.Grade),
    DockTab(MagsPage.Huiswerk, "Huiswerk", Icons.Outlined.Checklist),
)

object MagsChrome {
    val TopBarHeight = 56.dp
    val DockClearance = 92.dp
    val DockGap = 10.dp
    val DockSide = 16.dp
    val PillHeight = 56.dp
    val FabSize = 56.dp
    val CompactFabSize = 48.dp
}
