package nl.cita.magister.desktop.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import nl.cita.magister.parse.MagisterJson
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple file-backed JSON cache with reactive StateFlow observation per key.
 */
class DesktopCache(private val dataDir: Path) {
    private val cacheDir: Path = dataDir.resolve("cache")
    private val overridesFile: Path = cacheDir.resolve("homework-overrides.json")
    private val flows = ConcurrentHashMap<String, MutableStateFlow<String?>>()
    private val overridesFlow = MutableStateFlow(loadOverrides())

    init {
        Files.createDirectories(cacheDir)
    }

    fun observe(key: String): StateFlow<String?> =
        flows.computeIfAbsent(key) { MutableStateFlow(readFromDisk(it)) }.asStateFlow()

    fun get(key: String): String? =
        flows.computeIfAbsent(key) { MutableStateFlow(readFromDisk(it)) }.value

    @Synchronized
    fun put(key: String, json: String) {
        Files.createDirectories(cacheDir)
        Files.writeString(fileFor(key), json)
        val flow = flows.computeIfAbsent(key) { MutableStateFlow(null) }
        flow.value = json
    }

    fun observeHomeworkOverrides(): StateFlow<Map<Int, Boolean>> = overridesFlow.asStateFlow()

    @Synchronized
    fun setHomeworkDone(homeworkId: Int, done: Boolean) {
        val next = overridesFlow.value.toMutableMap()
        next[homeworkId] = done
        overridesFlow.value = next
        Files.createDirectories(cacheDir)
        val encoded = MagisterJson.instance.encodeToString(
            MapSerializer(Int.serializer(), Boolean.serializer()),
            next,
        )
        Files.writeString(overridesFile, encoded)
    }

    @Synchronized
    fun clear() {
        flows.values.forEach { it.value = null }
        overridesFlow.value = emptyMap()
        if (Files.exists(cacheDir)) {
            Files.list(cacheDir).use { stream ->
                stream.forEach { path -> runCatching { Files.deleteIfExists(path) } }
            }
        }
    }

    private fun fileFor(key: String): Path {
        val safe = key.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return cacheDir.resolve("$safe.json")
    }

    private fun readFromDisk(key: String): String? {
        val file = fileFor(key)
        if (!Files.exists(file)) return null
        return runCatching { Files.readString(file) }.getOrNull()
    }

    private fun loadOverrides(): Map<Int, Boolean> {
        if (!Files.exists(overridesFile)) return emptyMap()
        return runCatching {
            MagisterJson.instance.decodeFromString(
                MapSerializer(Int.serializer(), Boolean.serializer()),
                Files.readString(overridesFile),
            )
        }.getOrDefault(emptyMap())
    }
}
