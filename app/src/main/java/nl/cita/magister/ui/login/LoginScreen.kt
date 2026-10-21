package nl.cita.magister.ui.login

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.cita.magister.api.School
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.ui.school.SchoolPicker
import nl.cita.magister.ui.theme.LocalSafeMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreHoriz, contentDescription = "Meer")
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
                .padding(horizontal = 28.dp, vertical = 36.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Mags",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Magister, maar sneller.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                try {
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                } catch (_: ActivityNotFoundException) {
                    onWebViewLogin()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Text("Inloggen", style = MaterialTheme.typography.titleMedium)
        }

        lastError?.let { ErrorDetails(it) }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ErrorDetails(error: ApiFailure) {
    var open by remember(error) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(error.friendly(), style = MaterialTheme.typography.bodyMedium)
        Text(
            error.summary(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.material3.TextButton(onClick = { open = !open }) {
            Text(if (open) "Verberg details" else "Details")
        }
        AnimatedVisibility(open) {
            Text(
                error.details(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BootSplash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (LocalSafeMode.current) {
                androidx.compose.material3.CircularProgressIndicator()
            } else {
                LoadingIndicator(Modifier.size(64.dp))
            }
            Text("Mags", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge)
        }
    }
}
