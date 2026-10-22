package nl.cita.magister

import android.app.Application
import android.content.Context
import nl.cita.magister.crash.CrashGuard
import nl.cita.magister.di.AppContainer
import nl.cita.magister.work.MagisterSyncWorker
import nl.cita.magister.work.MagsNotifier

class CitaApp : Application() {
    private val containerHolder = lazy { AppContainer(this) }
    val container: AppContainer get() = containerHolder.value

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        CrashGuard.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (CrashGuard.isCrashProcess(this)) return
        runCatching { MagsNotifier.ensureChannel(this) }
        // Robolectric heeft geen WorkManager-initializer; op het toestel staat die via androidx.startup.
        runCatching { MagisterSyncWorker.enqueue(this) }
    }
}
