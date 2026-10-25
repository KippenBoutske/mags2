package nl.cita.magister.platform

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.cita.magister.parse.MagisterJson

interface SettingsStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getLong(key: String, default: Long = 0L): Long =
        getString(key)?.toLongOrNull() ?: default
    fun putLong(key: String, value: Long) = putString(key, value.toString())
    fun getInt(key: String, default: Int = 0): Int =
        getString(key)?.toIntOrNull() ?: default
    fun putInt(key: String, value: Int) = putString(key, value.toString())
    fun getBoolean(key: String, default: Boolean = false): Boolean =
        getString(key)?.toBooleanStrictOrNull() ?: default
    fun putBoolean(key: String, value: Boolean) = putString(key, value.toString())
    fun clear()
    fun flush()
}

class InMemorySettingsStore : SettingsStore {
    private val values = ConcurrentHashMap<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }

    override fun clear() = values.clear()

    override fun flush() = Unit
}

@Serializable
private data class StoreBlob(val values: Map<String, String> = emptyMap())

class FileSettingsStore(
    private val file: File,
    private val secret: SecretKey? = null,
) : SettingsStore {
    private val lock = Any()
    private val values = LinkedHashMap<String, String>()

    init {
        file.parentFile?.mkdirs()
        synchronized(lock) { values.putAll(read()) }
    }

    override fun getString(key: String): String? = synchronized(lock) { values[key] }

    override fun putString(key: String, value: String?) {
        synchronized(lock) {
            if (value == null) values.remove(key) else values[key] = value
            write()
        }
    }

    override fun clear() {
        synchronized(lock) {
            values.clear()
            if (file.exists()) file.delete()
        }
    }

    override fun flush() = synchronized(lock) { write() }

    private fun read(): Map<String, String> = runCatching {
        if (!file.exists()) return emptyMap()
        val raw = file.readBytes()
        val text = if (secret != null) decrypt(raw, secret) else raw.decodeToString()
        MagisterJson.instance.decodeFromString(StoreBlob.serializer(), text).values
    }.getOrDefault(emptyMap())

    private fun write() {
        val text = MagisterJson.instance.encodeToString(StoreBlob(values.toMap()))
        val bytes = if (secret != null) encrypt(text, secret) else text.encodeToByteArray()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(file)) {
            file.writeBytes(bytes)
            tmp.delete()
        }
    }

    companion object {
        fun aesKeyFromBytes(bytes: ByteArray): SecretKey = SecretKeySpec(bytes, "AES")

        fun generateAesKey(): SecretKey {
            val generator = KeyGenerator.getInstance("AES")
            generator.init(256)
            return generator.generateKey()
        }

        fun loadOrCreateKey(file: File): SecretKey {
            file.parentFile?.mkdirs()
            if (file.exists()) {
                return aesKeyFromBytes(file.readBytes())
            }
            val key = generateAesKey()
            file.writeBytes(key.encoded)
            runCatching { file.setReadable(false, false) }
            runCatching { file.setWritable(false, false) }
            runCatching { file.setReadable(true, true) }
            runCatching { file.setWritable(true, true) }
            return key
        }

        private fun encrypt(plain: String, key: SecretKey): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val packed = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            return iv + packed
        }

        private fun decrypt(packed: ByteArray, key: SecretKey): String {
            require(packed.size > 12)
            val iv = packed.copyOfRange(0, 12)
            val bytes = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            return String(cipher.doFinal(bytes), Charsets.UTF_8)
        }
    }
}

expect fun appDataDir(): File

expect fun platformEncryptedStore(name: String): SettingsStore

expect fun platformPlainStore(name: String): SettingsStore
