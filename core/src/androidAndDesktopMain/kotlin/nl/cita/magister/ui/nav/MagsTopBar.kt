package nl.cita.magister.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.hapticOnDown
import nl.cita.magister.model.StudentProfile

@Composable
fun MagsTopBar(
    profile: StudentProfile?,
    unreadMessages: Int,
    onMessages: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MagsChrome.TopBarHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        actions()
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .hapticOnDown(HapticEvent.LongPress)
                    .clickable { menu = true }
                    .semantics { contentDescription = "Profiel" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(profile),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (unreadMessages > 0) "Berichten ($unreadMessages)" else "Berichten",
                        )
                    },
                    onClick = {
                        menu = false
                        onMessages()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Meer") },
                    onClick = {
                        menu = false
                        onMore()
                    },
                )
            }
        }
    }
}

fun initials(profile: StudentProfile?): String {
    val first = profile?.firstName?.firstOrNull()?.uppercaseChar()
    val last = profile?.lastName?.firstOrNull()?.uppercaseChar()
    return buildString {
        append(first ?: 'M')
        if (last != null) append(last)
    }
}
