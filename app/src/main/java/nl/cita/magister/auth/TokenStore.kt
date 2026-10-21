package nl.cita.magister.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import nl.cita.magister.auth.StoredSession as Session

class TokenStore(context: Context) {
    private val prefs: SharedPreferences = createSecurePrefs(context)

    @Synchronized
    fun read(): Session? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        val tenant = prefs.getString(KEY_TENANT, null) ?: return null
        return Session(
            accessToken = access,
            refreshToken = prefs.getString(KEY_REFRESH, null),
            idToken = prefs.getString(KEY_ID, null),
            expiresAtEpochMs = prefs.getLong(KEY_EXPIRES, 0L),
            tenantHost = tenant,
            personId = prefs.getInt(KEY_PERSON, -1).takeIf { it > 0 },
            enrollmentId = prefs.getInt(KEY_ENROLLMENT, -1).takeIf { it > 0 },
            displayName = prefs.getString(KEY_NAME, null),
        )
    }

    @Synchronized
    fun write(session: Session) {
        prefs.edit()
            .putString(KEY_ACCESS, session.accessToken)
            .putString(KEY_REFRESH, session.refreshToken)
            .putString(KEY_ID, session.idToken)
            .putLong(KEY_EXPIRES, session.expiresAtEpochMs)
            .putString(KEY_TENANT, session.tenantHost)
            .putInt(KEY_PERSON, session.personId ?: -1)
            .putInt(KEY_ENROLLMENT, session.enrollmentId ?: -1)
            .putString(KEY_NAME, session.displayName)
            .apply()
    }

    @Synchronized
    fun updateTokens(tokens: TokenResponse, existing: Session) {
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
    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun createSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (error: Exception) {
            // Emulator/CI zonder lockscreen: val terug op gewone prefs i.p.v. crashen.
            context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    private companion object {
        const val PREFS_NAME = "cita_tokens"
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_ID = "id"
        const val KEY_EXPIRES = "expires"
        const val KEY_TENANT = "tenant"
        const val KEY_PERSON = "person"
        const val KEY_ENROLLMENT = "enrollment"
        const val KEY_NAME = "name"
    }
}

class PendingAuthStore(context: Context) {
    private val prefs = context.getSharedPreferences("cita_pending_auth", Context.MODE_PRIVATE)

    fun save(verifier: String, state: String, tenantHost: String) {
        prefs.edit()
            .putString("verifier", verifier)
            .putString("state", state)
            .putString("tenant", tenantHost)
            .apply()
    }

    fun read(): Triple<String, String, String>? {
        val verifier = prefs.getString("verifier", null) ?: return null
        val state = prefs.getString("state", null) ?: return null
        val tenant = prefs.getString("tenant", null) ?: return null
        return Triple(verifier, state, tenant)
    }

    fun clear() = prefs.edit().clear().apply()
}
