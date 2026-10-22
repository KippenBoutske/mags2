package nl.cita.magister.ui.theme

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class CitaAction(val label: String, val selected: Boolean = false, val onClick: () -> Unit)

/**
 * Filter/actie-rij die nooit [androidx.compose.material3.ButtonGroup] gebruikt.
 *
 * ButtonGroup (material3 1.5 beta) crasht in measure wanneer hij `fillMaxWidth`
 * in een LazyColumn-item krijgt: min-breedtes van de items + overflow-knop
 * overschrijden de beschikbare breedte, waarna
 * `maxWidth must be >= than minWidth` volgt (Pixel 10 / 0.1.5).
 */
@Composable
fun CitaActionRow(
    actions: List<CitaAction>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.forEach { action ->
                FilterChip(
                    selected = action.selected,
                    onClick = action.onClick,
                    modifier = Modifier
                        .widthIn(min = 48.dp)
                        .heightIn(min = 48.dp)
                        .defaultMinSize(minHeight = 48.dp),
                    label = { Text(action.label, maxLines = 1) },
                )
            }
        }
    }
}
