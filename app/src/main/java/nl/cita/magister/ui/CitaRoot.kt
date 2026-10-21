package nl.cita.magister.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.cita.magister.ui.grades.GradesScreen
import nl.cita.magister.ui.homework.HomeworkScreen
import nl.cita.magister.ui.login.LoginScreen
import nl.cita.magister.ui.login.LoginWebView
import nl.cita.magister.ui.messages.MessagesScreen
import nl.cita.magister.ui.schedule.ScheduleScreen
import nl.cita.magister.ui.settings.SettingsScreen

private enum class Tab { Cijfers, Rooster, Huiswerk, Berichten, Meer }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CitaRoot(viewModel: CitaViewModel) {
    val booting by viewModel.booting.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val webViewUrl by viewModel.webViewUrl.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.snackbar.collect { snackbarHost.showSnackbar(it) }
    }

    if (booting) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        return
    }

    if (webViewUrl != null) {
        LoginWebView(
            url = webViewUrl!!,
            onRedirect = viewModel::handleRedirect,
            onClose = viewModel::closeWebView,
        )
        return
    }

    if (!signedIn) {
        val school by viewModel.schoolName.collectAsStateWithLifecycle()
        val tenant by viewModel.tenantHost.collectAsStateWithLifecycle()
        Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) {
                LoginScreen(
                    schoolName = school,
                    tenantHost = tenant,
                    onSchoolChange = viewModel::setSchool,
                    onStartLogin = { viewModel.startLogin().authorizeUrl },
                    onWebViewLogin = viewModel::openWebViewLogin,
                    onDemo = viewModel::enterDemo,
                )
        }
        return
    }

    SignedInShell(viewModel, snackbarHost)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SignedInShell(viewModel: CitaViewModel, snackbarHost: SnackbarHostState) {
    var tab by remember { mutableStateOf(Tab.Cijfers) }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val grades by viewModel.grades.collectAsStateWithLifecycle()
    val averages by viewModel.averages.collectAsStateWithLifecycle()
    val appointments by viewModel.appointments.collectAsStateWithLifecycle()
    val homework by viewModel.homework.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val detail by viewModel.messageDetail.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val tenant by viewModel.tenantHost.collectAsStateWithLifecycle()
    val school by viewModel.schoolName.collectAsStateWithLifecycle()
    val webhook by viewModel.webhookUrl.collectAsStateWithLifecycle()
    val demo by viewModel.demoMode.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            ShortNavigationBar {
                ShortNavigationBarItem(
                    selected = tab == Tab.Cijfers,
                    onClick = { tab = Tab.Cijfers },
                    icon = { Icon(Icons.Outlined.Grade, contentDescription = null) },
                    label = { Text("Cijfers") },
                )
                ShortNavigationBarItem(
                    selected = tab == Tab.Rooster,
                    onClick = { tab = Tab.Rooster },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    label = { Text("Rooster") },
                )
                ShortNavigationBarItem(
                    selected = tab == Tab.Huiswerk,
                    onClick = { tab = Tab.Huiswerk },
                    icon = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
                    label = { Text("Huiswerk") },
                )
                ShortNavigationBarItem(
                    selected = tab == Tab.Berichten,
                    onClick = { tab = Tab.Berichten },
                    icon = { Icon(Icons.Outlined.Mail, contentDescription = null) },
                    label = { Text("Berichten") },
                )
                ShortNavigationBarItem(
                    selected = tab == Tab.Meer,
                    onClick = { tab = Tab.Meer },
                    icon = { Icon(Icons.Outlined.MoreHoriz, contentDescription = null) },
                    label = { Text("Meer") },
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = viewModel::refresh) {
            when (tab) {
                Tab.Cijfers -> GradesScreen(
                    profile = profile,
                    averages = averages,
                    grades = grades,
                    refreshing = refreshing,
                    exporting = exporting,
                    contentPadding = padding,
                    onExport = viewModel::exportToPlanGoon,
                    onShareJson = viewModel::shareExportJson,
                )
                Tab.Rooster -> ScheduleScreen(appointments, padding)
                Tab.Huiswerk -> HomeworkScreen(homework, padding, viewModel::toggleHomework)
                Tab.Berichten -> MessagesScreen(messages, detail, padding, viewModel::openMessage, viewModel::closeMessage)
                Tab.Meer -> SettingsScreen(
                    profile = profile,
                    tenantHost = tenant,
                    schoolName = school,
                    webhookUrl = webhook,
                    hasWebhookKey = viewModel.hasWebhookKey,
                    demoMode = demo,
                    contentPadding = padding,
                    onSaveWebhook = viewModel::saveWebhook,
                    onExport = viewModel::exportToPlanGoon,
                    onShareJson = viewModel::shareExportJson,
                    onLogout = viewModel::logout,
                )
            }
        }
    }
}
