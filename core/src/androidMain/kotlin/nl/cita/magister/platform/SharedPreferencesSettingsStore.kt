package nl.cita.magister.platform

import android.content.SharedPreferences

class SharedPreferencesSettingsStore(
    private val prefs: SharedPreferences,
) : SettingsStore {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    override fun getLong(key: String, default: Long): Long {
        return try {
            prefs.getLong(key, default)
        } catch (_: ClassCastException) {
            prefs.getString(key, null)?.toLongOrNull() ?: default
        }
    }

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getInt(key: String, default: Int): Int {
        return try {
            prefs.getInt(key, default)
        } catch (_: ClassCastException) {
            prefs.getString(key, null)?.toIntOrNull() ?: default
        }
    }

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, default)
        } catch (_: ClassCastException) {
            prefs.getString(key, null)?.toBooleanStrictOrNull() ?: default
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun flush() {
        prefs.edit().commit()
    }
}
