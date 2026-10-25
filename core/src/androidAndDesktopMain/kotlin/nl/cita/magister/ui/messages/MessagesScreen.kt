package nl.cita.magister.ui.messages

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.MessageDetail
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.parse.stripHtml
import nl.cita.magister.ui.components.ListSkeleton
import nl.cita.magister.ui.components.PageTitle
import nl.cita.magister.ui.components.SectionError
import nl.cita.magister.ui.formatWhen
import nl.cita.magister.ui.motion.PressScaleCard
import nl.cita.magister.ui.motion.pageFadeSpring
import nl.cita.magister.ui.motion.staggerEnter
import nl.cita.magister.ui.nav.MagsChrome

@Composable
fun MessagesScreen(
    messages: List<MessagePreview>,
    detail: MessageDetail?,
    contentPadding: PaddingValues,
    onOpen: (Int) -> Unit,
    onClose: () -> Unit,
    messagesError: ApiFailure? = null,
    loading: Boolean = false,
) {
    val showSkeleton = loading && messages.isEmpty() && messagesError == null
    val listPadding = PaddingValues(
        start = MagsChrome.PageGutter,
        end = MagsChrome.PageGutter,
        top = contentPadding.calculateTopPadding() + 4.dp,
        bottom = contentPadding.calculateBottomPadding() + MagsChrome.ListBottomGap,
    )
    Crossfade(
        targetState = showSkeleton,
        animationSpec = pageFadeSpring(),
        label = "berichten-load",
        modifier = Modifier.fillMaxSize(),
    ) { skeleton ->
    if (skeleton) {
        Column(
            Modifier.fillMaxSize().padding(listPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PageTitle("Berichten", null)
            ListSkeleton(rows = 4, contentDescription = "Berichten laden")
        }
    } else {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { PageTitle("Berichten", null) }
        messagesError?.let { item { SectionError(it) } }
        if (messages.isEmpty()) {
            item { Text("Geen berichten.") }
        }
        itemsIndexed(messages, key = { _, it -> it.id }) { index, message ->
            val colors = if (message.isRead) {
                CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
            } else {
                CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
            }
            PressScaleCard(
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerEnter(index, key = message.id)
                    .animateItem(),
                onClick = { onOpen(message.id) },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (message.isRead) message.sender else "●  ${message.sender}", style = MaterialTheme.typography.labelLarge)
                    Text(message.subject, style = MaterialTheme.typography.titleMedium)
                    message.snippet?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
                    Text(
                        buildString {
                            append(formatWhen(message.sentAt))
                            if (message.hasAttachments) append(" · bijlage")
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
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
                if (detail.attachments.isNotEmpty()) {
                    Text("Bijlagen", style = MaterialTheme.typography.titleSmall)
                    detail.attachments.forEach { file ->
                        Text(
                            buildString {
                                append(file.name)
                                file.sizeBytes?.let { append(" · ${it / 1024} kB") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
