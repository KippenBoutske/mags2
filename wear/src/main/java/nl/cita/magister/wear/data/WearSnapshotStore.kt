package nl.cita.magister.wear.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.cita.magister.wear.WearSyncPayload

object WearSnapshotStore {
    private const val PREFS_NAME = "mags_wear_snapshot"
    private const val KEY_JSON = "snapshot_json"

    private val _payload = MutableStateFlow<WearSyncPayload?>(null)
    val payload: StateFlow<WearSyncPayload?> = _payload.asStateFlow()

    fun load(context: Context): WearSyncPayload? {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null)
        val parsed = raw?.let { WearSyncPayload.fromJson(it) }
        _payload.value = parsed
        return parsed
    }

    fun save(context: Context, payload: WearSyncPayload) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, payload.toJson())
            .apply()
        _payload.value = payload
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_JSON)
            .apply()
        _payload.value = null
    }
}
