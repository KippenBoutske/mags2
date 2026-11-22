package nl.cita.magister.desktop.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.cita.magister.api.KnownSchools
import nl.cita.magister.api.School
import nl.cita.magister.auth.AuthRequest
import nl.cita.magister.auth.AuthRequestFactory
import nl.cita.magister.desktop.data.DesktopAuthCallback
import nl.cita.magister.desktop.data.DesktopPendingAuthStore
import nl.cita.magister.desktop.data.DesktopPreferences
import nl.cita.magister.desktop.data.DesktopRepository
import nl.cita.magister.desktop.data.PendingAuth
import nl.cita.magister.desktop.data.ThemeMode
import nl.cita.magister.model.Absence
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessageDetail
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.StudyGuide
import nl.cita.magister.model.SubjectAverage
import java.time.LocalDate

enum class DesktopPage(val label: String) {
    Vandaag("Vandaag"),
    Rooster("Rooster"),
    Cijfers("Cijfers"),
    Huiswerk("Huiswerk"),
    Berichten("Berichten"),
    Meer("Meer"),
}

class MagsDesktopController(
    val repo: DesktopRepository,
    private val pendingAuthStore: DesktopPendingAuthStore,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val prefs: StateFlow<DesktopPreferences> = repo.prefs.state
    val profile: StateFlow<StudentProfile?> = repo.observeProfile()
        .stateIn(scope, SharingStarted.Eagerly, null)
    val grades: StateFlow<List<Grade>> = repo.observeGrades()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val averages: StateFlow<List<SubjectAverage>> = repo.observeAverages()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val latestGrades: StateFlow<List<Grade>> = repo.observeLatestGrades()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val appointments: StateFlow<List<Appointment>> = repo.observeAppointments()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val homework: StateFlow<List<HomeworkItem>> = repo.observeHomework()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val messages: StateFlow<List<MessagePreview>> = repo.observeMessages()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val absences: StateFlow<List<Absence>> = repo.observeAbsences()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val studyGuides: StateFlow<List<StudyGuide>> = repo.observeStudyGuides()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val assignments: StateFlow<List<HomeworkItem>> = repo.observeAssignments()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _authenticated = MutableStateFlow(repo.isLoggedIn() || repo.isDemo())
    val authenticated: StateFlow<Boolean> = _authenticated.asStateFlow()

    private val _page = MutableStateFlow(DesktopPage.Vandaag)
    val page: StateFlow<DesktopPage> = _page.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _warnings = MutableStateFlow<List<ApiFailure>>(emptyList())
    val warnings: StateFlow<List<ApiFailure>> = _warnings.asStateFlow()

    private val _activeAuth = MutableStateFlow<AuthRequest?>(null)
    val activeAuth: StateFlow<AuthRequest?> = _activeAuth.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _schoolResults = MutableStateFlow(KnownSchools.suggestions)
    val schoolResults: StateFlow<List<School>> = _schoolResults.asStateFlow()

    private val _searchingSchools = MutableStateFlow(false)
    val searchingSchools: StateFlow<Boolean> = _searchingSchools.asStateFlow()

    private val _selectedMessageDetail = MutableStateFlow<MessageDetail?>(null)
    val selectedMessageDetail: StateFlow<MessageDetail?> = _selectedMessageDetail.asStateFlow()

    private val _loadingMessage = MutableStateFlow(false)
    val loadingMessage: StateFlow<Boolean> = _loadingMessage.asStateFlow()

    init {
        if (_authenticated.value) {
            refreshAll(silent = true)
        }
    }

    fun navigate(target: DesktopPage) {
        _page.value = target
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun showStatus(message: String) {
        _statusMessage.value = message
    }

    fun setThemeMode(mode: ThemeMode) {
        repo.prefs.setThemeMode(mode)
    }

    fun searchSchools(query: String) {
        scope.launch {
            _searchingSchools.value = true
            _schoolResults.value = repo.searchSchools(query)
            _searchingSchools.value = false
        }
    }

    fun startInAppLogin(school: School) {
        _loginError.value = null
        repo.prefs.setTenant(school.host, school.name, school.tenantId)
        val req = AuthRequestFactory.create(
            tenantHost = school.host,
            tenantId = school.tenantId,
        )
        _activeAuth.value = req
        pendingAuthStore.save(
            PendingAuth(
                verifier = req.codeVerifier,
                state = req.state,
                tenantHost = req.tenantHost,
                schoolName = school.name,
            ),
        )
        nl.cita.magister.desktop.ui.screens.InAppLoginWindow.open(
            authorizeUrl = req.authorizeUrl,
            title = "Inloggen — ${school.name}",
            onRedirectUrl = { url -> handleRedirectUrl(url) },
            onError = { msg -> _loginError.value = msg },
        )
    }

    fun startBrowserLogin(school: School) {
        _loginError.value = null
        DesktopAuthCallback.ensureWindowsProtocolRegistered()
        repo.prefs.setTenant(school.host, school.name, school.tenantId)
        val req = AuthRequestFactory.create(
            tenantHost = school.host,
            tenantId = school.tenantId,
        )
        _activeAuth.value = req
        pendingAuthStore.save(
            PendingAuth(
                verifier = req.codeVerifier,
                state = req.state,
                tenantHost = req.tenantHost,
                schoolName = school.name,
            ),
        )
        val opened = DesktopAuthCallback.openInBrowser(req.authorizeUrl)
        if (!opened) {
            _loginError.value = "Kon browser niet automatisch openen. Kopieer de inloglink hieronder."
        }
    }

    fun handleRedirectUrl(rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return
        val error = AuthRequestFactory.extractError(trimmed)
        if (error != null) {
            _loginError.value = "Magister gaf een fout terug: $error"
            return
        }
        val code = AuthRequestFactory.extractCode(trimmed)
        if (code.isNullOrBlank()) {
            _loginError.value = if (trimmed.contains("login.microsoftonline.com", ignoreCase = true)) {
                "Dit is nog de Microsoft-tussenpagina, niet de Magister-redirect. " +
                    "Klik in je browser op 'Open Mags' zodra de pop-up verschijnt, of kopieer de m6loapp://oauth2redirect/... link uit F12 → Netwerk."
            } else {
                "Geen autorisatiecode gevonden in deze URL. Plak de volledige m6loapp://oauth2redirect/... link."
            }
            return
        }
        val active = _activeAuth.value
        val pending = pendingAuthStore.read()
        val verifier = active?.codeVerifier ?: pending?.verifier
        val tenantHost = active?.tenantHost ?: pending?.tenantHost ?: repo.prefs.current.tenantHost
        if (verifier.isNullOrBlank()) {
            _loginError.value = "Geen actieve inlogsessie gevonden. Klik eerst opnieuw op 'Inloggen in browser'."
            return
        }
        scope.launch {
            _refreshing.value = true
            _loginError.value = null
            runCatching {
                repo.completeLogin(code, verifier, tenantHost)
            }.onSuccess { hydration ->
                pendingAuthStore.clear()
                _activeAuth.value = null
                _authenticated.value = true
                _warnings.value = hydration.warnings
                _page.value = DesktopPage.Vandaag
                showStatus("Ingelogd als ${hydration.profile?.firstName ?: "leerling"}")
            }.onFailure { err ->
                _loginError.value = err.message ?: "Inloggen mislukt."
            }
            _refreshing.value = false
        }
    }

    fun enterDemo() {
        _loginError.value = null
        _activeAuth.value = null
        pendingAuthStore.clear()
        repo.enterDemo()
        _authenticated.value = true
        _warnings.value = emptyList()
        _page.value = DesktopPage.Vandaag
        showStatus("Voorbeeldmodus geladen (Citadel College)")
    }

    fun logout() {
        repo.logout()
        pendingAuthStore.clear()
        _activeAuth.value = null
        _selectedMessageDetail.value = null
        _warnings.value = emptyList()
        _authenticated.value = false
        _page.value = DesktopPage.Vandaag
    }

    fun refreshAll(silent: Boolean = false) {
        scope.launch {
            _refreshing.value = true
            runCatching { repo.refreshAll() }
                .onSuccess { hydration ->
                    _warnings.value = hydration.warnings
                    if (!silent) {
                        val suffix = if (hydration.warnings.isEmpty()) "" else " (${hydration.warnings.size} meldingen)"
                        showStatus("Bijgewerkt$suffix")
                    }
                }
                .onFailure { err ->
                    if (!silent) showStatus("Kon niet verversen: ${err.message}")
                }
            _refreshing.value = false
        }
    }

    fun prefetchWeek(monday: LocalDate) {
        scope.launch {
            runCatching { repo.prefetchAgendaAround(monday) }
        }
    }

    fun toggleHomework(item: HomeworkItem, done: Boolean) {
        scope.launch {
            repo.markHomeworkDone(item, done)
            showStatus(if (done) "Afgevinkt: ${item.title}" else "Weer op open gezet")
        }
    }

    fun openMessage(preview: MessagePreview) {
        scope.launch {
            _loadingMessage.value = true
            runCatching { repo.messageDetail(preview.id) }
                .onSuccess { detail ->
                    _selectedMessageDetail.value = detail
                    if (!preview.isRead && !repo.isDemo()) {
                        runCatching { repo.markMessageRead(preview.id, true) }
                    }
                }
                .onFailure { err ->
                    showStatus("Kon bericht niet openen: ${err.message}")
                }
            _loadingMessage.value = false
        }
    }

    fun saveWebhook(url: String, key: String?) {
        repo.prefs.setWebhookUrl(url)
        if (key != null) repo.prefs.setWebhookKey(key)
        showStatus("Export-instellingen opgeslagen")
    }

    fun exportToPlanGoon() {
        scope.launch {
            val result = repo.exportToPlanGoon()
            result.onSuccess { code ->
                showStatus("Cijfers verstuurd (HTTP $code)")
            }.onFailure { err ->
                showStatus(
                    if (err.message == "geen-adres") {
                        "Stel eerst een export-URL in onder Meer → Geavanceerd"
                    } else {
                        "Export mislukt: ${err.message}"
                    },
                )
            }
        }
    }

    fun registerWindowsProtocol() {
        val result = DesktopAuthCallback.registerWindowsProtocol()
        result.onSuccess { showStatus(it) }
            .onFailure { showStatus(it.message ?: "Registratie mislukt") }
    }

    fun dispose() {
        scope.cancel()
    }
}
