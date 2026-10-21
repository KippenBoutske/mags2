package nl.cita.magister.auth

import android.content.Context
import android.content.SharedPreferences

/** Bewaart de Plan Goon webhook-key versleuteld. Nooit loggen. */
class SecretStore(context: Context) {
    private val prefs: SharedPreferences = SecurePrefs.open(context, PREFS_NAME)

    fun webhookKey(): String? = prefs.getString(KEY_WEBHOOK, null)?.takeIf { it.isNotBlank() }

    fun hasWebhookKey(): Boolean = !webhookKey().isNullOrBlank()

    fun setWebhookKey(key: String?) {
        prefs.edit().apply {
            val trimmed = key?.trim().orEmpty()
            if (trimmed.isEmpty()) remove(KEY_WEBHOOK) else putString(KEY_WEBHOOK, trimmed)
        }.apply()
    }

    private companion object {
        const val PREFS_NAME = "mags_secrets"
        const val KEY_WEBHOOK = "plan_goon_bearer"
    }
}
