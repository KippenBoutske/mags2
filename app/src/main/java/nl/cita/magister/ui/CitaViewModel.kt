package nl.cita.magister.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.cita.magister.CitaApp
import nl.cita.magister.api.Tenant
import nl.cita.magister.auth.AuthRequest
import nl.cita.magister.auth.AuthRequestFactory
import nl.cita.magister.data.repo.MagisterRepository
import nl.cita.magister.di.AppContainer
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessageDetail

class CitaViewModel(
    application: Application,
    private val container: AppContainer = (application as CitaApp).container,
) : AndroidViewModel(application) {
    private val repo: MagisterRepository = container.repository

    val profile = repo.observeProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val grades = repo.observeGrades().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val averages = repo.observeAverages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val appointments = repo.observeAppointments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val homework = repo.observeHomework().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val messages = repo.observeMessages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tenantHost = container.prefs.tenantHost.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Tenant.CITADEL_HOST)
    val schoolName = container.prefs.schoolName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Tenant.CITADEL_DISPLAY)
    val webhookUrl = container.prefs.webhookUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val demoMode = container.prefs.demoMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    private val _messageDetail = MutableStateFlow<MessageDetail?>(null)
    val messageDetail = _messageDetail.asStateFlow()

    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbar = _snackbar.asSharedFlow()

    private val _shareJson = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val shareJson = _shareJson.asSharedFlow()

    val hasWebhookKey: Boolean get() = repo.webhookConfigured()

    init {
        viewModelScope.launch {
            val demo = repo.isDemo()
            _signedIn.value = repo.isLoggedIn() || demo
            if (_signedIn.value && profile.value == null) {
                runCatching { repo.refreshAll() }
            }
            _booting.value = false
        }
    }

    fun setSchool(host: String, name: String) {
        viewModelScope.launch { container.prefs.setTenant(host, name) }
    }

    fun startLogin(): AuthRequest {
        val host = tenantHost.value
        val request = AuthRequestFactory.create(host)
        container.pendingAuth.save(request.codeVerifier, request.state, host)
        return request
    }

    fun openWebViewLogin() {
        val request = startLogin()
        _webViewUrl.value = request.authorizeUrl
    }

    fun closeWebView() {
        _webViewUrl.value = null
    }

    fun handleRedirect(url: String) {
        viewModelScope.launch {
            val error = AuthRequestFactory.extractError(url)
            if (error != null) {
                _snackbar.emit("Inloggen geannuleerd: $error")
                return@launch
            }
            val code = AuthRequestFactory.extractCode(url)
            val pending = container.pendingAuth.read()
            if (code == null || pending == null) {
                _snackbar.emit("Geen autorisatiecode ontvangen.")
                return@launch
            }
            val (verifier, _, tenant) = pending
            _refreshing.value = true
            runCatching { repo.completeLogin(code, verifier, tenant) }
                .onSuccess {
                    container.pendingAuth.clear()
                    _webViewUrl.value = null
                    _signedIn.value = true
                    _snackbar.emit("Ingelogd als ${it.firstName}.")
                }
                .onFailure { _snackbar.emit("Inloggen mislukt: ${it.message}") }
            _refreshing.value = false
        }
    }

    fun enterDemo() {
        viewModelScope.launch {
            repo.enterDemo()
            _signedIn.value = true
            _snackbar.emit("Voorbeelddata van 5 VWO Citadel College.")
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            runCatching { repo.refreshAll() }
                .onFailure { _snackbar.emit("Vernieuwen mislukt: ${it.message}") }
            _refreshing.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _signedIn.value = false
            _snackbar.emit("Uitgelogd. Tokens gewist.")
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
                .onFailure { _snackbar.emit("Bericht laden mislukt: ${it.message}") }
        }
    }

    fun closeMessage() {
        _messageDetail.value = null
    }

    fun saveWebhook(url: String, key: String) {
        viewModelScope.launch {
            repo.saveWebhook(url, key.ifBlank { null })
            _snackbar.emit("Plan Goon-webhook opgeslagen.")
        }
    }

    fun exportToPlanGoon() {
        viewModelScope.launch {
            _exporting.value = true
            repo.exportToPlanGoon()
                .onSuccess { _snackbar.emit("Cijfers verstuurd naar Plan Goon.") }
                .onFailure { _snackbar.emit("Export mislukt: ${it.message}") }
            _exporting.value = false
        }
    }

    fun shareExportJson() {
        viewModelScope.launch {
            runCatching { repo.exportJson(pretty = true) }
                .onSuccess {
                    _shareJson.emit(it)
                    _snackbar.emit("JSON gekopieerd naar het klembord.")
                }
                .onFailure { _snackbar.emit("JSON maken mislukt: ${it.message}") }
        }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CitaViewModel(app) as T
                }
            }
    }
}
