package nl.cita.magister.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.cita.magister.model.MessageDetail
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.parse.stripHtml
import nl.cita.magister.ui.formatWhen

@Composable
fun MessagesScreen(
    messages: List<MessagePreview>,
    detail: MessageDetail?,
    contentPadding: PaddingValues,
    onOpen: (Int) -> Unit,
    onClose: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Berichten", style = MaterialTheme.typography.headlineMedium)
            Text("Alleen lezen. Versturen kan later.", style = MaterialTheme.typography.bodyMedium)
        }
        if (messages.isEmpty()) {
            item { Text("Geen berichten in de cache.") }
        }
        items(messages, key = { it.id }) { message ->
            val colors = if (message.isRead) {
                CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
            } else {
                CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
            }
            Card(
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(message.id) },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (message.isRead) message.sender else "●  ${message.sender}", style = MaterialTheme.typography.labelLarge)
                    Text(message.subject, style = MaterialTheme.typography.titleMedium)
                    message.snippet?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
                    Text(formatWhen(message.sentAt), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    if (detail != null) {
        ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(
                Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(detail.preview.subject, style = MaterialTheme.typography.headlineSmall)
                Text(detail.preview.sender, style = MaterialTheme.typography.titleSmall)
                Text(formatWhen(detail.preview.sentAt), style = MaterialTheme.typography.labelMedium)
                Text(detail.bodyHtml?.stripHtml() ?: detail.preview.snippet.orEmpty())
            }
        }
    }
}
