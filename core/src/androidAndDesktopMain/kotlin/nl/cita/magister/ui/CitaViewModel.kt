package nl.cita.magister.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.cita.magister.api.School
import nl.cita.magister.auth.AuthRequest
import nl.cita.magister.auth.AuthRequestFactory
import nl.cita.magister.pairing.LanAddresses
import nl.cita.magister.pairing.PairingClient
import nl.cita.magister.pairing.PairingOffer
import nl.cita.magister.pairing.PairingSession
import nl.cita.magister.pairing.pairingErrorMessage
import nl.cita.magister.pairing.startPairingHost
import nl.cita.magister.pairing.stopPairingHost
import nl.cita.magister.platform.logMags
import nl.cita.magister.data.api.MagisterApiException
import nl.cita.magister.data.repo.MagisterRepository
import nl.cita.magister.di.AppContainer
import nl.cita.magister.fixtures.DemoFixture
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.HapticStrength
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessageDetail
import nl.cita.magister.model.SectionErrors
import nl.cita.magister.platform.platformLiveLessonCancel
import nl.cita.magister.platform.platformLiveLessonSync
import nl.cita.magister.platform.platformLiveLessonTick
import nl.cita.magister.ui.nav.MagsPage
import java.time.LocalDate

class CitaViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val repo: MagisterRepository = container.repository

    val profile = repo.observeProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val grades = repo.observeGrades().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val averages = repo.observeAverages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val latestGrades = repo.observeLatestGrades().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val appointments = repo.observeAppointments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val homework = repo.observeHomework().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val messages = repo.observeMessages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val absences = repo.observeAbsences().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val studyGuides = repo.observeStudyGuides().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val assignments = repo.observeAssignments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tenantHost = container.prefs.tenantHost.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val schoolName = container.prefs.schoolName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val tenantId = container.prefs.tenantId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val webhookUrl = container.prefs.webhookUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val demoMode = container.prefs.demoMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val dynamicColor = container.prefs.dynamicColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val notifications = container.prefs.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val liveUpdate = container.prefs.liveUpdate.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val haptics = container.prefs.haptics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val hapticStrength = container.prefs.hapticStrength.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HapticStrength.Strong,
    )

    private val _signedIn = MutableStateFlow(repo.isLoggedIn() || false)
    val signedIn = _signedIn.asStateFlow()

    private val _booting = MutableStateFlow(true)
    val booting = _booting.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting = _exporting.asStateFlow()

    private val _webViewUrl = MutableStateFlow<String?>(null)
    val webViewUrl = _webViewUrl.asStateFlow()

    private val _pairingOffer = MutableStateFlow<PairingOffer?>(null)
    val pairingOffer = _pairingOffer.asStateFlow()

    private val _pairingScan = MutableStateFlow(false)
    val pairingScan = _pairingScan.asStateFlow()

    private val _pairingBusy = MutableStateFlow(false)
    val pairingBusy = _pairingBusy.asStateFlow()

    private val _pairingError = MutableStateFlow<String?>(null)
    val pairingError = _pairingError.asStateFlow()

    private val _messageDetail = MutableStateFlow<MessageDetail?>(null)
    val messageDetail = _messageDetail.asStateFlow()

    private val _schoolHits = MutableStateFlow<List<School>>(emptyList())
    val schoolHits = _schoolHits.asStateFlow()

    private val _schoolSearching = MutableStateFlow(false)
    val schoolSearching = _schoolSearching.asStateFlow()

    private val _lastError = MutableStateFlow<ApiFailure?>(null)
    val lastError = _lastError.asStateFlow()

    private val _sectionErrors = MutableStateFlow(SectionErrors())
    val sectionErrors = _sectionErrors.asStateFlow()

    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbar = _snackbar.asSharedFlow()

    private val _shareJson = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val shareJson = _shareJson.asSharedFlow()

    private val _hapticPulse = MutableSharedFlow<HapticEvent>(extraBufferCapacity = 8)
    val hapticPulse = _hapticPulse.asSharedFlow()

    private val _clock = MutableStateFlow(AppClock.system())
    val clock = _clock.asStateFlow()
    private var clockPinned = false

    private val _pendingPage = MutableStateFlow<MagsPage?>(null)
    val pendingPage = _pendingPage.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline = _offline.asStateFlow()

    val hasWebhookKey: Boolean get() = repo.webhookConfigured()

    private var searchJob: Job? = null
    @Volatile private var skipBootRefresh = false

    init {
        viewModelScope.launch {
            kotlinx.coroutines.yield()
            if (skipBootRefresh) return@launch
            val demo = repo.isDemo()
            if (demo && !clockPinned) _clock.value = AppClock.Demo
            _signedIn.value = repo.isLoggedIn() || demo
            runCatching { repo.cachedAppointments() }
            _booting.value = false
            if (_signedIn.value) {
                platformLiveLessonSync()
                if (!demo) {
                    runCatching { repo.refreshAll() }
                        .onSuccess {
                            reportWarnings(it.warnings)
                            _offline.value = false
                        }
                        .onFailure {
                            rememberFailure(it)
                            _offline.value = true
                        }
                    platformLiveLessonSync()
                }
            }
        }
    }

    fun searchSchools(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            _schoolHits.value = emptyList()
            _schoolSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(280)
            _schoolSearching.value = true
            _schoolHits.value = runCatching { repo.searchSchools(q) }.getOrDefault(emptyList())
            _schoolSearching.value = false
        }
    }

    fun setSchool(host: String, name: String, tenantId: String? = null) {
        viewModelScope.launch { container.prefs.setTenant(host, name, tenantId) }
    }

    fun switchSchool(host: String, name: String, tenantId: String? = null) {
        viewModelScope.launch {
            repo.logout()
            container.prefs.setTenant(host, name, tenantId)
            _signedIn.value = false
            _lastError.value = null
            _snackbar.emit("Log opnieuw in bij $name.")
        }
    }

    fun startLogin(): AuthRequest {
        val host = tenantHost.value
        val auth = AuthRequestFactory.create(host, tenantId = tenantId.value.ifBlank { null })
        container.pendingAuth.save(auth.codeVerifier, auth.state, host.ifBlank { auth.tenantHost })
        return auth
    }

    fun openWebViewLogin() {
        val request = startLogin()
        _webViewUrl.value = request.authorizeUrl
    }

    fun closeWebView() {
        _webViewUrl.value = null
    }

    fun startPhonePairing() {
        viewModelScope.launch {
            if (LanAddresses.firstLanIpv4() == null) {
                _snackbar.emit("Geen LAN-IP. Zet wifi aan en verbind met hetzelfde netwerk als je telefoon.")
                return@launch
            }
            runCatching {
                startPairingHost { session ->
                    viewModelScope.launch { acceptPairedSession(session) }
                }
            }.onSuccess { offer ->
                _pairingOffer.value = offer
            }.onFailure { error ->
                rememberFailure(error)
                _snackbar.emit(error.message ?: "Kon koppelen niet starten.")
            }
        }
    }

    fun stopPhonePairing() {
        stopPairingHost()
        _pairingOffer.value = null
    }

    fun openPairingScan() {
        _pairingError.value = null
        _pairingScan.value = true
    }

    fun closePairingScan() {
        _pairingScan.value = false
        _pairingBusy.value = false
        _pairingError.value = null
    }

    fun sendPairingToDesktop(offer: PairingOffer) {
        viewModelScope.launch {
            _pairingBusy.value = true
            _pairingError.value = null
            runCatching {
                val session = repo.exportPairingSession()
                PairingClient.send(offer, session)
            }.onSuccess {
                _pairingScan.value = false
                _hapticPulse.tryEmit(HapticEvent.LoginSuccess)
                _snackbar.emit("Desktop gekoppeld.")
            }.onFailure { error ->
                logMags("pairing send failed", error)
                _pairingError.value = pairingErrorMessage(error)
                _hapticPulse.tryEmit(HapticEvent.LoginError)
            }
            _pairingBusy.value = false
        }
    }

    private suspend fun acceptPairedSession(session: PairingSession) {
        _refreshing.value = true
        runCatching { repo.importPairedSession(session) }
            .onSuccess { result ->
                stopPhonePairing()
                _signedIn.value = true
                _hapticPulse.tryEmit(HapticEvent.LoginSuccess)
                val name = result.profile?.firstName ?: session.displayName
                _snackbar.emit(if (name != null) "Gekoppeld als $name." else "Desktop gekoppeld.")
                reportWarnings(result.warnings)
                platformLiveLessonSync()
            }
            .onFailure { error ->
                rememberFailure(error)
                _hapticPulse.tryEmit(HapticEvent.LoginError)
                _snackbar.emit(friendlyOf(error))
            }
        _refreshing.value = false
    }

    fun handleRedirect(url: String) {
        viewModelScope.launch {
            val error = AuthRequestFactory.extractError(url)
            if (error != null) {
                _lastError.value = ApiFailure("oauth", 0, error)
                _hapticPulse.tryEmit(HapticEvent.LoginError)
                _snackbar.emit("Inloggen geannuleerd.")
                return@launch
            }
            val code = AuthRequestFactory.extractCode(url)
            val pending = container.pendingAuth.read()
            if (code == null || pending == null) {
                _lastError.value = ApiFailure("oauth", 0, "Geen autorisatiecode ontvangen.")
                _hapticPulse.tryEmit(HapticEvent.LoginError)
                _snackbar.emit("Inloggen mislukt.")
                return@launch
            }
            val (verifier, _, tenant) = pending
            _refreshing.value = true
            runCatching { repo.completeLogin(code, verifier, tenant) }
                .onSuccess { result ->
                    container.pendingAuth.clear()
                    _webViewUrl.value = null
                    _signedIn.value = true
                    _hapticPulse.tryEmit(HapticEvent.LoginSuccess)
                    val name = result.profile?.firstName
                    if (name != null) {
                        _snackbar.emit("Ingelogd als $name.")
                    } else {
                        _snackbar.emit("Ingelogd.")
                    }
                    reportWarnings(result.warnings)
                    platformLiveLessonSync()
                }
                .onFailure { error ->
                    rememberFailure(error)
                    _hapticPulse.tryEmit(HapticEvent.LoginError)
                    _snackbar.emit(friendlyOf(error))
                }
            _refreshing.value = false
        }
    }

    fun enterDemo(fixture: DemoFixture = DemoFixture.Default, offline: Boolean = false) {
        skipBootRefresh = true
        viewModelScope.launch {
            repo.enterDemo(fixture)
            if (!clockPinned) _clock.value = AppClock.Demo
            _signedIn.value = true
            _lastError.value = null
            _offline.value = offline
            _booting.value = false
            _hapticPulse.tryEmit(HapticEvent.LoginSuccess)
            _snackbar.emit(if (offline) "Offline · cache." else "Voorbeeld.")
            platformLiveLessonSync()
        }
    }

    fun setOffline(value: Boolean) {
        _offline.value = value
    }

    fun setClock(clock: AppClock) {
        clockPinned = true
        _clock.value = clock
    }

    fun openPage(page: MagsPage) {
        _pendingPage.value = page
    }

    fun consumePendingPage() {
        _pendingPage.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            runCatching { repo.refreshAll() }
                .onSuccess {
                    reportWarnings(it.warnings)
                    _offline.value = false
                    _hapticPulse.tryEmit(HapticEvent.RefreshDone)
                    platformLiveLessonSync()
                }
                .onFailure { error ->
                    rememberFailure(error)
                    _offline.value = true
                    _hapticPulse.tryEmit(HapticEvent.RefreshError)
                    _snackbar.emit(friendlyOf(error))
                }
            _refreshing.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            platformLiveLessonCancel()
            stopPhonePairing()
            closePairingScan()
            repo.logout()
            _signedIn.value = false
            _lastError.value = null
            _snackbar.emit("Uitgelogd.")
        }
    }

    fun toggleHomework(item: HomeworkItem, done: Boolean) {
        viewModelScope.launch { repo.markHomeworkDone(item, done) }
    }

    fun openMessage(id: Int) {
        viewModelScope.launch {
            runCatching { repo.messageDetail(id) }
                .onSuccess {
                    _messageDetail.value = it
                    runCatching { repo.markMessageRead(id) }
                }
                .onFailure { error ->
                    rememberFailure(error)
                    _snackbar.emit(friendlyOf(error))
                }
        }
    }

    fun closeMessage() {
        _messageDetail.value = null
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { container.prefs.setDynamicColor(enabled) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { container.prefs.setNotifications(enabled) }
    }

    fun setLiveUpdate(enabled: Boolean) {
        viewModelScope.launch {
            container.prefs.setLiveUpdate(enabled)
            if (enabled) {
                platformLiveLessonTick()
            } else {
                platformLiveLessonCancel()
            }
        }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { container.prefs.setHaptics(enabled) }
    }

    fun setHapticStrength(strength: HapticStrength) {
        viewModelScope.launch { container.prefs.setHapticStrength(strength) }
    }

    fun prefetchAgenda(monday: LocalDate) {
        viewModelScope.launch {
            runCatching { repo.prefetchAgendaAround(monday) }
        }
    }

    fun saveWebhook(url: String, key: String) {
        viewModelScope.launch {
            repo.saveWebhook(url, key.ifBlank { null })
            _snackbar.emit("Opgeslagen.")
        }
    }

    fun exportToPlanGoon() {
        viewModelScope.launch {
            _exporting.value = true
            val url = webhookUrl.value
            if (url.isBlank()) {
                shareExportJson()
            } else {
                repo.exportToPlanGoon()
                    .onSuccess { _snackbar.emit("Verstuurd.") }
                    .onFailure { shareExportJson() }
            }
            _exporting.value = false
        }
    }

    fun shareExportJson() {
        viewModelScope.launch {
            runCatching { repo.exportJson(pretty = true) }
                .onSuccess {
                    _shareJson.emit(it)
                    _snackbar.emit("Gekopieerd.")
                }
                .onFailure { error ->
                    rememberFailure(error)
                    _snackbar.emit(friendlyOf(error))
                }
        }
    }

    private fun reportWarnings(warnings: List<ApiFailure>) {
        _sectionErrors.value = SectionErrors.from(warnings)
        val loginish = warnings.firstOrNull { warning ->
            warning.path.contains("account", ignoreCase = true) ||
                warning.path.contains("oauth", ignoreCase = true)
        }
        _lastError.value = loginish
        loginish?.let { viewModelScope.launch { _snackbar.emit(it.friendly()) } }
    }

    private fun rememberFailure(error: Throwable) {
        _lastError.value = if (error is MagisterApiException) {
            ApiFailure(error.path, error.code, error.body)
        } else {
            ApiFailure("mags", 0, error.message ?: error.toString())
        }
    }

    private fun friendlyOf(error: Throwable): String =
        if (error is MagisterApiException) error.friendly() else "Dat ging niet."
}
