package nl.cita.magister.data.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.cita.magister.api.Tenant
import nl.cita.magister.haptics.HapticStrength
import nl.cita.magister.platform.SettingsStore

/**
 * Niet-geheime voorkeuren (tenant, demo, webhook-URL).
 * Tokens/secrets zitten in [nl.cita.magister.auth.TokenStore] / SecretStore.
 */
class UserPrefs(private val prefs: SettingsStore) {
    private val _tenantHost = MutableStateFlow(prefs.getString(KEY_TENANT).orEmpty())
    val tenantHost: Flow<String> = _tenantHost.asStateFlow()

    private val _demoMode = MutableStateFlow(prefs.getBoolean(KEY_DEMO, false))
    val demoMode: Flow<Boolean> = _demoMode.asStateFlow()

    private val _schoolName = MutableStateFlow(prefs.getString(KEY_SCHOOL).orEmpty())
    val schoolName: Flow<String> = _schoolName.asStateFlow()

    private val _tenantId = MutableStateFlow(prefs.getString(KEY_TENANT_ID).orEmpty())
    val tenantId: Flow<String> = _tenantId.asStateFlow()

    private val _webhookUrl = MutableStateFlow(prefs.getString(KEY_WEBHOOK_URL).orEmpty())
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
        HapticStrength.fromId(prefs.getString(KEY_HAPTIC_STRENGTH) ?: HapticStrength.Strong.id),
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
    fun liveHiddenDateNow(): String = prefs.getString(KEY_LIVE_HIDDEN).orEmpty()
    fun liveRefreshDateNow(): String = prefs.getString(KEY_LIVE_REFRESH).orEmpty()
    suspend fun hapticsNow(): Boolean = _haptics.value
    suspend fun hapticStrengthNow(): HapticStrength = _hapticStrength.value

    suspend fun setDynamicColor(enabled: Boolean) {
        prefs.putBoolean(KEY_DYNAMIC, enabled)
        _dynamicColor.value = enabled
    }

    suspend fun setTenant(host: String, schoolName: String, tenantId: String? = null) {
        val normalized = Tenant.normalize(host)
        val id = tenantId?.trim().orEmpty()
        prefs.putString(KEY_TENANT, normalized)
        prefs.putString(KEY_SCHOOL, schoolName)
        prefs.putString(KEY_TENANT_ID, id)
        prefs.flush()
        _tenantHost.value = normalized
        _schoolName.value = schoolName
        _tenantId.value = id
    }

    suspend fun setDemo(enabled: Boolean) {
        prefs.putBoolean(KEY_DEMO, enabled)
        _demoMode.value = enabled
    }

    suspend fun setNotifications(enabled: Boolean) {
        prefs.putBoolean(KEY_NOTIFY, enabled)
        _notifications.value = enabled
    }

    suspend fun setLiveUpdate(enabled: Boolean) {
        prefs.putBoolean(KEY_LIVE, enabled)
        _liveUpdate.value = enabled
    }

    suspend fun setLiveHiddenDate(date: String) {
        prefs.putString(KEY_LIVE_HIDDEN, date)
    }

    suspend fun setLiveRefreshDate(date: String) {
        prefs.putString(KEY_LIVE_REFRESH, date)
    }

    suspend fun setHaptics(enabled: Boolean) {
        prefs.putBoolean(KEY_HAPTICS, enabled)
        _haptics.value = enabled
    }

    suspend fun setHapticStrength(strength: HapticStrength) {
        prefs.putString(KEY_HAPTIC_STRENGTH, strength.id)
        _hapticStrength.value = strength
    }

    fun seenGradeIds(): Set<Int> =
        prefs.getString(KEY_SEEN_GRADES)
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            .orEmpty()

    fun snapshotReady(): Boolean = prefs.getBoolean(KEY_SNAPSHOT, false)

    fun seenAppointmentKeys(): Set<String> =
        prefs.getString(KEY_SEEN_LESSONS)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

    suspend fun rememberSnapshot(gradeIds: Set<Int>, appointmentKeys: Set<String>) {
        prefs.putString(KEY_SEEN_GRADES, gradeIds.joinToString(","))
        prefs.putString(KEY_SEEN_LESSONS, appointmentKeys.joinToString("\n"))
        prefs.putBoolean(KEY_SNAPSHOT, true)
        prefs.flush()
    }

    suspend fun setWebhookUrl(url: String) {
        val trimmed = url.trim()
        prefs.putString(KEY_WEBHOOK_URL, trimmed)
        _webhookUrl.value = trimmed
    }

    private companion object {
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
