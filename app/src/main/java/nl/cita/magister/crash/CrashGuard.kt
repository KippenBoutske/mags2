package nl.cita.magister.crash

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import nl.cita.magister.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Process-wide uncaught-exception hook. Shows [CrashActivity] (plain Views,
 * separate process) instead of a silent system close.
 */
object CrashGuard {
    const val EXTRA_TRACE = "app.mags.crash.TRACE"
    const val TRACE_FILE = "last_crash.txt"

    private const val PREFS = "mags_crash_guard"
    private const val KEY_HANDLING = "handling_at"
    private const val KEY_SAFE = "safe_mode"
    private const val TAG = "MagsCrash"

    @Volatile
    private var installed = false

    fun install(app: Application) {
        if (installed) return
        installed = true
        if (isCrashProcess(app)) return

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val last = prefs.getLong(KEY_HANDLING, 0L)
            if (now - last < 4_000L) {
                previous?.uncaughtException(thread, error)
                return@setDefaultUncaughtExceptionHandler
            }
            prefs.edit()
                .putLong(KEY_HANDLING, now)
                .putBoolean(KEY_SAFE, true)
                .commit()
            val trace = format(error)
            persistTrace(app, trace)
            Log.e(TAG, trace)
            try {
                app.startActivity(crashIntent(app, trace))
            } catch (launchError: Throwable) {
                Log.e(TAG, "CrashActivity starten mislukt", launchError)
                previous?.uncaughtException(thread, error)
                return@setDefaultUncaughtExceptionHandler
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    fun show(context: Context, error: Throwable) {
        val app = context.applicationContext
        val trace = format(error)
        persistTrace(app, trace)
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SAFE, true)
            .commit()
        Log.e(TAG, trace, error)
        context.startActivity(crashIntent(context, trace))
    }

    fun readTrace(context: Context): String? {
        val file = File(context.filesDir, TRACE_FILE)
        if (!file.exists()) return null
        return runCatching { file.readText() }.getOrNull()
    }

    fun isSafeMode(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SAFE, false)

    fun clearSafeMode(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SAFE, false)
            .apply()
    }

    fun isCrashProcess(context: Context): Boolean = processName(context).endsWith(":crash")

    fun format(error: Throwable): String {
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        return buildString {
            appendLine("Mags ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("applicationId=${BuildConfig.APPLICATION_ID}")
            appendLine("SDK ${Build.VERSION.SDK_INT} ${Build.VERSION.RELEASE}")
            appendLine("${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("ABI=${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine()
            append(stack)
        }
    }

    private fun crashIntent(context: Context, trace: String): Intent {
        val clipped = if (trace.length > 200_000) trace.take(200_000) + "\n…(afgekapt)" else trace
        return Intent(context, CrashActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
            )
            putExtra(EXTRA_TRACE, clipped)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private fun persistTrace(context: Context, trace: String) {
        runCatching {
            File(context.filesDir, TRACE_FILE).writeText(trace)
        }
    }

    private fun processName(context: Context): String {
        if (Build.VERSION.SDK_INT >= 28) return Application.getProcessName()
        val pid = Process.myPid()
        val am = context.getSystemService(ActivityManager::class.java) ?: return ""
        return am.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName.orEmpty()
    }
}
