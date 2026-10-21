package nl.cita.magister.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import nl.cita.magister.api.Tenant

private val Context.dataStore by preferencesDataStore("cita_user")

class UserPrefs(private val context: Context) {
    val tenantHost: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TENANT] ?: Tenant.CITADEL_HOST
    }

    val demoMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEMO] ?: false
    }

    val schoolName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCHOOL] ?: Tenant.CITADEL_DISPLAY
    }

    val webhookUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEBHOOK_URL].orEmpty()
    }

    suspend fun tenantHostNow(): String = tenantHost.first()
    suspend fun isDemo(): Boolean = demoMode.first()
    suspend fun schoolNameNow(): String = schoolName.first()
    suspend fun webhookUrlNow(): String = webhookUrl.first()

    suspend fun setTenant(host: String, schoolName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TENANT] = Tenant.normalize(host)
            prefs[KEY_SCHOOL] = schoolName
        }
    }

    suspend fun setDemo(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEMO] = enabled
        }
    }

    suspend fun setWebhookUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WEBHOOK_URL] = url.trim()
        }
    }

    private companion object {
        val KEY_TENANT = stringPreferencesKey("tenant_host")
        val KEY_SCHOOL = stringPreferencesKey("school_name")
        val KEY_DEMO = booleanPreferencesKey("demo_mode")
        val KEY_WEBHOOK_URL = stringPreferencesKey("plan_goon_webhook_url")
    }
}
