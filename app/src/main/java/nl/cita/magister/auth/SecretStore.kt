package nl.cita.magister.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Bewaart de Plan Goon webhook-key versleuteld. Nooit loggen. */
class SecretStore(context: Context) {
    private val prefs: SharedPreferences = createSecurePrefs(context)

    fun webhookKey(): String? = prefs.getString(KEY_WEBHOOK, null)?.takeIf { it.isNotBlank() }

    fun hasWebhookKey(): Boolean = !webhookKey().isNullOrBlank()

    fun setWebhookKey(key: String?) {
        prefs.edit().apply {
            val trimmed = key?.trim().orEmpty()
            if (trimmed.isEmpty()) remove(KEY_WEBHOOK) else putString(KEY_WEBHOOK, trimmed)
        }.apply()
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
        } catch (_: Exception) {
            context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    private companion object {
        const val PREFS_NAME = "cita_secrets"
        const val KEY_WEBHOOK = "plan_goon_bearer"
    }
}
