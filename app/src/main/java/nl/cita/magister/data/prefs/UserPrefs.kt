package nl.cita.magister.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.cita.magister.api.Tenant
import nl.cita.magister.haptics.HapticStrength

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

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, false))
    val dynamicColor: Flow<Boolean> = _dynamicColor.asStateFlow()

    private val _notifications = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY, true))
    val notifications: Flow<Boolean> = _notifications.asStateFlow()

    private val _liveUpdate = MutableStateFlow(prefs.getBoolean(KEY_LIVE, true))
    val liveUpdate: Flow<Boolean> = _liveUpdate.asStateFlow()

    private val _haptics = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    val haptics: Flow<Boolean> = _haptics.asStateFlow()

    private val _hapticStrength = MutableStateFlow(
        HapticStrength.fromId(prefs.getString(KEY_HAPTIC_STRENGTH, HapticStrength.Strong.id)),
    )
    val hapticStrength: Flow<HapticStrength> = _hapticStrength.asStateFlow()

    suspend fun tenantHostNow(): String = _tenantHost.value
    suspend fun isDemo(): Boolean = _demoMode.value
    suspend fun schoolNameNow(): String = _schoolName.value
    suspend fun tenantIdNow(): String = _tenantId.value
    suspend fun webhookUrlNow(): String = _webhookUrl.value
    suspend fun dynamicColorNow(): Boolean = _dynamicColor.value
    suspend fun notificationsNow(): Boolean = _notifications.value
    suspend fun liveUpdateNow(): Boolean = _liveUpdate.value
    fun liveHiddenDateNow(): String = prefs.getString(KEY_LIVE_HIDDEN, null).orEmpty()
    fun liveRefreshDateNow(): String = prefs.getString(KEY_LIVE_REFRESH, null).orEmpty()
    suspend fun hapticsNow(): Boolean = _haptics.value
    suspend fun hapticStrengthNow(): HapticStrength = _hapticStrength.value

    suspend fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC, enabled).apply()
        _dynamicColor.value = enabled
    }

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

    suspend fun setNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY, enabled).apply()
        _notifications.value = enabled
    }

    suspend fun setLiveUpdate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LIVE, enabled).apply()
        _liveUpdate.value = enabled
    }

    suspend fun setLiveHiddenDate(date: String) {
        prefs.edit().putString(KEY_LIVE_HIDDEN, date).apply()
    }

    suspend fun setLiveRefreshDate(date: String) {
        prefs.edit().putString(KEY_LIVE_REFRESH, date).apply()
    }

    suspend fun setHaptics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _haptics.value = enabled
    }

    suspend fun setHapticStrength(strength: HapticStrength) {
        prefs.edit().putString(KEY_HAPTIC_STRENGTH, strength.id).apply()
        _hapticStrength.value = strength
    }

    fun seenGradeIds(): Set<Int> =
        prefs.getString(KEY_SEEN_GRADES, null)
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            .orEmpty()

    fun snapshotReady(): Boolean = prefs.getBoolean(KEY_SNAPSHOT, false)

    fun seenAppointmentKeys(): Set<String> =
        prefs.getString(KEY_SEEN_LESSONS, null)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

    suspend fun rememberSnapshot(gradeIds: Set<Int>, appointmentKeys: Set<String>) {
        prefs.edit()
            .putString(KEY_SEEN_GRADES, gradeIds.joinToString(","))
            .putString(KEY_SEEN_LESSONS, appointmentKeys.joinToString("\n"))
            .putBoolean(KEY_SNAPSHOT, true)
            .apply()
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
        const val KEY_DYNAMIC = "material_you"
        const val KEY_NOTIFY = "notify_updates"
        const val KEY_LIVE = "live_update"
        const val KEY_LIVE_HIDDEN = "live_hidden_date"
        const val KEY_LIVE_REFRESH = "live_refresh_date"
        const val KEY_HAPTICS = "haptics"
        const val KEY_HAPTIC_STRENGTH = "haptic_strength"
        const val KEY_SEEN_GRADES = "seen_grade_ids"
        const val KEY_SEEN_LESSONS = "seen_lesson_keys"
        const val KEY_SNAPSHOT = "sync_snapshot_ready"
    }
}
