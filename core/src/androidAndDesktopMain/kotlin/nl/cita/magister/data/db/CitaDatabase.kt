package nl.cita.magister.data.db

import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.cita.magister.parse.MagisterJson

/**
 * Offline cache. Zelfde rol als Room (JSON-blobs + huiswerk-vinkjes),
 * zonder KSP: eenvoudige JSON-bestanden in [appDataDir]/offline.
 */
@Serializable
data class CacheBlob(
    val key: String,
    val json: String,
    val updatedAt: Long,
)

@Serializable
data class LocalHomeworkState(
    val homeworkId: Int,
    val done: Boolean,
)

class CacheDao(private val store: JsonOfflineStore) {
    fun observe(key: String): Flow<CacheBlob?> = store.blobs.map { it[key] }
    suspend fun get(key: String): CacheBlob? = store.blobs.value[key]
    suspend fun put(blob: CacheBlob) = store.putBlob(blob)
    suspend fun clear() = store.clearBlobs()
}

class HomeworkDao(private val store: JsonOfflineStore) {
    fun observeAll(): Flow<List<LocalHomeworkState>> = store.homework.map { it.values.toList() }
    suspend fun upsert(state: LocalHomeworkState) = store.upsertHomework(state)
}

class CitaDatabase internal constructor(private val store: JsonOfflineStore) {
    fun cacheDao(): CacheDao = CacheDao(store)
    fun homeworkDao(): HomeworkDao = HomeworkDao(store)

    companion object {
        fun create(dataDir: File): CitaDatabase =
            CitaDatabase(JsonOfflineStore(File(dataDir, "offline")))
    }
}

class JsonOfflineStore(dir: File) {
    private val folder = dir.apply { mkdirs() }
    private val blobFile = File(folder, "blobs.json")
    private val homeworkFile = File(folder, "homework.json")

    private val _blobs = MutableStateFlow(readBlobs())
    val blobs = _blobs.asStateFlow()

    private val _homework = MutableStateFlow(readHomework())
    val homework = _homework.asStateFlow()

    @Synchronized
    fun putBlob(blob: CacheBlob) {
        _blobs.update { it + (blob.key to blob) }
        writeBlobs(_blobs.value)
    }

    @Synchronized
    fun clearBlobs() {
        _blobs.value = emptyMap()
        if (blobFile.exists()) blobFile.delete()
    }

    @Synchronized
    fun upsertHomework(state: LocalHomeworkState) {
        _homework.update { it + (state.homeworkId to state) }
        writeHomework(_homework.value)
    }

    private fun readBlobs(): Map<String, CacheBlob> = runCatching {
        if (!blobFile.exists()) return emptyMap()
        MagisterJson.instance.decodeFromString<Map<String, CacheBlob>>(blobFile.readText())
    }.getOrDefault(emptyMap())

    private fun readHomework(): Map<Int, LocalHomeworkState> = runCatching {
        if (!homeworkFile.exists()) return emptyMap()
        MagisterJson.instance.decodeFromString<Map<Int, LocalHomeworkState>>(homeworkFile.readText())
    }.getOrDefault(emptyMap())

    private fun writeBlobs(value: Map<String, CacheBlob>) {
        blobFile.writeText(MagisterJson.instance.encodeToString(value))
    }

    private fun writeHomework(value: Map<Int, LocalHomeworkState>) {
        homeworkFile.writeText(MagisterJson.instance.encodeToString(value))
    }
}
