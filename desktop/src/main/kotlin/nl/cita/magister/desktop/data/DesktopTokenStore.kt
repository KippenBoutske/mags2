package nl.cita.magister.desktop.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.cita.magister.auth.StoredSession
import nl.cita.magister.auth.TokenResponse
import nl.cita.magister.parse.MagisterJson
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts sensitive desktop data (OAuth tokens, webhook keys) with AES-256-GCM
 * using a per-user key file in the Mags app data folder.
 */
class DesktopSecretBox(private val dataDir: Path) {
    private val keyFile: Path = dataDir.resolve(".mags.key")
    private val random = SecureRandom()

    @Synchronized
    private fun loadOrCreateKey(): SecretKey {
        Files.createDirectories(dataDir)
        if (Files.exists(keyFile)) {
            val encoded = Files.readString(keyFile).trim()
            val bytes = Base64.getDecoder().decode(encoded)
            return SecretKeySpec(bytes, "AES")
        }
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, random)
        val key = keyGen.generateKey()
        Files.writeString(keyFile, Base64.getEncoder().encodeToString(key.encoded))
        restrictPermissions(keyFile)
        return key
    }

    fun encrypt(plainText: String): String {
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, loadOrCreateKey(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(cipherText: String): String? = runCatching {
        val combined = Base64.getDecoder().decode(cipherText.trim())
        if (combined.size <= 12) return null
        val iv = combined.copyOfRange(0, 12)
        val payload = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, loadOrCreateKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(payload), Charsets.UTF_8)
    }.getOrNull()

    fun restrictPermissions(path: Path) {
        runCatching {
            Files.setPosixFilePermissions(
                path,
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            )
        }
    }
}

@Serializable
private data class StoredSessionRecord(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null,
    val expiresAtEpochMs: Long = 0L,
    val tenantHost: String = "",
    val personId: Int? = null,
    val enrollmentId: Int? = null,
    val displayName: String? = null,
) {
    fun toSession(): StoredSession = StoredSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        idToken = idToken,
        expiresAtEpochMs = expiresAtEpochMs,
        tenantHost = tenantHost,
        personId = personId,
        enrollmentId = enrollmentId,
        displayName = displayName,
    )

    companion object {
        fun from(session: StoredSession): StoredSessionRecord = StoredSessionRecord(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            idToken = session.idToken,
            expiresAtEpochMs = session.expiresAtEpochMs,
            tenantHost = session.tenantHost,
            personId = session.personId,
            enrollmentId = session.enrollmentId,
            displayName = session.displayName,
        )
    }
}

class DesktopTokenStore(
    private val dataDir: Path,
    private val secretBox: DesktopSecretBox = DesktopSecretBox(dataDir),
) {
    private val sessionFile: Path = dataDir.resolve("session.enc")

    @Synchronized
    fun read(): StoredSession? {
        if (!Files.exists(sessionFile)) return null
        val decrypted = secretBox.decrypt(Files.readString(sessionFile)) ?: return null
        return runCatching {
            MagisterJson.instance
                .decodeFromString(StoredSessionRecord.serializer(), decrypted)
                .toSession()
        }.getOrNull()
    }

    @Synchronized
    fun write(session: StoredSession) {
        Files.createDirectories(dataDir)
        val json = MagisterJson.instance.encodeToString(
            StoredSessionRecord.serializer(),
            StoredSessionRecord.from(session),
        )
        Files.writeString(sessionFile, secretBox.encrypt(json))
        secretBox.restrictPermissions(sessionFile)
    }

    @Synchronized
    fun updateTokens(tokens: TokenResponse, existing: StoredSession) {
        write(
            existing.copy(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken ?: existing.refreshToken,
                idToken = tokens.idToken ?: existing.idToken,
                expiresAtEpochMs = System.currentTimeMillis() + tokens.expiresIn * 1000L,
            ),
        )
    }

    @Synchronized
    fun expiringSoon(withinMs: Long = 90_000L): Boolean {
        val session = read() ?: return false
        if (session.refreshToken.isNullOrBlank()) return false
        val expires = session.expiresAtEpochMs
        return expires > 0L && expires - System.currentTimeMillis() < withinMs
    }

    @Synchronized
    fun clear() {
        runCatching { Files.deleteIfExists(sessionFile) }
    }
}

@Serializable
data class PendingAuth(
    val verifier: String,
    val state: String,
    val tenantHost: String,
    val schoolName: String = "",
)

class DesktopPendingAuthStore(private val dataDir: Path) {
    private val file: Path = dataDir.resolve("pending-auth.json")

    @Synchronized
    fun save(pending: PendingAuth) {
        Files.createDirectories(dataDir)
        Files.writeString(file, MagisterJson.instance.encodeToString(pending))
    }

    @Synchronized
    fun read(): PendingAuth? {
        if (!Files.exists(file)) return null
        return runCatching {
            MagisterJson.instance.decodeFromString(PendingAuth.serializer(), Files.readString(file))
        }.getOrNull()
    }

    @Synchronized
    fun clear() {
        runCatching { Files.deleteIfExists(file) }
    }
}
