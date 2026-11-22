package nl.cita.magister.desktop.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.cita.magister.parse.MagisterJson
import java.nio.file.Files
import java.nio.file.Path

@Serializable
enum class ThemeMode {
    System,
    Light,
    Dark,
}

@Serializable
data class DesktopPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val tenantHost: String = "",
    val schoolName: String = "",
    val tenantId: String = "",
    val isDemo: Boolean = false,
    val webhookUrl: String = "",
)

class DesktopPrefs(
    private val dataDir: Path,
    private val secretBox: DesktopSecretBox = DesktopSecretBox(dataDir),
) {
    private val prefsFile: Path = dataDir.resolve("prefs.json")
    private val webhookKeyFile: Path = dataDir.resolve("webhook.enc")

    private val _state = MutableStateFlow(load())
    val state: StateFlow<DesktopPreferences> = _state.asStateFlow()

    val current: DesktopPreferences get() = _state.value

    @Synchronized
    private fun load(): DesktopPreferences {
        if (!Files.exists(prefsFile)) return DesktopPreferences()
        return runCatching {
            MagisterJson.instance.decodeFromString(DesktopPreferences.serializer(), Files.readString(prefsFile))
        }.getOrDefault(DesktopPreferences())
    }

    @Synchronized
    private fun update(transform: (DesktopPreferences) -> DesktopPreferences) {
        val next = transform(_state.value)
        _state.value = next
        Files.createDirectories(dataDir)
        Files.writeString(prefsFile, MagisterJson.instance.encodeToString(next))
    }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setDemo(demo: Boolean) = update { it.copy(isDemo = demo) }

    fun setTenant(host: String, schoolName: String, tenantId: String?) = update {
        it.copy(
            tenantHost = host,
            schoolName = schoolName,
            tenantId = tenantId.orEmpty(),
        )
    }

    fun setWebhookUrl(url: String) = update { it.copy(webhookUrl = url.trim()) }

    @Synchronized
    fun webhookKey(): String? {
        if (!Files.exists(webhookKeyFile)) return null
        return secretBox.decrypt(Files.readString(webhookKeyFile))?.takeIf { it.isNotBlank() }
    }

    fun hasWebhookKey(): Boolean = !webhookKey().isNullOrBlank()

    @Synchronized
    fun setWebhookKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            runCatching { Files.deleteIfExists(webhookKeyFile) }
            return
        }
        Files.createDirectories(dataDir)
        Files.writeString(webhookKeyFile, secretBox.encrypt(trimmed))
        secretBox.restrictPermissions(webhookKeyFile)
    }
}
