package nl.cita.magister.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.cita.magister.api.Tenant

/**
 * Gewone SharedPreferences — geen DataStore-native (`libdatastore_shared_counter.so`)
 * op het startpad. Alleen niet-geheime voorkeuren (tenant, demo, webhook-URL).
 */
class UserPrefs(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _tenantHost = MutableStateFlow(prefs.getString(KEY_TENANT, null).orEmpty())
    val tenantHost: Flow<String> = _tenantHost.asStateFlow()

    private val _demoMode = MutableStateFlow(prefs.getBoolean(KEY_DEMO, false))
    val demoMode: Flow<Boolean> = _demoMode.asStateFlow()

    private val _schoolName = MutableStateFlow(prefs.getString(KEY_SCHOOL, null).orEmpty())
    val schoolName: Flow<String> = _schoolName.asStateFlow()

    private val _tenantId = MutableStateFlow(prefs.getString(KEY_TENANT_ID, null).orEmpty())
    val tenantId: Flow<String> = _tenantId.asStateFlow()

    private val _webhookUrl = MutableStateFlow(prefs.getString(KEY_WEBHOOK_URL, null).orEmpty())
    val webhookUrl: Flow<String> = _webhookUrl.asStateFlow()

    suspend fun tenantHostNow(): String = _tenantHost.value
    suspend fun isDemo(): Boolean = _demoMode.value
    suspend fun schoolNameNow(): String = _schoolName.value
    suspend fun tenantIdNow(): String = _tenantId.value
    suspend fun webhookUrlNow(): String = _webhookUrl.value

    suspend fun setTenant(host: String, schoolName: String, tenantId: String? = null) {
        val normalized = Tenant.normalize(host)
        val id = tenantId?.trim().orEmpty()
        prefs.edit()
            .putString(KEY_TENANT, normalized)
            .putString(KEY_SCHOOL, schoolName)
            .putString(KEY_TENANT_ID, id)
            .apply()
        _tenantHost.value = normalized
        _schoolName.value = schoolName
        _tenantId.value = id
    }

    suspend fun setDemo(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO, enabled).apply()
        _demoMode.value = enabled
    }

    suspend fun setWebhookUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString(KEY_WEBHOOK_URL, trimmed).apply()
        _webhookUrl.value = trimmed
    }

    private companion object {
        const val PREFS_NAME = "mags_user"
        const val KEY_TENANT = "tenant_host"
        const val KEY_SCHOOL = "school_name"
        const val KEY_TENANT_ID = "tenant_id"
        const val KEY_DEMO = "demo_mode"
        const val KEY_WEBHOOK_URL = "plan_goon_webhook_url"
    }
}
