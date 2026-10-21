package nl.cita.magister.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import nl.cita.magister.ui.login.BootSplash
import nl.cita.magister.ui.login.LoginScreen
import nl.cita.magister.ui.login.LoginWebView
import nl.cita.magister.ui.messages.MessagesScreen
import nl.cita.magister.ui.schedule.ScheduleScreen
import nl.cita.magister.ui.settings.SettingsScreen
import nl.cita.magister.ui.theme.LocalSafeMode

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
        BootSplash()
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
        val hits by viewModel.schoolHits.collectAsStateWithLifecycle()
        val searching by viewModel.schoolSearching.collectAsStateWithLifecycle()
        val error by viewModel.lastError.collectAsStateWithLifecycle()
        Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) {
            LoginScreen(
                schoolName = school,
                tenantHost = tenant,
                schools = hits,
                searching = searching,
                lastError = error,
                onQuery = viewModel::searchSchools,
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
    val hits by viewModel.schoolHits.collectAsStateWithLifecycle()
    val searching by viewModel.schoolSearching.collectAsStateWithLifecycle()
    val error by viewModel.lastError.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            val items = listOf(
                Tab.Cijfers to Icons.Outlined.Grade,
                Tab.Rooster to Icons.Outlined.CalendarMonth,
                Tab.Huiswerk to Icons.Outlined.Checklist,
                Tab.Berichten to Icons.Outlined.Mail,
                Tab.Meer to Icons.Outlined.MoreHoriz,
            )
            if (LocalSafeMode.current) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    items.forEach { (item, icon) ->
                        TextButton(onClick = { tab = item }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, contentDescription = item.name)
                                Text(item.name)
                            }
                        }
                    }
                }
            } else {
                ShortNavigationBar {
                    items.forEach { (item, icon) ->
                        ShortNavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(item.name) },
                        )
                    }
                }
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
                    schools = hits,
                    searching = searching,
                    lastError = error,
                    contentPadding = padding,
                    onQuery = viewModel::searchSchools,
                    onSwitchSchool = viewModel::switchSchool,
                    onSaveWebhook = viewModel::saveWebhook,
                    onExport = viewModel::exportToPlanGoon,
                    onShareJson = viewModel::shareExportJson,
                    onLogout = viewModel::logout,
                )
            }
        }
    }
}
