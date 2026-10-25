package nl.cita.magister.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Tokens/secrets zonder de deprecated EncryptedSharedPreferences/MasterKey/Tink-stack.
 * Die crasht op een verse install op sommige toestellen (Pixel + Android 16/17 Keystore).
 *
 * Waarden gaan AES-GCM via AndroidKeystore (geen StrongBox). Bij elke Throwable:
 * key + prefs wissen, opnieuw, anders gewone SharedPreferences.
 */
object SecurePrefs {
    private const val TAG = "MagsSecurePrefs"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "mags_prefs_aes"
    private val lock = Any()

    fun open(context: Context, name: String): SharedPreferences = synchronized(lock) {
        val app = context.applicationContext
        try {
            KeystoreAesPrefs(app, name)
        } catch (first: Throwable) {
            Log.w(TAG, "Keystore-prefs($name) mislukt, wis en probeer opnieuw", first)
            recover(app, name)
            try {
                KeystoreAesPrefs(app, name)
            } catch (second: Throwable) {
                Log.e(TAG, "Keystore-prefs($name) opnieuw mislukt, fallback", second)
                app.getSharedPreferences("${name}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    internal fun recover(context: Context, name: String) {
        runCatching { context.deleteSharedPreferences(name) }
        runCatching {
            val dir = File(context.applicationInfo.dataDir, "shared_prefs")
            File(dir, "$name.xml").delete()
            File(dir, "$name.xml.bak").delete()
        }
        runCatching {
            val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
        }
    }

    internal fun secretKey(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey
        return generateKey()
    }

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
        if (Build.VERSION.SDK_INT >= 28) {
            // Pixel StrongBox heeft MasterKey/Tink laten vallen; software-TEE is genoeg.
            spec.setIsStrongBoxBacked(false)
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        return try {
            generator.init(spec.build())
            generator.generateKey()
        } catch (error: Throwable) {
            Log.w(TAG, "Eerste Keystore-generate mislukt, retry", error)
            val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
            val retry = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
            generator.init(retry.build())
            generator.generateKey()
        }
    }
}

private class KeystoreAesPrefs(
    context: Context,
    name: String,
) : SharedPreferences {
    private val raw = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val key = SecurePrefs.secretKey()

    override fun getAll(): MutableMap<String, *> {
        val out = LinkedHashMap<String, String?>()
        raw.all.keys.forEach { keyName ->
            out[keyName] = getString(keyName, null)
        }
        return out
    }

    override fun getString(key: String?, defValue: String?): String? {
        val stored = raw.getString(key, null) ?: return defValue
        return decrypt(stored) ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues

    override fun getInt(key: String?, defValue: Int): Int =
        getString(key, null)?.toIntOrNull() ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        getString(key, null)?.toLongOrNull() ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        getString(key, null)?.toFloatOrNull() ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        getString(key, null)?.toBooleanStrictOrNull() ?: defValue

    override fun contains(key: String?): Boolean = raw.contains(key)

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        raw.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        raw.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val bytes = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(iv.size + bytes.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(bytes, 0, packed, iv.size, bytes.size)
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String? = runCatching {
        val packed = Base64.decode(stored, Base64.NO_WRAP)
        require(packed.size > 12)
        val iv = packed.copyOfRange(0, 12)
        val bytes = packed.copyOfRange(12, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        String(cipher.doFinal(bytes), Charsets.UTF_8)
    }.getOrNull()

    private inner class EditorImpl : SharedPreferences.Editor {
        private val editor = raw.edit()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key == null) return this
            if (value == null) editor.remove(key) else editor.putString(key, encrypt(value))
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) editor.remove(key)
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor =
            putString(key, value.toString())

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
            putString(key, value.toString())

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
            putString(key, value.toString())

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
            putString(key, value.toString())

        override fun remove(key: String?): SharedPreferences.Editor {
            editor.remove(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            editor.clear()
            return this
        }

        override fun commit(): Boolean = editor.commit()

        override fun apply() = editor.apply()
    }
}
