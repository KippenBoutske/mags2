package nl.cita.magister.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.cita.magister.api.School
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.hapticOnDown
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.platform.openSystemBrowser
import nl.cita.magister.platform.prefersEmbeddedLogin
import nl.cita.magister.ui.components.CookieFlower
import nl.cita.magister.ui.school.SchoolPicker
import nl.cita.magister.ui.theme.LocalSafeMode

@Composable
fun LoginScreen(
    schoolName: String,
    tenantHost: String,
    schools: List<School>,
    searching: Boolean,
    lastError: ApiFailure?,
    onQuery: (String) -> Unit,
    onSchoolChange: (host: String, name: String, tenantId: String?) -> Unit,
    onStartLogin: () -> String,
    onWebViewLogin: () -> Unit,
    onDemo: () -> Unit,
) {
    val focus = LocalFocusManager.current
    var menuOpen by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { focus.clearFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("login-screen"),
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .padding(bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier
                    .defaultMinSize(48.dp, 48.dp)
                    .hapticOnDown(HapticEvent.LongPress)
                    .semantics { contentDescription = "Meer" },
            ) {
                Icon(Icons.Outlined.MoreHoriz, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("In de app") },
                    onClick = {
                        menuOpen = false
                        onWebViewLogin()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Voorbeeld") },
                    onClick = {
                        menuOpen = false
                        onDemo()
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(40.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CookieFlower(
                    size = 92.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Mags",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp,
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
            }
        }

        SchoolPicker(
            selectedName = schoolName,
            selectedHost = tenantHost,
            hits = schools,
            searching = searching,
            onQuery = onQuery,
            onPick = onSchoolChange,
        )

        Button(
            onClick = {
                val url = onStartLogin()
                if (prefersEmbeddedLogin() || !openSystemBrowser(url)) {
                    onWebViewLogin()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics { contentDescription = "Inloggen bij Magister" },
            shape = RoundedCornerShape(28.dp),
            enabled = tenantHost.isNotBlank() || schoolName.isNotBlank(),
        ) {
            Text("Inloggen", style = MaterialTheme.typography.titleMedium)
        }

        lastError?.let { ErrorDetails(it) }
        Spacer(Modifier.height(24.dp))
    }
        TextButton(
            onClick = onDemo,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("login-demo")
                .semantics { contentDescription = "Voorbeeld zonder account" },
        ) {
            Text(
                "Voorbeeld",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ErrorDetails(error: ApiFailure) {
    var open by remember(error) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(error.friendly(), style = MaterialTheme.typography.bodyMedium)
        TextButton(
            onClick = { open = !open },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        ) {
            Text(if (open) "Verberg" else "Details")
        }
        AnimatedVisibility(open) {
            Text(
                "${error.summary()}\n${error.details()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BootSplash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (LocalSafeMode.current) {
                androidx.compose.material3.CircularProgressIndicator()
            } else {
                nl.cita.magister.ui.theme.MagsLoadingIndicator(Modifier.size(64.dp))
            }
            Text("Mags", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge)
        }
    }
}
