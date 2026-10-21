package nl.cita.magister.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.KeyStore

/**
 * EncryptedSharedPreferences + MasterKey, met herstel dat op echte toestellen
 * bekend faalt (Keystore, Tink-keyset, race bij twee MasterKey.Builder calls).
 *
 * Volgorde: create → bij elke Throwable prefs + master key wissen → opnieuw →
 * anders onversleutelde fallback (beter dan crashen bij de start).
 */
object SecurePrefs {
    private const val TAG = "CitaSecurePrefs"
    private val lock = Any()

    fun open(context: Context, name: String): SharedPreferences = synchronized(lock) {
        val app = context.applicationContext
        try {
            encrypted(app, name)
        } catch (first: Throwable) {
            Log.w(TAG, "EncryptedSharedPreferences($name) mislukt, wis en probeer opnieuw", first)
            recover(app, name)
            try {
                encrypted(app, name)
            } catch (second: Throwable) {
                Log.e(TAG, "EncryptedSharedPreferences($name) opnieuw mislukt, fallback", second)
                app.getSharedPreferences("${name}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private fun encrypted(context: Context, name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun recover(context: Context, name: String) {
        runCatching { context.deleteSharedPreferences(name) }
        runCatching {
            val dir = File(context.applicationInfo.dataDir, "shared_prefs")
            File(dir, "$name.xml").delete()
            File(dir, "$name.xml.bak").delete()
        }
        runCatching {
            val store = KeyStore.getInstance("AndroidKeyStore")
            store.load(null)
            val alias = MasterKey.DEFAULT_MASTER_KEY_ALIAS
            if (store.containsAlias(alias)) {
                store.deleteEntry(alias)
            }
        }
    }
}
