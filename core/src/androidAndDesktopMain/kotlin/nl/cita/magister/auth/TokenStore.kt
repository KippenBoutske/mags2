package nl.cita.magister.auth

import nl.cita.magister.platform.SettingsStore

class TokenStore(private val prefs: SettingsStore) {
    @Synchronized
    fun read(): StoredSession? {
        val access = prefs.getString(KEY_ACCESS) ?: return null
        val tenant = prefs.getString(KEY_TENANT).orEmpty()
        return StoredSession(
            accessToken = access,
            refreshToken = prefs.getString(KEY_REFRESH),
            idToken = prefs.getString(KEY_ID),
            expiresAtEpochMs = prefs.getLong(KEY_EXPIRES, 0L),
            tenantHost = tenant,
            personId = prefs.getInt(KEY_PERSON, -1).takeIf { it > 0 },
            enrollmentId = prefs.getInt(KEY_ENROLLMENT, -1).takeIf { it > 0 },
            displayName = prefs.getString(KEY_NAME),
        )
    }

    @Synchronized
    fun write(session: StoredSession) {
        prefs.putString(KEY_ACCESS, session.accessToken)
        prefs.putString(KEY_REFRESH, session.refreshToken)
        prefs.putString(KEY_ID, session.idToken)
        prefs.putLong(KEY_EXPIRES, session.expiresAtEpochMs)
        prefs.putString(KEY_TENANT, session.tenantHost)
        prefs.putInt(KEY_PERSON, session.personId ?: -1)
        prefs.putInt(KEY_ENROLLMENT, session.enrollmentId ?: -1)
        prefs.putString(KEY_NAME, session.displayName)
        prefs.flush()
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
        prefs.clear()
    }

    private companion object {
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

class PendingAuthStore(private val prefs: SettingsStore) {
    fun save(verifier: String, state: String, tenantHost: String) {
        prefs.putString("verifier", verifier)
        prefs.putString("state", state)
        prefs.putString("tenant", tenantHost)
        prefs.flush()
    }

    fun read(): Triple<String, String, String>? {
        val verifier = prefs.getString("verifier") ?: return null
        val state = prefs.getString("state") ?: return null
        val tenant = prefs.getString("tenant").orEmpty()
        return Triple(verifier, state, tenant)
    }

    fun clear() = prefs.clear()
}

class SecretStore(private val prefs: SettingsStore) {
    fun webhookKey(): String? = prefs.getString(KEY_WEBHOOK)?.takeIf { it.isNotBlank() }

    fun hasWebhookKey(): Boolean = !webhookKey().isNullOrBlank()

    fun setWebhookKey(key: String?) {
        val trimmed = key?.trim().orEmpty()
        prefs.putString(KEY_WEBHOOK, trimmed.ifEmpty { null })
        prefs.flush()
    }

    private companion object {
        const val KEY_WEBHOOK = "plan_goon_bearer"
    }
}
